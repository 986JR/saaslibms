package com.saas.libms.institution;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="institutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_id", unique = true, nullable = false, length = 20)
    private String publicId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    private String phone;
    private  String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InstitutionStatus status = InstitutionStatus.PENDING;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private boolean isVerified = false;

    @Column(name = "website")
    private String website;

    @Column(name = "domain")
    private String domain;   // extracted from website — e.g. "udom.ac.tz"

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "domain_verified", nullable = false)
    @Builder.Default
    private boolean domainVerified = false;

    // Token for email verification link (replaces the 6-char code)
    @Column(name = "email_verification_token", length = 64)
    private String emailVerificationToken;

    @Column(name = "email_token_expires_at")
    private LocalDateTime emailTokenExpiresAt;

    // DNS TXT record value they must add to prove domain ownership
    @Column(name = "dns_txt_record", length = 100)
    private String dnsTxtRecord;

    @Column(name = "domain_checks_passed", nullable = false)
    @Builder.Default
    private boolean domainChecksPassed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime   createdAt;

    @UpdateTimestamp
    @Column(name="updated_at")
    private LocalDateTime updatedAt;



}
