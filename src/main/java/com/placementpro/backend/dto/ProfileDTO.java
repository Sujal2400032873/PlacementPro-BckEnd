package com.placementpro.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileDTO {
    private String phone;
    private String department;
    private String skills;
    private String resumeUrl;
    private String companyName;
}
