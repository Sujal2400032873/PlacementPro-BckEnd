package com.placementpro.backend.service;

import com.placementpro.backend.dto.JobDTO;
import com.placementpro.backend.entity.Job;
import com.placementpro.backend.entity.JobStatus;
import com.placementpro.backend.entity.Role;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private JobService jobService;

    private User employer;

    @BeforeEach
    void setUp() {
        employer = User.builder()
                .id(10L)
                .email("employer@test.com")
                .roles(Set.of(Role.builder().name("ROLE_EMPLOYER").build()))
                .build();
    }

    @Test
    void createJobUsesAuthenticatedEmployerAndDefaultsToOpen() {
        JobDTO input = JobDTO.builder()
                .title("Backend Engineer")
                .companyName("Tech Corp")
                .location("Remote")
                .description("Build APIs")
                .requirements("Java")
                .salary("10 LPA")
                .type("Full-time")
                .build();

        Job mappedJob = Job.builder().build();
        Job savedJob = Job.builder()
                .id(99L)
                .title("Backend Engineer")
                .companyName("Tech Corp")
                .status(JobStatus.OPEN)
                .employer(employer)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(employer);
        when(currentUserService.hasRole(employer, "ROLE_EMPLOYER")).thenReturn(true);
        when(modelMapper.map(input, Job.class)).thenReturn(mappedJob);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);
        when(modelMapper.map(savedJob, JobDTO.class)).thenReturn(JobDTO.builder().build());

        JobDTO result = jobService.createJob(input);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job persisted = captor.getValue();

        assertEquals(employer, persisted.getEmployer());
        assertEquals(JobStatus.OPEN, persisted.getStatus());
        assertEquals(10L, result.getEmployerId());
        assertEquals("OPEN", result.getStatus());
    }

    @Test
    void updateJobRejectsAnotherEmployersJob() {
        User otherEmployer = User.builder().id(20L).build();
        Job existing = Job.builder()
                .id(1L)
                .employer(otherEmployer)
                .status(JobStatus.OPEN)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(employer);
        when(currentUserService.hasRole(employer, "ROLE_EMPLOYER")).thenReturn(true);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jobService.updateJob(1L, JobDTO.builder().status("CLOSED").build()));

        assertEquals("You can only manage your own jobs", ex.getMessage());
    }

    @Test
    void updateJobAppliesExplicitStatus() {
        Job existing = Job.builder()
                .id(1L)
                .employer(employer)
                .status(JobStatus.OPEN)
                .build();
        JobDTO input = JobDTO.builder()
                .title("Updated")
                .companyName("Tech Corp")
                .location("Pune")
                .description("Updated desc")
                .requirements("Spring")
                .salary("12 LPA")
                .type("Full-time")
                .status("CLOSED")
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(employer);
        when(currentUserService.hasRole(employer, "ROLE_EMPLOYER")).thenReturn(true);
        when(jobRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(jobRepository.save(existing)).thenReturn(existing);
        when(modelMapper.map(existing, JobDTO.class)).thenReturn(new JobDTO());

        jobService.updateJob(1L, input);

        assertEquals(JobStatus.CLOSED, existing.getStatus());
    }
}
