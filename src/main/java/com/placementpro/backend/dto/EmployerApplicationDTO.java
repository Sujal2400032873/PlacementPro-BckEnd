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
public class EmployerApplicationDTO {
    private Long id;
    private Long applicationId;
    private Long jobId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String phone;
    private String course;
    private String branch;
    private Double cgpa;
    private String skills;
    private String resumeUrl;
    private String status;
    private LocalDateTime appliedDate;
    private String notes;
    private String coverLetter;
}
