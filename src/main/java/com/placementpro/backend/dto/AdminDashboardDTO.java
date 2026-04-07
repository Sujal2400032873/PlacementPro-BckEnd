package com.placementpro.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardDTO {
    private long totalStudents;
    private long totalEmployers;
    private long totalJobs;
    private long totalApplications;
    private List<String> recentActivities;
}
