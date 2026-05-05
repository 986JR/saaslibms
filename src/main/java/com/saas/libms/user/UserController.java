package com.saas.libms.user;

import com.saas.libms.common.ApiResponse;
import com.saas.libms.security.CustomUserDetails;
import com.saas.libms.user.dto.UserCreateDTO;
import com.saas.libms.user.dto.UserResponseDTO;
import com.saas.libms.user.dto.UserUpdateDTO;
import jakarta.persistence.PrePersist;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final  UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> createUSer(
            @Valid @RequestBody UserCreateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        UserResponseDTO created = userService.createUser(dto,currentUser);
        return ResponseEntity
            .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully",created));

    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponseDTO>>> getAllUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<UserResponseDTO> users = userService.getAllUsers(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", users));

    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        UserResponseDTO user = userService.getCurrentUser(currentUser);
        return ResponseEntity.ok(ApiResponse.success("User profile fetched", user));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(
            @PathVariable String publicId,
            @Valid @RequestBody UserUpdateDTO dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
            ) {
        UserResponseDTO updated = userService.updateUser(publicId, dto, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully",updated));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String publicId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        userService.deleteUser(publicId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User disabled successfully", null));
    }
}
