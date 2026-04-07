package com.placementpro.backend.controller;

import com.placementpro.backend.dto.ApplicationDTO;
import com.placementpro.backend.dto.ApplicationRequest;
import com.placementpro.backend.dto.EmployerApplicationDTO;
import com.placementpro.backend.dto.UpdateApplicationStatusRequest;
import com.placementpro.backend.service.ApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<ApplicationDTO> applyForJob(@Valid @RequestBody ApplicationRequest applicationRequest) {
        return ResponseEntity.ok(applicationService.applyForJob(applicationRequest));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    public ResponseEntity<List<ApplicationDTO>> getStudentApplications(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getApplicationsByStudent(studentId, page, size));
    }

    @GetMapping("/job/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<EmployerApplicationDTO>> getJobApplications(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, page, size));
    }

    @GetMapping("/employer/applications/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<EmployerApplicationDTO>> getEmployerApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, page, size));
    }

    @GetMapping("/employer/{employerId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<ApplicationDTO>> getEmployerApplications(
            @PathVariable Long employerId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getApplicationsByEmployer(employerId, page, size));
    }

    @PutMapping("/employer/application/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<ApplicationDTO> updateEmployerStatus(@PathVariable Long id, @RequestBody UpdateApplicationStatusRequest statusRequest) {
        return ResponseEntity.ok(applicationService.updateStatus(id, statusRequest.getStatus()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<ApplicationDTO> updateStatus(@PathVariable Long id, @RequestBody UpdateApplicationStatusRequest statusRequest) {
        return ResponseEntity.ok(applicationService.updateStatus(id, statusRequest.getStatus()));
    }
}
