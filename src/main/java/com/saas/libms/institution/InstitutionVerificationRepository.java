package com.saas.libms.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InstitutionVerificationRepository extends JpaRepository<InstitutionVerification, UUID> {

    @Query("""
            SELECT iv FROM InstitutionVerification iv
            WHERE iv.institution = :institution
              AND iv.verified = false
            ORDER BY iv.createdAt DESC
            LIMIT 1
            """)
    Optional<InstitutionVerification> findLatestUnverifiedByInstitution(
            @Param("institution") Institution institution
    );

    // Used to check if institution already has a verified record before allowing admin setup
    boolean existsByInstitutionAndVerifiedTrue(Institution institution);
}
