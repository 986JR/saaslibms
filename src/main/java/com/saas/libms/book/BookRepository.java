package com.saas.libms.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Page<Book> findAllByInstitutionId(UUID institutionId, Pageable pageable);

    Optional<Book> findByPublicIdAndInstitutionId(String publicId, UUID institutionId);

    boolean existsByPublicId(String publicId);

    boolean existsByIsbnAndInstitutionId(String isbn, UUID institutionId);

    // Total books across all institutions
    @Query("SELECT COUNT(b) FROM Book b")
    long countAllBooks();

    // Book count per institution — used in institution activity ranking
    @Query("SELECT b.institution.id, COUNT(b) FROM Book b GROUP BY b.institution.id")
    List<Object[]> countBooksPerInstitution();

}
