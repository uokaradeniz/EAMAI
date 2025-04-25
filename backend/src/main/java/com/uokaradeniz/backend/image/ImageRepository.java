package com.uokaradeniz.backend.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findImagesByTwinId(UUID twinId);

    List<Image> findImagesByProcessStatusAndSessionId(boolean processStatus, UUID sessionId);

    @Query("select i from Image i where i.processStatus = ?1 and i.isPhoto = ?2")
    List<Image> findAllByProcessStatusAndIsPhoto(boolean processStatus, boolean isPhoto);
}