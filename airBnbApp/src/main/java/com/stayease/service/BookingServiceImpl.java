package com.stayease.service;

import com.stayease.exception.BadRequestException;
import com.stayease.exception.ConflictException;
import com.stayease.dto.BookingDto;
import com.stayease.dto.BookingRequest;
import com.stayease.dto.GuestDto;
import com.stayease.entity.*;
import com.stayease.entity.enums.BookingStatus;
import com.stayease.entity.enums.PaymentStatus;
import com.stayease.exception.ResourceNotFoundException;
import com.stayease.repository.*;
import com.stayease.security.SecurityUtils;
import com.stayease.service.payment.PaymentGateway;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final GuestRepository guestRepository;
    private final ModelMapper modelMapper;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryRepository inventoryRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final Duration holdDuration;

    public BookingServiceImpl(GuestRepository guestRepository,
                              ModelMapper modelMapper,
                              BookingRepository bookingRepository,
                              HotelRepository hotelRepository,
                              RoomRepository roomRepository,
                              InventoryRepository inventoryRepository,
                              PaymentRepository paymentRepository,
                              PaymentGateway paymentGateway,
                              @Value("${booking.hold-minutes:10}") long holdMinutes) {
        this.guestRepository = guestRepository;
        this.modelMapper = modelMapper;
        this.bookingRepository = bookingRepository;
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.inventoryRepository = inventoryRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.holdDuration = Duration.ofMinutes(holdMinutes);
    }

    @Override
    @Transactional
    public BookingDto initialiseBooking(BookingRequest bookingRequest) {
        log.info("Initialising booking for hotel: {}, room: {}, dates: {}-{}",
                bookingRequest.getHotelId(), bookingRequest.getRoomId(),
                bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());

        // Resolve the caller before taking any inventory locks
        User user = SecurityUtils.getCurrentUser();

        long daysCount = ChronoUnit.DAYS.between(bookingRequest.getCheckInDate(), bookingRequest.getCheckOutDate());
        if (daysCount <= 0) {
            throw new BadRequestException("Check-out date must be after check-in date");
        }

        Hotel hotel = hotelRepository.findById(bookingRequest.getHotelId())
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id: " + bookingRequest.getHotelId()));
        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new ConflictException("This hotel is not accepting bookings");
        }

        // The room must belong to the hotel being booked
        Room room = roomRepository.findByIdAndHotelId(bookingRequest.getRoomId(), hotel.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + bookingRequest.getRoomId()));

        List<Inventory> inventoryList = inventoryRepository.findAndLockAvailableInventory(
                room.getId(),
                bookingRequest.getCheckInDate(),
                bookingRequest.getCheckOutDate(),
                bookingRequest.getRoomsCount());

        if (inventoryList.size() != daysCount) {
            throw new ConflictException("Room is not available for the selected dates");
        }

        for (Inventory inventory : inventoryList) {
            inventory.setReservedCount(inventory.getReservedCount() + bookingRequest.getRoomsCount());
        }
        inventoryRepository.saveAll(inventoryList);

        BigDecimal totalAmount = inventoryList.stream()
                .map(Inventory::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(bookingRequest.getRoomsCount()));

        Booking booking = Booking.builder()
                .bookingStatus(BookingStatus.RESERVED)
                .hotel(hotel)
                .room(room)
                .checkInDate(bookingRequest.getCheckInDate())
                .checkOutDate(bookingRequest.getCheckOutDate())
                .user(user)
                .roomsCount(bookingRequest.getRoomsCount())
                .amount(totalAmount)
                .build();

        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto addGuests(Long bookingId, List<GuestDto> guestDtoList) {
        log.info("Adding guests for booking with id: {}", bookingId);

        User user = SecurityUtils.getCurrentUser();
        Booking booking = getOwnBookingForUpdate(bookingId, user);

        if (booking.getBookingStatus() != BookingStatus.RESERVED) {
            throw new ConflictException("Guests can only be added to a reserved booking");
        }
        requireHoldActive(booking);

        int maxGuests = booking.getRoom().getCapacity() * booking.getRoomsCount();
        if (guestDtoList.size() > maxGuests) {
            throw new BadRequestException("This booking allows at most " + maxGuests + " guests");
        }

        for (GuestDto guestDto : guestDtoList) {
            Guest guest = modelMapper.map(guestDto, Guest.class);
            guest.setId(null);
            guest.setUser(user);
            guest = guestRepository.save(guest);
            booking.getGuests().add(guest);
        }

        booking.setBookingStatus(BookingStatus.GUESTS_ADDED);
        booking = bookingRepository.save(booking);
        return toDto(booking);
    }

    @Override
    @Transactional
    public BookingDto payForBooking(Long bookingId) {
        log.info("Processing payment for booking with id: {}", bookingId);

        Booking booking = getOwnBookingForUpdate(bookingId, SecurityUtils.getCurrentUser());

        if (booking.getBookingStatus() != BookingStatus.GUESTS_ADDED) {
            throw new ConflictException(booking.getBookingStatus() == BookingStatus.RESERVED
                    ? "Add guest details before paying"
                    : "This booking cannot be paid for");
        }
        requireHoldActive(booking);

        // Charge first: if the gateway declines, the exception rolls back and the hold stays as it was
        String transactionId = paymentGateway.charge(booking.getId(), booking.getAmount());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(booking.getAmount());
        payment.setTransactionId(transactionId);
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
        paymentRepository.save(payment);

        // The held rooms become booked rooms
        for (Inventory inventory : lockInventory(booking)) {
            inventory.setReservedCount(Math.max(0, inventory.getReservedCount() - booking.getRoomsCount()));
            inventory.setBookedCount(inventory.getBookedCount() + booking.getRoomsCount());
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        return toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto cancelBooking(Long bookingId) {
        log.info("Cancelling booking with id: {}", bookingId);

        Booking booking = getOwnBookingForUpdate(bookingId, SecurityUtils.getCurrentUser());
        BookingStatus status = booking.getBookingStatus();

        if (status == BookingStatus.CANCELLED || status == BookingStatus.EXPIRED) {
            throw new ConflictException("This booking is already " + status.name().toLowerCase());
        }
        if (!booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new ConflictException("Bookings can only be cancelled before the check-in date");
        }

        if (status == BookingStatus.CONFIRMED) {
            paymentRepository.findByBookingId(booking.getId()).ifPresent(payment -> {
                paymentGateway.refund(payment.getTransactionId(), payment.getAmount());
                payment.setPaymentStatus(PaymentStatus.CANCELLED);
            });
        }
        releaseRooms(booking, status);

        booking.setBookingStatus(BookingStatus.CANCELLED);
        return toDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getMyBookings() {
        return bookingRepository.findAllByUserIdOrderByCreatedAtDesc(SecurityUtils.getCurrentUser().getId())
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public boolean expireIfHoldElapsed(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        // Re-checked under the row lock: the guest may have paid or cancelled in the meantime
        if (booking == null || !booking.getBookingStatus().isHold() || !hasHoldElapsed(booking)) {
            return false;
        }
        releaseRooms(booking, booking.getBookingStatus());
        booking.setBookingStatus(BookingStatus.EXPIRED);
        bookingRepository.save(booking);
        log.info("Booking {} expired, released {} room(s)", booking.getId(), booking.getRoomsCount());
        return true;
    }

    private void releaseRooms(Booking booking, BookingStatus fromStatus) {
        for (Inventory inventory : lockInventory(booking)) {
            if (fromStatus == BookingStatus.CONFIRMED) {
                inventory.setBookedCount(Math.max(0, inventory.getBookedCount() - booking.getRoomsCount()));
            } else {
                inventory.setReservedCount(Math.max(0, inventory.getReservedCount() - booking.getRoomsCount()));
            }
        }
    }

    private List<Inventory> lockInventory(Booking booking) {
        return inventoryRepository.findAndLockInventoryForStay(
                booking.getRoom().getId(), booking.getCheckInDate(), booking.getCheckOutDate());
    }

    // Bookings of other users are reported as "not found" so their existence is not revealed
    private Booking getOwnBookingForUpdate(Long bookingId, User user) {
        return bookingRepository.findByIdAndUserIdForUpdate(bookingId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));
    }

    private void requireHoldActive(Booking booking) {
        if (hasHoldElapsed(booking)) {
            throw new ConflictException("Your reservation hold has expired, please book again");
        }
    }

    private boolean hasHoldElapsed(Booking booking) {
        return booking.getCreatedAt().plus(holdDuration).isBefore(LocalDateTime.now());
    }

    private BookingDto toDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setHotelId(booking.getHotel().getId());
        dto.setHotelName(booking.getHotel().getName());
        dto.setHotelCity(booking.getHotel().getCity());
        dto.setRoomId(booking.getRoom().getId());
        dto.setRoomType(booking.getRoom().getType());
        dto.setRoomsCount(booking.getRoomsCount());
        dto.setCheckInDate(booking.getCheckInDate());
        dto.setCheckOutDate(booking.getCheckOutDate());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());
        dto.setBookingStatus(booking.getBookingStatus());
        dto.setAmount(booking.getAmount());
        if (booking.getBookingStatus().isHold() && booking.getCreatedAt() != null) {
            dto.setHoldExpiresAt(booking.getCreatedAt().plus(holdDuration));
        }
        dto.setGuests(booking.getGuests().stream()
                .map(guest -> modelMapper.map(guest, GuestDto.class))
                .collect(Collectors.toSet()));
        return dto;
    }
}
