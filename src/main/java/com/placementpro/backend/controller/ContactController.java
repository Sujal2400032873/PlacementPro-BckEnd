package com.placementpro.backend.controller;

import com.placementpro.backend.entity.ContactMessage;
import com.placementpro.backend.service.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    @Autowired
    private ContactService contactService;

    @PostMapping
    public ResponseEntity<ContactMessage> sendContactMessage(@RequestParam String name,
                                                             @RequestParam String email,
                                                             @RequestParam(required = false) String organization,
                                                             @RequestParam String message) {
        ContactMessage saved = contactService.sendContactMessage(name, email, organization, message);
        return ResponseEntity.ok(saved);
    }
}
