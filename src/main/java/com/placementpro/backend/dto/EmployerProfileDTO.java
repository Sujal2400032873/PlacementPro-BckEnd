package com.placementpro.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployerProfileDTO {
    private String companyName;
    private String industry;
    private String companySize;
    private String website;
    private String description;
    private String hrContact;
}
