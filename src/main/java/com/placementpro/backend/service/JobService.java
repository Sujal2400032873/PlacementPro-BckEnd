package com.placementpro.backend.service;

import com.placementpro.backend.dto.JobDTO;
import com.placementpro.backend.entity.Job;
import com.placementpro.backend.entity.JobStatus;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.security.CurrentUserService;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<JobDTO> getAllActiveJobs(Integer page, Integer size) {
        Pageable pageable = buildPageable(page, size, "postedAt");
        return jobRepository.findDetailedByStatus(JobStatus.OPEN, pageable).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JobDTO> getJobsByEmployer(Long employerId, Integer page, Integer size) {
        User employer = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER") || !employer.getId().equals(employerId)) {
            throw new RuntimeException("You can only view your own jobs");
        }

        Pageable pageable = buildPageable(page, size, "postedAt");
        return jobRepository.findDetailedByEmployerId(employerId, pageable).stream()
                .map(this::convertToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public JobDTO getJobById(Long id) {
        Job job = jobRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("Job not found with id: " + id));
        return convertToDto(job);
    }

    @CacheEvict(cacheNames = {"adminDashboard", "officerDashboard"}, allEntries = true)
    public JobDTO createJob(JobDTO jobDTO) {
        User employer = currentUserService.getCurrentUser();
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER")) {
            throw new RuntimeException("Only employers can create jobs");
        }

        validateJobPayload(jobDTO);
        
        Job job = modelMapper.map(jobDTO, Job.class);
        if (job == null) {
            job = new Job();
        }
        job.setId(null); // Ensure creation
        job.setTitle(jobDTO.getTitle());
        job.setCompanyName(jobDTO.getCompanyName());
        job.setLocation(jobDTO.getLocation());
        job.setDescription(jobDTO.getDescription());
        job.setRequirements(jobDTO.getRequirements());
        job.setSalary(jobDTO.getSalary());
        job.setType(jobDTO.getType());
        job.setEmployer(employer);
        job.setPostedAt(LocalDateTime.now());
        job.setStatus(parseJobStatus(jobDTO.getStatus(), JobStatus.OPEN));
        
        Job savedJob = jobRepository.save(job);
        return convertToDto(savedJob);
    }

    @CacheEvict(cacheNames = {"adminDashboard", "officerDashboard"}, allEntries = true)
    public JobDTO updateJob(Long id, JobDTO jobDTO) {
        User employer = currentUserService.getCurrentUser();
        Job existingJob = jobRepository.findDetailedById(id)
                .or(() -> jobRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Job not found"));
        validateEmployerOwnership(existingJob, employer);

        validateJobPayload(jobDTO);
        
        existingJob.setTitle(jobDTO.getTitle());
        existingJob.setCompanyName(jobDTO.getCompanyName());
        existingJob.setLocation(jobDTO.getLocation());
        existingJob.setDescription(jobDTO.getDescription());
        existingJob.setRequirements(jobDTO.getRequirements());
        existingJob.setSalary(jobDTO.getSalary());
        existingJob.setType(jobDTO.getType());
        existingJob.setStatus(parseJobStatus(jobDTO.getStatus(), existingJob.getStatus()));
        
        Job updatedJob = jobRepository.save(existingJob);
        return convertToDto(updatedJob);
    }

    private void validateJobPayload(JobDTO jobDTO) {
        if (jobDTO == null) {
            throw new RuntimeException("Job data is required");
        }

        if (jobDTO.getCompanyName() == null || jobDTO.getCompanyName().isBlank()) {
            throw new RuntimeException("Company name is required");
        }
    }

    @CacheEvict(cacheNames = {"adminDashboard", "officerDashboard"}, allEntries = true)
    public void deleteJob(Long id) {
        User employer = currentUserService.getCurrentUser();
        Job existingJob = jobRepository.findDetailedById(id)
                .or(() -> jobRepository.findById(id))
                .orElseThrow(() -> new RuntimeException("Job not found"));
        validateEmployerOwnership(existingJob, employer);
        jobRepository.delete(existingJob);
    }

    private JobDTO convertToDto(Job job) {
        JobDTO dto = modelMapper.map(job, JobDTO.class);
        if (dto == null) {
            dto = new JobDTO();
        }
        dto.setId(job.getId());
        dto.setTitle(job.getTitle());
        dto.setCompanyName(job.getCompanyName());
        dto.setLocation(job.getLocation());
        dto.setDescription(job.getDescription());
        dto.setRequirements(job.getRequirements());
        dto.setSalary(job.getSalary());
        dto.setType(job.getType());
        dto.setStatus(job.getStatus() != null ? job.getStatus().name() : null);
        dto.setPostedAt(job.getPostedAt());
        if (job.getEmployer() != null) {
            dto.setEmployerId(job.getEmployer().getId());
        }
        return dto;
    }

    private JobStatus parseJobStatus(String status, JobStatus fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        try {
            return JobStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid job status");
        }
    }

    private void validateEmployerOwnership(Job job, User employer) {
        if (!currentUserService.hasRole(employer, "ROLE_EMPLOYER")) {
            throw new RuntimeException("Only employers can manage jobs");
        }
        if (job.getEmployer() == null || !job.getEmployer().getId().equals(employer.getId())) {
            throw new RuntimeException("You can only manage your own jobs");
        }
    }

    private Pageable buildPageable(Integer page, Integer size, String sortBy) {
        int safePage = page == null || page < 0 ? DEFAULT_PAGE : page;
        int safeSize = size == null || size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortBy));
    }
}
