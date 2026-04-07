package com.placementpro.backend.controller;

import com.placementpro.backend.dto.OfficerDashboardDTO;
import com.placementpro.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/placement")
@RequiredArgsConstructor
public class PlacementDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_PLACEMENT_OFFICER')")
    public ResponseEntity<OfficerDashboardDTO> dashboard() {
        return ResponseEntity.ok(dashboardService.getOfficerDashboard());
    }
}
