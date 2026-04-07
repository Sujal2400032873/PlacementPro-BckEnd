package com.placementpro.backend.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileDTO {
    private String rollNumber;
    private String branch;
    private String skills;
    private Double cgpa;
    private Integer graduationYear;
    private String resumeUrl;
}
