package com.placementpro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficerDashboardDTO {
    private long totalStudents;
    private long totalCompanies;
    private long activeJobs;
    private long placedStudents;
    private long pendingApplications;
}
