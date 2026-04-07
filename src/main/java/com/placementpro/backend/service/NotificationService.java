package com.placementpro.backend.service;

import com.placementpro.backend.dto.NotificationDTO;
import com.placementpro.backend.entity.Notification;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.NotificationRepository;
import com.placementpro.backend.repository.UserRepository;
import com.placementpro.backend.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public NotificationDTO createNotification(Long userId, String message, String type) {
        if (userId == null) {
            throw new RuntimeException("Notification user is required");
        }
        if (message == null || message.isBlank()) {
            throw new RuntimeException("Notification message is required");
        }

        User user = userRepository.findDetailedById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message.trim())
                .type((type == null || type.isBlank()) ? "info" : type.trim().toUpperCase())
                .isRead(false)
                .build();

        return toDto(notificationRepository.save(notification));
    }

    public List<NotificationDTO> getNotificationsForCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();
        return notificationRepository.findByUserId(currentUser.getId()).stream()
                .map(this::toDto)
                .toList();
    }

    public void notifyUser(Long userId, String message, String type) {
        createNotification(userId, message, type);
    }

    private NotificationDTO toDto(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
