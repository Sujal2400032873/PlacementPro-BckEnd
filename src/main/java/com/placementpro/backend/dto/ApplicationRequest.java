package com.placementpro.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationRequest {
    @NotNull(message = "Job ID is required")
    private Long jobId;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Course is required")
    private String course;

    @NotBlank(message = "Branch is required")
    private String branch;

    @NotNull(message = "CGPA is required")
    @DecimalMin(value = "0.0", message = "CGPA cannot be negative")
    @DecimalMax(value = "10.0", message = "CGPA cannot exceed 10")
    private Double cgpa;

    @NotBlank(message = "Skills are required")
    private String skills;

    @NotBlank(message = "Resume URL is required")
    @Size(max = 500)
    private String resumeUrl;

    @Size(max = 1000)
    private String coverLetter;

    @Size(max = 1000)
    private String notes;
}
