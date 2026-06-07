package com.saas.libms.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    @Query(
            value = "SELECT m FROM Member m JOIN FETCH m.institution i WHERE i.id = :institutionId",
            countQuery = "SELECT COUNT(m) FROM Member m WHERE m.institution.id = :institutionId"
    )
    Page<Member> findAllByInstitutionId(
            @Param("institutionId") UUID institutionId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM Member m
            JOIN FETCH m.institution i
            WHERE m.publicId = :publicId
            AND i.id = :institutionId
            """)
    Optional<Member> findByPublicIdAndInstitutionId(
            @Param("publicId") String publicId,
            @Param("institutionId") UUID institutionId
    );

    boolean existsByEmailAndInstitutionId(
            String email,
            UUID institutionId
    );

    @Query("""
            SELECT COUNT(m) > 0
            FROM Member m
            WHERE m.email = :email
            AND m.institution.id = :institutionId
            AND m.publicId <> :excludePublicId
            """)
    boolean existsByEmailAndInstitutionIdExcluding(
            @Param("email") String email,
            @Param("institutionId") UUID institutionId,
            @Param("excludePublicId") String excludePublicId
    );

    // Member count per institution — used in institution activity ranking
    @Query("SELECT m.institution.id, COUNT(m) FROM Member m GROUP BY m.institution.id")
    List<Object[]> countMembersPerInstitution();
}