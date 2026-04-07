package com.placementpro.backend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationDTO {
    private Long id;
    private Long jobId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String status;
    private LocalDateTime appliedAt;
    private String notes;
    private String coverLetter;
    private String resumeUrl;
    private String jobTitle;
    private String companyName;
}
