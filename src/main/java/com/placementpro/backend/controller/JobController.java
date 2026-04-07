package com.placementpro.backend.controller;

import com.placementpro.backend.dto.JobDTO;
import com.placementpro.backend.service.JobService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobService jobService;

    @GetMapping
    public ResponseEntity<List<JobDTO>> getAllJobs(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(jobService.getAllActiveJobs(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobDTO> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(jobService.getJobById(id));
    }

    @GetMapping("/employer/{employerId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<JobDTO>> getJobsByEmployer(
            @PathVariable Long employerId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(jobService.getJobsByEmployer(employerId, page, size));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<JobDTO> createJob(@Valid @RequestBody JobDTO jobDTO) {
        return ResponseEntity.ok(jobService.createJob(jobDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<JobDTO> updateJob(@PathVariable Long id, @Valid @RequestBody JobDTO jobDTO) {
        return ResponseEntity.ok(jobService.updateJob(id, jobDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}
