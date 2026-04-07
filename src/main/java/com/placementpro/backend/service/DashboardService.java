package com.placementpro.backend.service;

import com.placementpro.backend.dto.AdminDashboardDTO;
import com.placementpro.backend.dto.OfficerDashboardDTO;
import com.placementpro.backend.entity.ApplicationStatus;
import com.placementpro.backend.entity.JobStatus;
import com.placementpro.backend.entity.Notification;
import com.placementpro.backend.repository.ApplicationRepository;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.repository.NotificationRepository;
import com.placementpro.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationRepository notificationRepository;

    @Cacheable(cacheNames = "adminDashboard", key = "'summary'")
    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboard() {
        return AdminDashboardDTO.builder()
                .totalStudents(userRepository.countByRoles_Name("ROLE_STUDENT"))
                .totalEmployers(userRepository.countByRoles_Name("ROLE_EMPLOYER"))
                .totalJobs(jobRepository.count())
                .totalApplications(applicationRepository.count())
                .recentActivities(buildRecentActivities())
                .build();
    }

    @Cacheable(cacheNames = "officerDashboard", key = "'summary'")
    @Transactional(readOnly = true)
    public OfficerDashboardDTO getOfficerDashboard() {
        return OfficerDashboardDTO.builder()
                .totalStudents(userRepository.countByRoles_Name("ROLE_STUDENT"))
                .totalCompanies(userRepository.countByRoles_Name("ROLE_EMPLOYER"))
                .activeJobs(jobRepository.countByStatus(JobStatus.OPEN))
                .placedStudents(applicationRepository.countByStatus(ApplicationStatus.APPROVED))
                .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.APPLIED))
                .build();
    }

    private List<String> buildRecentActivities() {
        List<String> activities = new ArrayList<>();
        for (Notification notification : notificationRepository.findTop5ByOrderByCreatedAtDesc()) {
            activities.add(notification.getMessage());
        }
        if (activities.isEmpty()) {
            activities.add("No recent activities available.");
        }
        return activities;
    }
}
