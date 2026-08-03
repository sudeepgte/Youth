package com.example.demo.service;

import com.example.demo.model.MultiplayerRoomEntity;
import com.example.demo.repository.MultiplayerRoomRepository;
import com.example.demo.util.RoomCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MultiplayerRoomService {

    private static final Logger logger = LoggerFactory.getLogger(MultiplayerRoomService.class);

    @Autowired(required = false)
    private MultiplayerRoomRepository roomRepository;

    // Fast in-memory cache indexed by normalized room ID
    private final Map<String, Object> roomCache = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAccessTime = new ConcurrentHashMap<>();

    /**
     * Generates a unique 6-character room code.
     */
    public String generateUniqueRoomId() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = RoomCodeUtil.generateRoomCode();
            String norm = RoomCodeUtil.normalize(code);
            if (!roomCache.containsKey(norm)) {
                if (roomRepository != null) {
                    try {
                        if (roomRepository.findByNormalizedRoomId(norm).isPresent()) {
                            continue;
                        }
                    } catch (Exception e) {
                        logger.warn("DB check for room code failed, using in-memory check: {}", e.getMessage());
                    }
                }
                return code;
            }
        }
        return RoomCodeUtil.generateRoomCode();
    }

    /**
     * Saves or updates a room in memory and DB.
     */
    public void registerRoom(String roomId, String gameType, Object roomObject, String player1, Integer maxPlayers) {
        if (roomId == null || roomId.isBlank()) return;
        String normId = RoomCodeUtil.normalize(roomId);
        roomCache.put(normId, roomObject);
        lastAccessTime.put(normId, System.currentTimeMillis());

        if (roomRepository != null) {
            try {
                Optional<MultiplayerRoomEntity> existing = roomRepository.findByNormalizedRoomId(normId);
                MultiplayerRoomEntity entity;
                if (existing.isPresent()) {
                    entity = existing.get();
                    entity.setUpdatedAt(LocalDateTime.now());
                    if (player1 != null) entity.setPlayer1(player1);
                } else {
                    entity = new MultiplayerRoomEntity(roomId, normId, gameType, player1, maxPlayers);
                }
                
                if (roomObject != null) {
 entity.setRoomDataJson(String.valueOf(roomObject)); }

                roomRepository.save(entity);
            } catch (Exception e) {
                logger.warn("Failed to persist room entity to DB: {}", e.getMessage());
            }
        }
    }

    /**
     * Retrieves a room by raw room ID using canonical normalization.
     */
    @SuppressWarnings("unchecked")
    public <T> T getRoom(String rawRoomId, Class<T> clazz) {
        if (rawRoomId == null || rawRoomId.isBlank()) return null;
        String normId = RoomCodeUtil.normalize(rawRoomId);

        Object cachedObj = roomCache.get(normId);
        if (cachedObj != null && clazz.isInstance(cachedObj)) {
            lastAccessTime.put(normId, System.currentTimeMillis());
            return (T) cachedObj;
        }

        // Secondary fallback lookup using original raw string in case of edge cases
        Object rawCached = roomCache.get(rawRoomId.trim().toUpperCase());
        if (rawCached != null && clazz.isInstance(rawCached)) {
            lastAccessTime.put(normId, System.currentTimeMillis());
            return (T) rawCached;
        }

        return null;
    }

    /**
     * Touch a room to keep it active.
     */
    public void touchRoom(String rawRoomId) {
        if (rawRoomId == null) return;
        String normId = RoomCodeUtil.normalize(rawRoomId);
        if (roomCache.containsKey(normId)) {
            lastAccessTime.put(normId, System.currentTimeMillis());
        }
    }

    /**
     * Removes a room when explicitly completed or terminated.
     */
    public void removeRoom(String rawRoomId) {
        if (rawRoomId == null || rawRoomId.isBlank()) return;
        String normId = RoomCodeUtil.normalize(rawRoomId);
        roomCache.remove(normId);
        lastAccessTime.remove(normId);

        if (roomRepository != null) {
            try {
                roomRepository.findByNormalizedRoomId(normId).ifPresent(entity -> {
                    entity.setStatus("completed");
                    roomRepository.save(entity);
                });
            } catch (Exception e) {
                logger.warn("Failed to update room status in DB: {}", e.getMessage());
            }
        }
    }

    /**
     * Checks if a room exists under any canonical variation of rawRoomId.
     */
    public boolean roomExists(String rawRoomId) {
        if (rawRoomId == null || rawRoomId.isBlank()) return false;
        String normId = RoomCodeUtil.normalize(rawRoomId);
        if (roomCache.containsKey(normId)) return true;
        if (roomRepository != null) {
            try {
                return roomRepository.findByNormalizedRoomId(normId).isPresent();
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}
