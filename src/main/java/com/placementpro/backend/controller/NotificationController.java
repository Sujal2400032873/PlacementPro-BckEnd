package com.placementpro.backend.controller;

import com.placementpro.backend.dto.CreateNotificationRequest;
import com.placementpro.backend.dto.NotificationDTO;
import com.placementpro.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotificationsForCurrentUser());
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationDTO> createNotification(@RequestBody CreateNotificationRequest request) {
        return ResponseEntity.ok(notificationService.createNotification(
                request.getUserId(),
                request.getMessage(),
                request.getType()
        ));
    }
}
