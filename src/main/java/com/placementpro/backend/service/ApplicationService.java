package com.placementpro.backend.service;

import com.placementpro.backend.dto.ApplicationDTO;
import com.placementpro.backend.dto.ApplicationRequest;
import com.placementpro.backend.dto.EmployerApplicationDTO;
import com.placementpro.backend.entity.Application;
import com.placementpro.backend.entity.ApplicationStatus;
import com.placementpro.backend.entity.Job;
import com.placementpro.backend.entity.JobStatus;
import com.placementpro.backend.entity.StudentProfile;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.ApplicationRepository;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ApplicationRepository applicationRepository;
    private final JobRepository jobRepository;
    private final CurrentUserService currentUserService;
    private final NotificationService notificationService;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<ApplicationDTO> getApplicationsByStudent(Long studentId, Integer page, Integer size) {
        User currentUser = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(currentUser, "ROLE_STUDENT") || !currentUser.getId().equals(studentId)) {
            throw new RuntimeException("You can only view your own applications");
        }

        Pageable pageable = buildPageable(page, size, "appliedAt");
        return applicationRepository.findByStudent_IdOrderByAppliedAtDesc(studentId, pageable).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployerApplicationDTO> getApplicationsByJob(Long jobId, Integer page, Integer size) {
        User employer = currentUserService.getCurrentUser();
        Job job = jobRepository.findDetailedById(jobId)
                .or(() -> jobRepository.findById(jobId))
                .orElseThrow(() -> new RuntimeException("Job not found"));
        validateEmployerOwnership(job, employer);

        Pageable pageable = buildPageable(page, size, "appliedAt");
        return applicationRepository.findByJob_IdOrderByAppliedAtDesc(jobId, pageable).stream()
                .map(this::convertToEmployerDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> getApplicationsByEmployer(Long employerId, Integer page, Integer size) {
        User employer = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER") || !employer.getId().equals(employerId)) {
            throw new RuntimeException("You can only view applications for your own jobs");
        }

        Pageable pageable = buildPageable(page, size, "appliedAt");
        return applicationRepository.findByJob_Employer_IdOrderByAppliedAtDesc(employerId, pageable).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EmployerApplicationDTO> getEmployerApplications(Long employerId, Integer page, Integer size) {
        User employer = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER") || !employer.getId().equals(employerId)) {
            throw new RuntimeException("You can only view applications for your own jobs");
        }

        Pageable pageable = buildPageable(page, size, "appliedAt");
        return applicationRepository.findByJob_Employer_IdOrderByAppliedAtDesc(employerId, pageable).stream()
                .map(this::convertToEmployerDto)
                .toList();
    }

    @CacheEvict(cacheNames = {"adminDashboard", "officerDashboard"}, allEntries = true)
    public ApplicationDTO applyForJob(ApplicationRequest applicationRequest) {
        User student = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(student, "ROLE_STUDENT")) {
            throw new RuntimeException("Only students can apply for jobs");
        }

        if (applicationRequest.getJobId() == null) {
            throw new RuntimeException("Job is required");
        }

        if (applicationRepository.existsByJob_IdAndStudent_Id(applicationRequest.getJobId(), student.getId())) {
            throw new RuntimeException("You have already applied for this job!");
        }

        Job job = jobRepository.findDetailedById(applicationRequest.getJobId())
                .or(() -> jobRepository.findById(applicationRequest.getJobId()))
                .orElseThrow(() -> new RuntimeException("Job not found"));
        if (job.getStatus() != JobStatus.OPEN) {
            throw new RuntimeException("Applications are only allowed for open jobs");
        }

        StudentProfile studentProfile = student.getStudentProfile();
        String resumeUrl = firstNonBlank(
                applicationRequest.getResumeUrl(),
                studentProfile != null ? studentProfile.getResumeUrl() : null
        );
        if (resumeUrl == null) {
            throw new RuntimeException("Please upload resume before applying");
        }

        Application application = Application.builder()
                .job(job)
                .student(student)
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDateTime.now())
                .notes(applicationRequest.getNotes())
                .phone(trimToNull(applicationRequest.getPhone()))
                .course(trimToNull(applicationRequest.getCourse()))
                .branch(firstNonBlank(applicationRequest.getBranch(), studentProfile != null ? studentProfile.getBranch() : null))
                .cgpa(applicationRequest.getCgpa() != null ? applicationRequest.getCgpa() : studentProfile != null ? studentProfile.getCgpa() : null)
                .skills(firstNonBlank(applicationRequest.getSkills(), studentProfile != null ? studentProfile.getSkills() : null))
                .coverLetter(firstNonBlank(applicationRequest.getCoverLetter(), applicationRequest.getNotes()))
                .resumeUrl(resumeUrl)
                .build();

        Application savedApp = applicationRepository.save(application);
        if (notificationService != null && job.getEmployer() != null) {
            notificationService.notifyUser(
                    job.getEmployer().getId(),
                    student.getName() + " applied for " + job.getTitle(),
                    "APPLICATION_SUBMITTED"
            );
        }
        return convertToDto(savedApp);
    }

    @CacheEvict(cacheNames = {"adminDashboard", "officerDashboard"}, allEntries = true)
    public ApplicationDTO updateStatus(Long id, String status) {
        User employer = currentUserService.getCurrentUser();
        Application application = applicationRepository.findDetailedById(id)
                .or(() -> applicationRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Application not found"));
        validateEmployerOwnership(application.getJob(), employer);

        ApplicationStatus nextStatus = parseApplicationStatus(status);
        application.setStatus(nextStatus);
        Application updatedApp = applicationRepository.save(application);
        if (notificationService != null && application.getStudent() != null && application.getJob() != null) {
            notificationService.notifyUser(
                    application.getStudent().getId(),
                    "Your application for " + application.getJob().getTitle() + " is now " + nextStatus.name(),
                    "APPLICATION_STATUS"
            );
        }
        return convertToDto(updatedApp);
    }

    private ApplicationDTO convertToDto(Application application) {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(application.getId());
        dto.setStatus(application.getStatus() != null ? application.getStatus().name() : null);
        dto.setAppliedAt(application.getAppliedAt());
        dto.setNotes(application.getNotes());
        dto.setCoverLetter(application.getCoverLetter());
        dto.setResumeUrl(resolveResumeUrl(application));
        if (application.getJob() != null) {
            dto.setJobId(application.getJob().getId());
            dto.setJobTitle(application.getJob().getTitle());
            dto.setCompanyName(application.getJob().getCompanyName());
        }
        if (application.getStudent() != null) {
            dto.setStudentId(application.getStudent().getId());
            dto.setStudentName(application.getStudent().getName());
            dto.setStudentEmail(application.getStudent().getEmail());
        }
        return dto;
    }

    private EmployerApplicationDTO convertToEmployerDto(Application application) {
        StudentProfile profile = application.getStudent() != null ? application.getStudent().getStudentProfile() : null;
        return EmployerApplicationDTO.builder()
                .applicationId(application.getId())
                .jobId(application.getJob() != null ? application.getJob().getId() : null)
                .studentId(application.getStudent() != null ? application.getStudent().getId() : null)
                .studentName(application.getStudent() != null ? application.getStudent().getName() : null)
                .studentEmail(application.getStudent() != null ? application.getStudent().getEmail() : null)
                .phone(application.getPhone())
                .course(application.getCourse())
                .branch(firstNonBlank(application.getBranch(), profile != null ? profile.getBranch() : null))
                .cgpa(application.getCgpa() != null ? application.getCgpa() : profile != null ? profile.getCgpa() : null)
                .skills(firstNonBlank(application.getSkills(), profile != null ? profile.getSkills() : null))
                .resumeUrl(resolveResumeUrl(application))
                .id(application.getId())
                .status(application.getStatus() != null ? application.getStatus().name() : null)
                .appliedDate(application.getAppliedAt())
                .notes(application.getNotes())
                .coverLetter(application.getCoverLetter())
                .build();
    }

    private String resolveResumeUrl(Application application) {
        if (application.getResumeUrl() != null && !application.getResumeUrl().isBlank()) {
            return application.getResumeUrl();
        }
        StudentProfile profile = application.getStudent() != null ? application.getStudent().getStudentProfile() : null;
        return profile != null ? profile.getResumeUrl() : null;
    }

    private String firstNonBlank(String primary, String fallback) {
        String trimmedPrimary = trimToNull(primary);
        return trimmedPrimary != null ? trimmedPrimary : trimToNull(fallback);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateEmployerOwnership(Job job, User employer) {
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER")) {
            throw new RuntimeException("Only employers can review applications");
        }
        if (job == null || job.getEmployer() == null || !job.getEmployer().getId().equals(employer.getId())) {
            throw new RuntimeException("You can only manage applications for your own jobs");
        }
    }

    private ApplicationStatus parseApplicationStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Application status is required");
        }
        try {
            return ApplicationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid application status");
        }
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy) {
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortBy));
    }
}
