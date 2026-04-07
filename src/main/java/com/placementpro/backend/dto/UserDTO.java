package com.placementpro.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean profileComplete;
    private boolean profileCompleted;
    private StudentProfileDTO studentProfile;
    private EmployerProfileDTO employerProfile;
    private PlacementProfileDTO placementProfile;
    private LocalDateTime createdAt;
}
