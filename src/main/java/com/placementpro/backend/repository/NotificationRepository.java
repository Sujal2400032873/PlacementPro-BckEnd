package com.placementpro.backend.repository;

import com.placementpro.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    void deleteByUser_Id(Long userId);
}
