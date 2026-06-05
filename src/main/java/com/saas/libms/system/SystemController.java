package com.saas.libms.system;

import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import com.saas.libms.system.dto.*;
import com.saas.libms.user.dto.UserResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
@PreAuthorize("hasRole('SYSTEM')")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<SystemDashboardStats>> getDashboardStats() {
        SystemDashboardStats stats = systemService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("System dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/institutions")
    public ResponseEntity<ApiResponse<Page<SystemInstitutionResponse>>> getAllInstitutions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<SystemInstitutionResponse> data = systemService.getAllInstitutions(status, search, page, size);
        return ResponseEntity.ok(ApiResponse.success("Institutions retrieved successfully", data));
    }

    @PatchMapping("/institutions/{publicId}/status")
    public ResponseEntity<ApiResponse<SystemInstitutionResponse>> updateInstitutionStatus(
            @PathVariable String publicId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        String status = body.get("status");
        SystemInstitutionResponse updated = systemService.updateInstitutionStatus(publicId, status, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Institution status updated successfully", updated));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponseDTO>>> getAllUsers(
            @RequestParam(required = false) String institutionPublicId,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponseDTO> data = systemService.getAllUsers(institutionPublicId, role, page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", data));
    }

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUser(
            @Valid @RequestBody SystemUserCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponseDTO created = systemService.createUser(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", created));
    }

    @PatchMapping("/users/{publicId}")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(
            @PathVariable String publicId,
            @Valid @RequestBody SystemUserUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponseDTO updated = systemService.updateUser(publicId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", updated));
    }

    @DeleteMapping("/users/{publicId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        systemService.deleteUser(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User disabled successfully", null));
    }
}
