package com.placementpro.backend.service;

import com.placementpro.backend.entity.ContactMessage;
import com.placementpro.backend.repository.ContactMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ContactService {

    @Autowired
    private ContactMessageRepository contactMessageRepository;

    public ContactMessage sendContactMessage(String name, String email, String organization, String message) {
        ContactMessage contactMessage = ContactMessage.builder()
                .name(name)
                .email(email)
                .organization(organization)
                .message(message)
                .status("NEW")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ContactMessage saved = contactMessageRepository.save(contactMessage);
        return saved;
    }
}
