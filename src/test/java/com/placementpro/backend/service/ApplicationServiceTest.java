package com.placementpro.backend.service;

import com.placementpro.backend.dto.ApplicationDTO;
import com.placementpro.backend.dto.ApplicationRequest;
import com.placementpro.backend.entity.Application;
import com.placementpro.backend.entity.ApplicationStatus;
import com.placementpro.backend.entity.Job;
import com.placementpro.backend.entity.JobStatus;
import com.placementpro.backend.entity.Role;
import com.placementpro.backend.entity.StudentProfile;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.ApplicationRepository;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private ApplicationService applicationService;

    private User student;
    private User employer;

    @BeforeEach
    void setUp() {
        student = User.builder()
                .id(7L)
                .email("student@test.com")
                .roles(Set.of(Role.builder().name("ROLE_STUDENT").build()))
                .build();
        student.setStudentProfile(StudentProfile.builder()
                .user(student)
                .resumeUrl("https://example.com/resume")
                .build());

        employer = User.builder()
                .id(10L)
                .email("employer@test.com")
                .roles(Set.of(Role.builder().name("ROLE_EMPLOYER").build()))
                .build();
    }

    @Test
    void applyForJobRejectsClosedJob() {
        Job job = Job.builder().id(1L).status(JobStatus.CLOSED).employer(employer).build();

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(currentUserService.hasRole(student, "ROLE_STUDENT")).thenReturn(true);
        when(applicationRepository.existsByJob_IdAndStudent_Id(1L, 7L)).thenReturn(false);
        when(jobRepository.findDetailedById(1L)).thenReturn(Optional.of(job));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(ApplicationRequest.builder().jobId(1L).build()));

        assertEquals("Applications are only allowed for open jobs", ex.getMessage());
    }

    @Test
    void applyForJobRequiresResumeUrl() {
        student.getStudentProfile().setResumeUrl(" ");
        Job job = Job.builder().id(1L).status(JobStatus.OPEN).employer(employer).build();

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(currentUserService.hasRole(student, "ROLE_STUDENT")).thenReturn(true);
        when(applicationRepository.existsByJob_IdAndStudent_Id(1L, 7L)).thenReturn(false);
        when(jobRepository.findDetailedById(1L)).thenReturn(Optional.of(job));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.applyForJob(ApplicationRequest.builder().jobId(1L).build()));

        assertEquals("Please upload resume before applying", ex.getMessage());
    }

    @Test
    void applyForJobSnapshotsResumeUrlAndAppliedStatus() {
        Job job = Job.builder().id(1L).status(JobStatus.OPEN).employer(employer).title("Java Dev").companyName("Tech").build();
        Application saved = Application.builder()
                .id(11L)
                .job(job)
                .student(student)
                .status(ApplicationStatus.APPLIED)
                .resumeUrl("https://example.com/resume")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(student);
        when(currentUserService.hasRole(student, "ROLE_STUDENT")).thenReturn(true);
        when(applicationRepository.existsByJob_IdAndStudent_Id(1L, 7L)).thenReturn(false);
        when(jobRepository.findDetailedById(1L)).thenReturn(Optional.of(job));
        when(applicationRepository.save(any(Application.class))).thenReturn(saved);

        ApplicationDTO result = applicationService.applyForJob(ApplicationRequest.builder()
                .jobId(1L)
                .notes("Interested")
                .build());

        ArgumentCaptor<Application> captor = ArgumentCaptor.forClass(Application.class);
        verify(applicationRepository).save(captor.capture());
        Application persisted = captor.getValue();

        assertEquals(ApplicationStatus.APPLIED, persisted.getStatus());
        assertEquals("https://example.com/resume", persisted.getResumeUrl());
        assertEquals("APPLIED", result.getStatus());
        assertEquals("https://example.com/resume", result.getResumeUrl());
    }

    @Test
    void getApplicationsByStudentRejectsAnotherStudent() {
        User otherStudent = User.builder()
                .id(99L)
                .roles(Set.of(Role.builder().name("ROLE_STUDENT").build()))
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(otherStudent);
        when(currentUserService.hasRole(otherStudent, "ROLE_STUDENT")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.getApplicationsByStudent(7L, 0, 20));

        assertEquals("You can only view your own applications", ex.getMessage());
    }

    @Test
    void updateStatusRejectsInvalidStatus() {
        Job job = Job.builder().id(1L).employer(employer).status(JobStatus.OPEN).build();
        Application application = Application.builder().id(3L).job(job).status(ApplicationStatus.APPLIED).build();

        when(currentUserService.getCurrentUser()).thenReturn(employer);
        when(currentUserService.hasRole(employer, "ROLE_EMPLOYER")).thenReturn(true);
        when(applicationRepository.findDetailedById(3L)).thenReturn(Optional.of(application));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> applicationService.updateStatus(3L, "INTERVIEW"));

        assertEquals("Invalid application status", ex.getMessage());
    }
}
