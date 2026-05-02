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
import org.springframework.cglib.core.Local;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionAuthService {

    private final InstitutionRepository institutionRepository;
    private final InstitutionVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegisterInstitutionResponse registerInstitution(RegisterInstitutionRequest request) {

        if (institutionRepository.existsByEmail(request.email())) {
            throw new ConflictException("An Insituttion with this email already registerd");
        }

        Institution institution = Institution.builder()
                .publicId(PublicIdGenerator.generate("INST"))
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .status(InstitutionStatus.PENDING)
                .isVerified(false)
                .build();
        institutionRepository.save(institution);

        // Generate a 6-char code and save it with a 24-hour expiry
        String code = PublicIdGenerator.generateVerificationCode();

        InstitutionVerification verification = InstitutionVerification.builder()
                .institution(institution)
                .verificationCode(code)
                .expiesAt(LocalDateTime.now().plusHours(24))
                .verified(false)
                .build();
        verificationRepository.save(verification);

        emailService.sendInstitutionVerificationCode(institution.getEmail(), institution.getName(), code);
        log.info("Institution registerd: {} | publicId: {}", institution.getEmail(), institution.getPublicId());
        return new RegisterInstitutionResponse(
                institution.getPublicId(),
                "Verification Code sent to " + institution.getEmail() + ". Please check your inbox."
        );
    }

    @Transactional
    public void verifyInstitution(VerifyInstitutionRequest request){

        //Find By Public Id
        Institution institution = institutionRepository.findByPublicId(request.InstitutionPublicId())
                .orElseThrow(()-> new ResourceNotFoundException("Institution not found"));

        //Gurd if verified
        if(institution.isVerified()) {
            throw  new BadRequestException("Institution is verified");
        }

        // Get latest unverified code
        InstitutionVerification verification = verificationRepository.findLatestUnverifiedByInstitution(institution)
                .orElseThrow(()-> new BadRequestException("No pending verification found. Please request a new code"));

        //chech code expiry
        if(LocalDateTime.now().isAfter(verification.getExpiesAt())) {
            throw new BadRequestException("Verification code has expired. Please request a new one");

        }

        //MArk as done
        verification.setVerified(true);
        verificationRepository.save(verification);

        //Activate Insitution
        institution.setVerified(true);
        institution.setStatus(InstitutionStatus.ACTIVE);
        institutionRepository.save(institution);

        log.info("Insitution verified: {} | publicId:{}", institution.getEmail(), institution.getPublicId());

    }


    @Transactional
    public void setupAdmin(SetUpAdminRequest request) {
        //Find institution
        Institution institution = institutionRepository
                .findByPublicId(request.institutionPublicId())
                .orElseThrow(()-> new ResourceNotFoundException("Institution not found"));

        //Must be verified
        if(!institution.isVerified()) {
            throw new BadRequestException("Institution must be verified before setting up an admin account");

        }

        //Gurad if admin already exists
        if(userRepository.existsAdminForInstitution(institution.getId())) {
            throw  new ConflictException("An admin account already exists for this institution");
        }

        //Password must mathc
        if(!request.password().equals(request.confirmPassword())) {
            throw new BadRequestException("Passwords Do not Match");
        }

        //Create the dmin user
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

        log.info("Admin created for institution:{} | userPublic: {}", institution.getPublicId(), admin.getInstitution());

    }
}
