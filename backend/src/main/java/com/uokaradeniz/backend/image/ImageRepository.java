package com.uokaradeniz.backend.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findImagesBySessionId(UUID sessionId);

    List<Image> findBySessionId(UUID sessionId);
}