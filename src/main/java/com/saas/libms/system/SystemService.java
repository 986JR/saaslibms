package com.saas.libms.system;

import com.saas.libms.audit.AuditAction;
import com.saas.libms.audit.AuditEntityType;
import com.saas.libms.audit.AuditLogService;
import com.saas.libms.audit.AuditMetadata;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.BadRequestException;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.institution.Institution;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.institution.InstitutionStatus;
import com.saas.libms.security.CustomUserDetails;
import com.saas.libms.system.dto.*;
import com.saas.libms.user.*;
import com.saas.libms.user.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public SystemDashboardStats getDashboardStats() {
        long totalInstitutions = institutionRepository.count();
        long activeInstitutions = institutionRepository.countByStatus(InstitutionStatus.ACTIVE);
        long suspendedInstitutions = institutionRepository.countByStatus(InstitutionStatus.SUSPENDED);
        long totalUsers = userRepository.count();
        long systemAdmins = userRepository.countByRole(UserRole.SYSTEM);

        return new SystemDashboardStats(
                totalInstitutions,
                activeInstitutions,
                suspendedInstitutions,
                totalUsers,
                systemAdmins
        );
    }

    @Transactional(readOnly = true)
    public Page<SystemInstitutionResponse> getAllInstitutions(String statusStr, String search, int page, int size) {
        InstitutionStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = InstitutionStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid institution status: " + statusStr);
            }
        }
        
        String searchPattern = null;
        if (search != null && !search.isBlank()) {
            searchPattern = "%" + search.trim().toLowerCase() + "%";
        }

        Pageable pageable = PageRequest.of(page, size);
        return institutionRepository.findAllGlobal(status, searchPattern, pageable)
                .map(SystemInstitutionResponse::from);
    }

    @Transactional
    public SystemInstitutionResponse updateInstitutionStatus(String publicId, String statusStr, CustomUserDetails currentUser) {
        Institution inst = institutionRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));

        if (inst.getPublicId().equals("INST-SYSTEM")) {
            throw new BadRequestException("Cannot modify the system management institution.");
        }

        try {
            InstitutionStatus status = InstitutionStatus.valueOf(statusStr.toUpperCase());
            inst.setStatus(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid institution status: " + statusStr);
        }

        inst.setUpdatedAt(LocalDateTime.now());
        Institution saved = institutionRepository.save(inst);

        log.info("[SystemService] System user {} changed institution {} status to {}",
                currentUser.getUsername(), publicId, statusStr);

        return SystemInstitutionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(String institutionPublicId, String roleStr, int page, int size) {
        UUID instId = null;
        if (institutionPublicId != null && !institutionPublicId.isBlank()) {
            Institution inst = institutionRepository.findByPublicId(institutionPublicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found."));
            instId = inst.getId();
        }

        UserRole role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = UserRole.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role value: " + roleStr);
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAllGlobal(instId, role, pageable)
                .map(UserResponseDTO::from);
    }

    @Transactional
    public UserResponseDTO createUser(SystemUserCreateRequest request, CustomUserDetails currentUser) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("A user with this email already exists");
        }

        Institution institution;
        if (request.role() == UserRole.SYSTEM) {
            institution = institutionRepository.findByPublicId("INST-SYSTEM")
                    .orElseThrow(() -> new ResourceNotFoundException("System institution not seeded."));
        } else {
            if (request.institutionPublicId() == null || request.institutionPublicId().isBlank()) {
                throw new BadRequestException("Institution ID is required for non-system users");
            }
            institution = institutionRepository.findByPublicId(request.institutionPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
        }

        User user = new User();
        user.setPublicId(PublicIdGenerator.generate("USER"));
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setInstitution(institution);
        user.setEmailVerified(true);

        User saved = userRepository.save(user);

        log.info("[SystemService] System user {} created user {} with role {} in institution {}",
                currentUser.getUsername(), saved.getPublicId(), saved.getRole(), institution.getPublicId());

        auditLogService.log(
                currentUser,
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                saved.getPublicId(),
                AuditMetadata.builder()
                        .put("username", saved.getUsername())
                        .put("email", saved.getEmail())
                        .put("role", saved.getRole().name())
                        .build()
        );

        return UserResponseDTO.from(saved);
    }

    @Transactional
    public UserResponseDTO updateUser(String publicId, SystemUserUpdateRequest request, CustomUserDetails currentUser) {
        User target = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.getPublicId().equals(currentUser.getUser().getPublicId())) {
            // Self update rules
            if (request.role() != target.getRole()) {
                throw new BadRequestException("You cannot change your own role");
            }
            if (request.status() != target.getStatus()) {
                throw new BadRequestException("You cannot change your own status");
            }
        }

        if (userRepository.existsByEmail(request.email()) && !request.email().equalsIgnoreCase(target.getEmail())) {
            throw new ConflictException("This email is already in use");
        }

        Institution institution;
        if (request.role() == UserRole.SYSTEM) {
            institution = institutionRepository.findByPublicId("INST-SYSTEM")
                    .orElseThrow(() -> new ResourceNotFoundException("System institution not seeded"));
        } else {
            if (request.institutionPublicId() == null || request.institutionPublicId().isBlank()) {
                throw new BadRequestException("Institution ID is required for non-system users");
            }
            institution = institutionRepository.findByPublicId(request.institutionPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));
        }

        target.setUsername(request.username());
        target.setEmail(request.email());
        target.setRole(request.role());
        target.setStatus(request.status());
        target.setInstitution(institution);

        User saved = userRepository.save(target);

        log.info("[SystemService] System user {} updated user {}", currentUser.getUsername(), saved.getPublicId());

        auditLogService.log(
                currentUser,
                AuditAction.USER_UPDATED,
                AuditEntityType.USER,
                saved.getPublicId(),
                AuditMetadata.builder()
                        .put("username", saved.getUsername())
                        .put("email", saved.getEmail())
                        .put("role", saved.getRole().name())
                        .put("status", saved.getStatus().name())
                        .build()
        );

        return UserResponseDTO.from(saved);
    }

    @Transactional
    public void deleteUser(String publicId, CustomUserDetails currentUser) {
        User target = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (target.getPublicId().equals(currentUser.getUser().getPublicId())) {
            throw new BadRequestException("You cannot delete your own account");
        }

        // Soft delete
        target.setStatus(UserStatus.DISABLED);
        userRepository.save(target);

        log.info("[SystemService] System user {} disabled user {}", currentUser.getUsername(), publicId);

        auditLogService.log(
                currentUser,
                AuditAction.USER_DELETED,
                AuditEntityType.USER,
                publicId,
                AuditMetadata.builder()
                        .put("username", target.getUsername())
                        .put("email", target.getEmail())
                        .put("role", target.getRole().name())
                        .build()
        );
    }
}
