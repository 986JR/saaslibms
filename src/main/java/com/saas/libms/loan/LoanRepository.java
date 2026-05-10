package com.saas.libms.loan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    @Query("""
            SELECT l FROM Loan l
                        JOIN FETCH l.institution
                        JOIN FETCH l.book
                        JOIN FETCH l.member
                        WHERE l.publicId = :publicId
                          AND l.institution.id = :institutionId
                          AND l.archived = false
            """)
    Optional<Loan> findByPublicIdInstitutionId(
            @Param("publicId") String publicId,
            @Param("institutionId") UUID institutionId
    );

    @Query(
            value = """
                     SELECT l FROM Loan l
                                JOIN FETCH l.institution
                                JOIN FETCH l.book
                                JOIN FETCH l.member
                                WHERE l.institution.id = :institutionId
                                  AND l.archived = false
                                  AND (:status IS NULL OR l.status = :status)
                                  AND (:memberPublicId IS NULL OR l.member.publicId = :memberPublicId)
                                  AND (:bookPublicId IS NULL OR l.book.publicId = :bookPublicId)
                                ORDER BY l.borrowDate DESC
                    """ ,
            countQuery = """
                     SELECT COUNT(l) FROM Loan l
                                WHERE l.institution.id = :institutionId
                                  AND l.archived = false
                                  AND (:status IS NULL OR l.status = :status)
                                  AND (:memberPublicId IS NULL OR l.member.publicId = :memberPublicId)
                                  AND (:bookPublicId IS NULL OR l.book.publicId = :bookPublicId)
                    """
    )
    Page<Loan> findAllByInstitutionId(@Param("institutionId") UUID institutionId,
                                      @Param("status") LoanStatus status,
                                      @Param("memberPublicId") String memberPublicId,
                                      @Param("bookPublicId") String bookPublicId,
                                      Pageable pageable);


    boolean existsByBookIdAndStatusAndArchivedFalse(UUID bookId, LoanStatus status);

    long countByMemberIdAndStatusAndArchivedFalse(UUID memberId, LoanStatus status);

    @Modifying
    @Query("""
            UPDATE Loan l SET l.status = com.saas.libms.loan.LoanStatus.LATE
                        WHERE l.status = com.saas.libms.loan.LoanStatus.BORROWED
                          AND l.dueDate < :today
                          AND l.archived = false
            """)
    int markOverdueLoanAsLate(@Param("today") LocalDate today);

    // Used to check if a member has any unreturned loans (for validation purposes)
    @Query("""
            SELECT COUNT(l) > 0 FROM Loan l
            WHERE l.member.id = :memberId
              AND l.status IN (com.saas.libms.loan.LoanStatus.BORROWED, com.saas.libms.loan.LoanStatus.LATE)
              AND l.archived = false
            """)
    boolean hasActiveLoans(@Param("memberId") UUID memberId);

    // Active loans per member in this institution (for the member summary on return)
    @Query("""
            SELECT l FROM Loan l
            JOIN FETCH l.book
            WHERE l.member.id = :memberId
              AND l.institution.id = :institutionId
              AND l.status IN (com.saas.libms.loan.LoanStatus.BORROWED, com.saas.libms.loan.LoanStatus.LATE)
              AND l.archived = false
            """)
    List<Loan> findActiveByMemberAndInstitution(@Param("memberId") UUID memberId,
                                                @Param("institutionId") UUID institutionId);
}
