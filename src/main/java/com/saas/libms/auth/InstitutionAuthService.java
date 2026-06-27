package com.saas.libms.auth;

import com.saas.libms.auth.dto.RegisterInstitutionRequest;
import com.saas.libms.auth.dto.RegisterInstitutionResponse;
import com.saas.libms.auth.dto.SetUpAdminRequest;
import com.saas.libms.auth.dto.VerifyInstitutionRequest;
import com.saas.libms.common.EmailService;
import com.saas.libms.common.PublicIdGenerator;
import com.saas.libms.exception.BadRequestException;
import com.saas.libms.exception.ConflictException;
import com.saas.libms.exception.ResourceNotFoundException;
import com.saas.libms.institution.*;
import com.saas.libms.user.User;
import com.saas.libms.user.UserRepository;
import com.saas.libms.user.UserRole;
import com.saas.libms.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionAuthService {

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final DomainVerificationService domainVerificationService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Transactional
    public RegisterInstitutionResponse registerInstitution(RegisterInstitutionRequest request) {

        // 1. Check email not already taken
        if (institutionRepository.existsByEmail(request.email())) {
            throw new ConflictException("An institution with this email is already registered");
        }

        // 2. Extract domains
        String emailDomain = domainVerificationService.extractEmailDomain(request.email());
        String websiteDomain = domainVerificationService.extractWebsiteDomain(request.website());

        // 3. Block personal/disposable emails
        domainVerificationService.assertNotDisposableOrPersonal(emailDomain);

        // 4. Email domain must match website domain
        domainVerificationService.assertDomainsMatch(emailDomain, websiteDomain);

        // 5. Domain must exist (DNS)
        domainVerificationService.assertDomainExists(websiteDomain);

        // 6. Website must be reachable
        domainVerificationService.assertWebsiteReachable(request.website());

        // 7. Email domain must have MX records
        domainVerificationService.assertHasMxRecords(emailDomain);

        // 8. Generate email verification token
        String token = UUID.randomUUID().toString().replace("-", "");
        String dnsTxtValue = domainVerificationService.generateDnsTxtValue();

        // 9. Save institution
        Institution institution = Institution.builder()
                .publicId(PublicIdGenerator.generate("INST"))
                .name(request.name())
                .email(request.email())
                .website(request.website())
                .domain(websiteDomain)
                .phone(request.phone())
                .address(request.address())
                .status(InstitutionStatus.PENDING)
                .isVerified(false)
                .emailVerified(false)
                .domainVerified(false)
                .domainChecksPassed(true)   // all checks above passed
                .emailVerificationToken(token)
                .emailTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .dnsTxtRecord(dnsTxtValue)
                .build();

        institutionRepository.save(institution);

        // 10. Send verification email with link
        String verificationLink = baseUrl + "/verify-email?token=" + token;
        emailService.sendInstitutionVerificationLink(institution.getEmail(), institution.getName(), verificationLink);

        log.info("Institution registered: {} | publicId: {}", institution.getEmail(), institution.getPublicId());

        return new RegisterInstitutionResponse(
                institution.getPublicId(),
                "A verification link has been sent to " + institution.getEmail() + ". Please check your inbox. Link expires in 24 hours."
        );
    }

    /**
     * Called when institution admin clicks the link in their email.
     * Replaces the old verifyInstitution(code) method.
     */
    @Transactional
    public void verifyEmail(String token) {
        Institution institution = institutionRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification link"));

        if (institution.isEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        if (LocalDateTime.now().isAfter(institution.getEmailTokenExpiresAt())) {
            throw new BadRequestException("Verification link has expired. Please register again or request a new link.");
        }

        institution.setEmailVerified(true);
        institution.setEmailVerificationToken(null);   // clear token after use
        institution.setEmailTokenExpiresAt(null);

        // If you are NOT requiring DNS TXT verification, approve here
        institution.setVerified(true);
        institution.setStatus(InstitutionStatus.ACTIVE);

        institutionRepository.save(institution);
        log.info("Institution email verified: {} | publicId: {}", institution.getEmail(), institution.getPublicId());
    }

    /**
     * Optional: institution admin adds TXT record to DNS and calls this to prove domain ownership.
     * Call this AFTER verifyEmail() succeeds.
     */
    @Transactional
    public void verifyDnsTxt(String institutionPublicId) {
        Institution institution = institutionRepository.findByPublicId(institutionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));

        if (!institution.isEmailVerified()) {
            throw new BadRequestException("Email must be verified before DNS verification");
        }

        if (institution.isDomainVerified()) {
            throw new BadRequestException("Domain already verified");
        }

        boolean verified = domainVerificationService.checkDnsTxtRecord(
                institution.getDomain(),
                institution.getDnsTxtRecord()
        );

        if (!verified) {
            throw new BadRequestException(
                    "TXT record not found. Please add this to your DNS:\n" +
                            "Host: @\nValue: " + institution.getDnsTxtRecord() +
                            "\nThen try again. DNS changes can take up to 48 hours."
            );
        }

        institution.setDomainVerified(true);
        institutionRepository.save(institution);
        log.info("Institution domain verified via TXT: {} | publicId: {}", institution.getDomain(), institution.getPublicId());
    }

    @Transactional
    public void setupAdmin(SetUpAdminRequest request) {
        Institution institution = institutionRepository
                .findByPublicId(request.institutionPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution not found"));

        // Must have email verified at minimum
        if (!institution.isEmailVerified()) {
            throw new BadRequestException("Institution email must be verified before setting up an admin account");
        }

        if (userRepository.existsAdminForInstitution(institution.getId())) {
            throw new ConflictException("An admin account already exists for this institution");
        }

        if (!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User admin = User.builder()
                .publicId(PublicIdGenerator.generate("USER"))
                .institution(institution)
                .username(institution.getName())
                .email(institution.getEmail())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .isEmailVerified(true)
                .build();

        userRepository.save(admin);
        log.info("Admin created for institution: {}", institution.getPublicId());
    }
}
