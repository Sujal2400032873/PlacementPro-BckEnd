package com.placementpro.backend.controller;

import com.placementpro.backend.dto.ApplicationDTO;
import com.placementpro.backend.dto.EmployerApplicationDTO;
import com.placementpro.backend.dto.UpdateApplicationStatusRequest;
import com.placementpro.backend.security.CurrentUserService;
import com.placementpro.backend.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employer")
@RequiredArgsConstructor
public class EmployerController {

    private final ApplicationService applicationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<EmployerApplicationDTO>> getEmployerApplications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getEmployerApplications(
                currentUserService.getCurrentUser().getId(),
                page,
                size
        ));
    }

    @GetMapping("/applications/{jobId}")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<List<EmployerApplicationDTO>> getEmployerApplicationsByJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(applicationService.getApplicationsByJob(jobId, page, size));
    }

    @PutMapping("/application/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_EMPLOYER')")
    public ResponseEntity<ApplicationDTO> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody UpdateApplicationStatusRequest request
    ) {
        return ResponseEntity.ok(applicationService.updateStatus(id, request.getStatus()));
    }
}
