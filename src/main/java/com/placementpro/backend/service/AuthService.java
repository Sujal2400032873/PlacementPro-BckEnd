package com.placementpro.backend.service;

import com.placementpro.backend.dto.*;
import com.placementpro.backend.entity.*;
import com.placementpro.backend.repository.*;
import com.placementpro.backend.security.JwtUtils;
import com.placementpro.backend.security.UserDetailsImpl;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtService;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        log.info("LOGIN_ATTEMPT email={}", request.getEmail());

        User user = userRepository.findDetailedByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("LOGIN_FAILED reason=USER_NOT_FOUND email={}", request.getEmail());
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
                });

        log.info("User found: {}", user.getEmail());

        if (user.getPassword() == null) {
            log.error("Password is null in database for user: {}", user.getEmail());
            throw new RuntimeException("Password is null in database");
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatches) {
            log.warn("LOGIN_FAILED reason=INVALID_PASSWORD email={}", user.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        log.info("Password matched for user: {}", user.getEmail());

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            log.error("User roles are missing for user: {}", user.getEmail());
            throw new RuntimeException("User roles are missing");
        }

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        String token = jwtService.generateJwtToken(authentication);

        log.info("Login successful for user: {}", user.getEmail());

        String primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("STUDENT");

        log.info("LOGIN_SUCCESS email={} role={}", user.getEmail(), primaryRole);

        syncProfileCompletion(user);
        UserDTO userDTO = toUserDto(user);

        log.debug("Login response: token={}, user={}", token, userDTO);

        return AuthResponse.builder()
                .token(token)
                .user(userDTO)
                .build();
    }

    public MessageResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("REGISTER_FAILED reason=EMAIL_ALREADY_IN_USE email={}", registerRequest.getEmail());
            return new MessageResponse("Error: Email is already in use!");
        }

        String normalizedRole = "ROLE_STUDENT";
        Role role = roleRepository.findByName(normalizedRole)
                .orElseGet(() -> {
                    Role newRole = Role.builder().name(normalizedRole).build();
                    return roleRepository.save(newRole);
                });

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .enabled(true)
                .profileCompleted(false)
                .build();

        user.getRoles().add(role);
        initializeRoleProfile(user, normalizedRole);
        userRepository.save(user);

        log.info("REGISTER_SUCCESS email={} role={} emailVerificationRequired=false", user.getEmail(), normalizedRole);
        return new MessageResponse("User registered successfully.");
    }

    public UserDTO createUserByAdmin(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("ADMIN_CREATE_USER_FAILED reason=EMAIL_ALREADY_IN_USE email={}", registerRequest.getEmail());
            throw new RuntimeException("Email is already in use");
        }

        String normalizedRole = normalizeRole(registerRequest.getRole());
        Role role = roleRepository.findByName(normalizedRole)
                .orElseGet(() -> roleRepository.save(Role.builder().name(normalizedRole).build()));

        User user = User.builder()
                .name(registerRequest.getName())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .enabled(true)
                .profileCompleted("ROLE_ADMIN".equals(normalizedRole))
                .build();

        user.getRoles().add(role);
        initializeRoleProfile(user, normalizedRole);
        User savedUser = userRepository.save(user);
        log.info("ADMIN_CREATE_USER_SUCCESS email={} role={}", savedUser.getEmail(), normalizedRole);
        syncProfileCompletion(savedUser);
        return toUserDto(savedUser);
    }

    public MessageResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(2))
                .build();

        passwordResetTokenRepository.save(resetToken);
        log.info("PASSWORD_RESET_REQUESTED email={}", user.getEmail());
        return new MessageResponse("Password reset link created");
    }

    public MessageResponse resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);

        log.info("PASSWORD_RESET_SUCCESS email={}", user.getEmail());
        return new MessageResponse("Password has been reset successfully");
    }

    /** Get client IP from current request context */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role is required");
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
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

    private void initializeRoleProfile(User user, String role) {
        switch (role) {
            case "ROLE_STUDENT" -> {
                StudentProfile profile = StudentProfile.builder().user(user).build();
                user.setStudentProfile(profile);
            }
            case "ROLE_EMPLOYER" -> {
                EmployerProfile profile = EmployerProfile.builder().user(user).build();
                user.setEmployerProfile(profile);
            }
            case "ROLE_PLACEMENT_OFFICER" -> {
                PlacementProfile profile = PlacementProfile.builder().user(user).build();
                user.setPlacementProfile(profile);
            }
            case "ROLE_ADMIN" -> {
                // Admin has no role-specific profile
            }
            default -> throw new RuntimeException("Unsupported role");
        }
    }

    private StudentProfileDTO mapStudentProfile(StudentProfile profile) {
        if (profile == null) return null;
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
        if (profile == null) return null;
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
        if (profile == null) return null;
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

    private void syncProfileCompletion(User user) {
        boolean completed = isProfileComplete(user);
        if (user.isProfileCompleted() != completed) {
            user.setProfileCompleted(completed);
            userRepository.save(user);
        }
    }
}
