package com.placementpro.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Legacy shared profile model kept only for compatibility with older DTO usage.
// It is intentionally not a JPA entity anymore so Hibernate does not create an
// obsolete `profiles` table alongside the active role-specific profile tables.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    private Long id;
    private String phone;
    private String department;
    private String year;
    private String skills;
    private String resumeUrl;
    private String bio;
    private String companyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
