package com.example.demo.repository;

import com.example.demo.model.MultiplayerRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MultiplayerRoomRepository extends JpaRepository<MultiplayerRoomEntity, Long> {
    Optional<MultiplayerRoomEntity> findByNormalizedRoomId(String normalizedRoomId);
    Optional<MultiplayerRoomEntity> findByNormalizedRoomIdAndGameType(String normalizedRoomId, String gameType);
    void deleteByNormalizedRoomId(String normalizedRoomId);
}
