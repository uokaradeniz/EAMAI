package com.uokaradeniz.backend.company;

import org.apache.tomcat.util.json.JSONFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/register")
    public ResponseEntity<Company> registerCompany(@RequestBody CompanyDTO companyDTO) {
        Company registeredCompany = companyService.registerCompany(companyDTO.getName(), companyDTO.getEmail());
        return ResponseEntity.ok(registeredCompany);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody String jsonPayload) {
        Optional<Company> company = companyService.authenticate(jsonPayload);
        if (company.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(company.get().getId());
    }
}