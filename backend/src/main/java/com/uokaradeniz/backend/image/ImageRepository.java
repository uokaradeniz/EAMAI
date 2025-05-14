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

    @Query("select i from Image i where i.processStatus = ?1 and i.isPhoto = ?2 and i.company.id = ?3")
    List<Image> findAllByProcessStatusAndIsPhotoAndCompanyId(boolean processStatus, boolean isPhoto, Long companyId);

    List<Image> findAllBySessionId(UUID sessionId);

    List<Image> findAllByProcessStatusAndCompanyId(boolean processStatus, Long company_id);

    @Query("select count(i) > 0 from Image i where i.sessionDetails is null and i.sessionId = ?1")
    boolean existsBySessionDetailsNullAndSessionId(UUID sessionId);

    List<Image> findAllBySessionIdAndCompanyId(UUID sessionId, Long companyId);
}