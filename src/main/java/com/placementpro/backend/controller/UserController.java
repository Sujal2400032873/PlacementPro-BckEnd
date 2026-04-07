package com.placementpro.backend.controller;

import com.placementpro.backend.dto.RoleUpdateRequest;
import com.placementpro.backend.dto.UserDTO;
import com.placementpro.backend.dto.StudentProfileDTO;
import com.placementpro.backend.dto.EmployerProfileDTO;
import com.placementpro.backend.dto.PlacementProfileDTO;
import com.placementpro.backend.dto.RegisterRequest;
import com.placementpro.backend.entity.EmployerProfile;
import com.placementpro.backend.entity.PlacementProfile;
import com.placementpro.backend.entity.Role;
import com.placementpro.backend.entity.StudentProfile;
import com.placementpro.backend.entity.User;
import com.placementpro.backend.repository.UserRepository;
import com.placementpro.backend.repository.RoleRepository;
import com.placementpro.backend.repository.ApplicationRepository;
import com.placementpro.backend.repository.JobRepository;
import com.placementpro.backend.repository.NotificationRepository;
import com.placementpro.backend.repository.PasswordResetTokenRepository;
import com.placementpro.backend.security.CurrentUserService;
import com.placementpro.backend.service.AuthService;
import jakarta.validation.Valid;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.lang.NonNull;

