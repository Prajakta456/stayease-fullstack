package com.codingshuttle.projects.airBnbApp.service;

import com.codingshuttle.projects.airBnbApp.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Inventory is created one year ahead when a hotel is activated. This daily job rolls that
 * horizon forward, so active hotels never run out of bookable dates.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InventoryExtensionScheduler {

    private final RoomRepository roomRepository;
    private final InventoryService inventoryService;

    @Scheduled(cron = "${inventory.extension-cron:0 30 2 * * *}")
    public void extendInventory() {
        for (Long roomId : roomRepository.findIdsOfRoomsInActiveHotels()) {
            try {
                inventoryService.ensureInventory(roomId); // own transaction per room
            } catch (Exception e) {
                log.error("Failed to extend inventory for room {}", roomId, e);
            }
        }
    }
}
