package com.stayease.config;

import com.stayease.entity.Hotel;
import com.stayease.entity.HotelContactInfo;
import com.stayease.entity.Room;
import com.stayease.entity.User;
import com.stayease.entity.enums.Role;
import com.stayease.repository.HotelRepository;
import com.stayease.repository.RoomRepository;
import com.stayease.repository.UserRepository;
import com.stayease.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Local-development demo data: the accounts advertised on the login page plus a few published
 * hotels. Enabled with SEED_DEMO_DATA=true; does nothing if the demo manager already exists.
 * Never enable this in production — the demo passwords are public.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo-data", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeeder implements ApplicationRunner {

    static final String DEMO_PASSWORD = "password123";
    static final String MANAGER_EMAIL = "manager@demo.com";
    static final String GUEST_EMAIL = "guest@demo.com";

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(MANAGER_EMAIL)) {
            log.info("Demo data already present, skipping seeding");
            return;
        }

        User manager = createUser("Demo Manager", MANAGER_EMAIL, Role.HOTEL_MANAGER);
        createUser("Demo Guest", GUEST_EMAIL, Role.GUEST);

        Hotel seaBreeze = createHotel(manager, "Sea Breeze Resort", "Goa",
                List.of("Pool", "WiFi", "Beach access"),
                "https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800",
                "Calangute Beach Road, Goa", "+91 98765 43210");
        createRoom(seaBreeze, "Garden Room", 2500, 5, 2, List.of("AC", "WiFi"));
        createRoom(seaBreeze, "Ocean Suite", 6000, 2, 3, List.of("Sea view", "Bathtub"));

        Hotel haveli = createHotel(manager, "Heritage Haveli", "Jaipur",
                List.of("Rooftop restaurant", "WiFi"),
                "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800",
                "Amer Road, Jaipur", "+91 91234 56789");
        createRoom(haveli, "Royal Room", 4200, 4, 2, List.of("AC", "Courtyard view"));

        Hotel skyline = createHotel(manager, "Skyline Suites", "Mumbai",
                List.of("Gym", "WiFi", "Spa"),
                "https://images.unsplash.com/photo-1542314831-068cd1dbfeeb?w=800",
                "Bandra Kurla Complex, Mumbai", "+91 99887 76655");
        createRoom(skyline, "Business King", 5500, 6, 2, List.of("Work desk", "WiFi"));
        createRoom(skyline, "Studio", 3200, 3, 1, List.of("Kitchenette"));

        log.info("Seeded demo data: {} / {} and {} / {} with 3 hotels",
                MANAGER_EMAIL, DEMO_PASSWORD, GUEST_EMAIL, DEMO_PASSWORD);
    }

    private User createUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRoles(Set.of(role));
        return userRepository.save(user);
    }

    private Hotel createHotel(User owner, String name, String city, List<String> amenities,
                              String photo, String address, String phone) {
        HotelContactInfo contact = new HotelContactInfo();
        contact.setAddress(address);
        contact.setPhoneNumber(phone);

        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setCity(city);
        hotel.setAmenities(amenities);
        hotel.setPhotos(List.of(photo));
        hotel.setContactInfo(contact);
        hotel.setOwner(owner);
        hotel.setActive(true); // published straight away; rooms get inventory below
        return hotelRepository.save(hotel);
    }

    private void createRoom(Hotel hotel, String type, int basePrice, int totalCount, int capacity,
                            List<String> amenities) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setType(type);
        room.setBasePrice(BigDecimal.valueOf(basePrice));
        room.setTotalCount(totalCount);
        room.setCapacity(capacity);
        room.setAmenities(amenities);
        room.setPhotos(List.of());
        inventoryService.ensureInventory(roomRepository.save(room));
    }
}
