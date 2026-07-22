package com.project.assignment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.assignment.entity.ClassMeeting;

public interface ClassMeetingRepository extends JpaRepository<ClassMeeting, UUID> {

    Optional<ClassMeeting> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    List<ClassMeeting> findByClassUuidOrderByCreatedAtDesc(UUID classUuid);

    List<ClassMeeting> findByClassUuidInAndActiveTrueOrderByCreatedAtDesc(Collection<UUID> classUuids);

    @Modifying
    @Query("UPDATE ClassMeeting m SET m.active = false WHERE m.classUuid = :classUuid AND m.active = true")
    int deactivateAllForClass(@Param("classUuid") UUID classUuid);
}