import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private AuthService authService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/profile/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String email) {
        User currentUser = currentUserService.getCurrentUser();
        boolean isAdmin = currentUserService.hasRole(currentUser, "ROLE_ADMIN");
        if (!isAdmin && !currentUser.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("You can only view your own profile");
        }

        User user = userRepository.findDetailedByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(toUserDto(user));
    }

    @PutMapping("/profile/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> updateProfile(@PathVariable @NonNull Long id, @Valid @RequestBody UserDTO userDTO) {
        User currentUser = currentUserService.getCurrentUser();
        if (!currentUser.getId().equals(id)) {
            throw new RuntimeException("You can only update your own profile");
        }

        User user = userRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());
        applyRoleSpecificProfileUpdate(user, userDTO);
        user.setProfileCompleted(isProfileComplete(user));
        
        User updatedUser = userRepository.save(user);
        return ResponseEntity.ok(toUserDto(updatedUser));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody RegisterRequest registerRequest) {
        User admin = currentUserService.getCurrentUser();
        UserDTO createdUser = authService.createUserByAdmin(registerRequest);
        logger.info("ADMIN_ACTION action=CREATE_USER admin={} targetUserEmail={} role={}",
                admin.getEmail(), createdUser.getEmail(), createdUser.getRole());
        return ResponseEntity.ok(createdUser);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAllDetailed().stream()
                .map(this::toUserDto)
                .toList());
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<UserDTO> updateUserRole(@PathVariable @NonNull Long id, @RequestBody RoleUpdateRequest roleUpdateRequest) {
        User admin = currentUserService.getCurrentUser();
        User user = userRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(normalizeRole(roleUpdateRequest.getRole()))
                .orElseThrow(() -> new RuntimeException("Role not found"));

        user.setRoles(new HashSet<>(List.of(role)));
        User updatedUser = userRepository.save(user);
        logger.info("ADMIN_ACTION action=ROLE_CHANGED admin={} targetUser={} newRole={}",
                admin.getEmail(), updatedUser.getEmail(), role.getName());
        return ResponseEntity.ok(toUserDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable @NonNull Long id) {
        User admin = currentUserService.getCurrentUser();
        User targetUser = userRepository.findDetailedById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (admin.getId().equals(targetUser.getId())) {
            throw new RuntimeException("You cannot delete your own account");
        }

        for (com.placementpro.backend.entity.Job job : jobRepository.findByEmployer_Id(id)) {
            applicationRepository.deleteByJob_Id(job.getId());
        }

        applicationRepository.deleteByStudent_Id(id);
        jobRepository.deleteByEmployer_Id(id);
        notificationRepository.deleteByUser_Id(id);
        passwordResetTokenRepository.deleteByUser_Id(id);
        entityManager.createNativeQuery("DELETE FROM email_verification_tokens WHERE user_id = :userId")
            .setParameter("userId", id)
            .executeUpdate();

        targetUser.getRoles().clear();
        userRepository.save(targetUser);
        userRepository.delete(targetUser);
        logger.info("ADMIN_ACTION action=DELETE_USER admin={} targetUser={} targetUserId={}",
                admin.getEmail(), targetUser.getEmail(), id);
        return ResponseEntity.ok().build();
    }

    private UserDTO toUserDto(User user) {
        UserDTO userDTO = modelMapper.map(user, UserDTO.class);
        userDTO.setRole(user.getRoles().stream().findFirst()
                .map(Role::getName)
                .orElseThrow(() -> new RuntimeException("User role not found")));
        userDTO.setStudentProfile(mapStudentProfile(user.getStudentProfile()));
        userDTO.setEmployerProfile(mapEmployerProfile(user.getEmployerProfile()));
        userDTO.setPlacementProfile(mapPlacementProfile(user.getPlacementProfile()));
        boolean profileCompleted = isProfileComplete(user);
        userDTO.setProfileComplete(profileCompleted);
        userDTO.setProfileCompleted(profileCompleted);
        userDTO.setCreatedAt(user.getCreatedAt());
        return userDTO;
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role is required");
        }

        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }

    private void applyRoleSpecificProfileUpdate(User user, UserDTO userDTO) {
        String role = user.getRoles().stream().findFirst()
                .map(Role::getName)
                .orElseThrow(() -> new RuntimeException("User role not found"));

        switch (role) {
            case "ROLE_STUDENT" -> updateStudentProfile(user, userDTO.getStudentProfile());
            case "ROLE_EMPLOYER" -> updateEmployerProfile(user, userDTO.getEmployerProfile());
            case "ROLE_PLACEMENT_OFFICER" -> updatePlacementProfile(user, userDTO.getPlacementProfile());
            case "ROLE_ADMIN" -> {
                // Admin has no self-managed role profile in Phase 2.
            }
            default -> throw new RuntimeException("Unsupported role");
        }
    }

    private void updateStudentProfile(User user, StudentProfileDTO profileDTO) {
        if (profileDTO == null) {
            return;
        }
        StudentProfile profile = user.getStudentProfile();
        if (profile == null) {
            profile = StudentProfile.builder().user(user).build();
            user.setStudentProfile(profile);
        }

        profile.setRollNumber(profileDTO.getRollNumber());
        profile.setBranch(profileDTO.getBranch());
        profile.setSkills(profileDTO.getSkills());
        profile.setCgpa(profileDTO.getCgpa());
        profile.setGraduationYear(profileDTO.getGraduationYear());
        profile.setResumeUrl(profileDTO.getResumeUrl());
    }

    private void updateEmployerProfile(User user, EmployerProfileDTO profileDTO) {
        if (profileDTO == null) {
            return;
        }
        EmployerProfile profile = user.getEmployerProfile();
        if (profile == null) {
            profile = EmployerProfile.builder().user(user).build();
            user.setEmployerProfile(profile);
        }

        profile.setCompanyName(profileDTO.getCompanyName());
        profile.setIndustry(profileDTO.getIndustry());
        profile.setCompanySize(profileDTO.getCompanySize());
        profile.setWebsite(profileDTO.getWebsite());
        profile.setDescription(profileDTO.getDescription());
        profile.setHrContact(profileDTO.getHrContact());
    }

    private void updatePlacementProfile(User user, PlacementProfileDTO profileDTO) {
        if (profileDTO == null) {
            return;
        }
        PlacementProfile profile = user.getPlacementProfile();
        if (profile == null) {
            profile = PlacementProfile.builder().user(user).build();
            user.setPlacementProfile(profile);
        }

        profile.setDepartment(profileDTO.getDepartment());
        profile.setDesignation(profileDTO.getDesignation());
        profile.setCollegeName(profileDTO.getCollegeName());
        profile.setContactNumber(profileDTO.getContactNumber());
    }

    private StudentProfileDTO mapStudentProfile(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        return StudentProfileDTO.builder()
                .rollNumber(profile.getRollNumber())
                .branch(profile.getBranch())
                .skills(profile.getSkills())
                .cgpa(profile.getCgpa())
                .graduationYear(profile.getGraduationYear())
                .resumeUrl(profile.getResumeUrl())
                .build();
    }

    private EmployerProfileDTO mapEmployerProfile(EmployerProfile profile) {
        if (profile == null) {
            return null;
        }
        return EmployerProfileDTO.builder()
                .companyName(profile.getCompanyName())
                .industry(profile.getIndustry())
                .companySize(profile.getCompanySize())
                .website(profile.getWebsite())
                .description(profile.getDescription())
                .hrContact(profile.getHrContact())
                .build();
    }

    private PlacementProfileDTO mapPlacementProfile(PlacementProfile profile) {
        if (profile == null) {
            return null;
        }
        return PlacementProfileDTO.builder()
                .department(profile.getDepartment())
                .designation(profile.getDesignation())
                .collegeName(profile.getCollegeName())
                .contactNumber(profile.getContactNumber())
                .build();
    }

    private boolean isProfileComplete(User user) {
        String role = user.getRoles().stream().findFirst()
                .map(Role::getName)
                .orElse("");

        return switch (role) {
            case "ROLE_ADMIN" -> true;
            case "ROLE_STUDENT" -> isStudentProfileComplete(user.getStudentProfile());
            case "ROLE_EMPLOYER" -> isEmployerProfileComplete(user.getEmployerProfile());
            case "ROLE_PLACEMENT_OFFICER" -> isPlacementProfileComplete(user.getPlacementProfile());
            default -> false;
        };
    }

    private boolean isStudentProfileComplete(StudentProfile profile) {
        return profile != null
                && hasText(profile.getRollNumber())
                && hasText(profile.getBranch())
                && hasText(profile.getSkills())
                && profile.getCgpa() != null
                && profile.getGraduationYear() != null
                && hasText(profile.getResumeUrl());
    }

    private boolean isEmployerProfileComplete(EmployerProfile profile) {
        return profile != null
                && hasText(profile.getCompanyName())
                && hasText(profile.getIndustry())
                && hasText(profile.getCompanySize())
                && hasText(profile.getWebsite())
                && hasText(profile.getDescription())
                && hasText(profile.getHrContact());
    }

    private boolean isPlacementProfileComplete(PlacementProfile profile) {
        return profile != null
                && hasText(profile.getDepartment())
                && hasText(profile.getDesignation())
                && hasText(profile.getCollegeName())
                && hasText(profile.getContactNumber());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
