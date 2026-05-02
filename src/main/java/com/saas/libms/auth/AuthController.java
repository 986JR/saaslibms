package com.saas.libms.auth;

import com.saas.libms.auth.dto.RegisterInstitutionRequest;
import com.saas.libms.auth.dto.RegisterInstitutionResponse;
import com.saas.libms.auth.dto.SetUpAdminRequest;
import com.saas.libms.auth.dto.VerifyInstitutionRequest;
import com.saas.libms.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final InstitutionAuthService institutionAuthService;

    //register Institution
    @PostMapping("/institution/register")
    public ResponseEntity<ApiResponse<RegisterInstitutionResponse>> registerInstitution(
            @Valid @RequestBody RegisterInstitutionRequest request
            ) {
        RegisterInstitutionResponse data = institutionAuthService.registerInstitution(request);

        return  ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Verification code sent to your email", data));
    }

    //verify institution with email code
    @PostMapping("/institution/verify")
    public ResponseEntity<ApiResponse<Void>> verifyInstitution(
            @Valid @RequestBody VerifyInstitutionRequest request
            ) {
        institutionAuthService.verifyInstitution(request);
        return ResponseEntity.ok(
                ApiResponse.success("Institution verified. Please Complete Your Admin setup")
        );
    }

    //Create Admin Account
    @PostMapping("/institution/setup-admin")
    public ResponseEntity<ApiResponse<Void>> setupAdmi(
            @Valid @RequestBody SetUpAdminRequest request
            ) {
        institutionAuthService.setupAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Admin account created succefully. You can now log in."));
    }

}
