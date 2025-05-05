package com.uokaradeniz.backend.company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, java.lang.Long> {
    Optional<Company> findByEmail(String email);

    Optional<Company> findByApiKey(String apiKey);
}