package com.placementpro.backend.repository;

import com.placementpro.backend.entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    List<ContactMessage> findByEmail(String email);
}
