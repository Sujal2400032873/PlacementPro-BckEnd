package com.placementpro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDTO {
    private Long id;
    private Long applicationId;
    private String studentName;
    private String studentEmail;
    private String phone;
    private String department;
    private Double cgpa;
    private String skills;
    private String resumeUrl;
    private String status;
    private LocalDateTime appliedDate;
}
