package com.placementpro.backend.repository;

import com.placementpro.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.user u
            WHERE u.id = :userId
            ORDER BY n.createdAt DESC
            """)
    List<Notification> findByUserId(@Param("userId") Long userId);

    List<Notification> findByType(String type);
    List<Notification> findTop5ByOrderByCreatedAtDesc();

        @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.user u
            WHERE u.id = :userId AND n.isRead = false
            ORDER BY n.createdAt DESC
            """)
        List<Notification> findUnreadByUserId(@Param("userId") Long userId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.user.id = :userId AND n.isRead = false
            """)
        int markAllAsReadByUserId(@Param("userId") Long userId);

        @Modifying(clearAutomatically = true, flushAutomatically = true)
        @Transactional
        @Query("""
            UPDATE Notification n
            SET n.isRead = true
            WHERE n.id = :notificationId AND n.user.id = :userId AND n.isRead = false
            """)
        int markAsReadByIdAndUserId(@Param("notificationId") Long notificationId, @Param("userId") Long userId);

        @Query("""
            SELECT n FROM Notification n
            JOIN FETCH n.user u
            WHERE u.id = :userId
              AND n.message = :message
              AND n.type = :type
              AND n.createdAt >= :afterTime
            ORDER BY n.createdAt DESC
            """)
        List<Notification> findRecentDuplicates(
            @Param("userId") Long userId,
            @Param("message") String message,
            @Param("type") String type,
            @Param("afterTime") LocalDateTime afterTime
        );

    void deleteByUser_Id(Long userId);
}
