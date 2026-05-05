package com.saas.libms.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    Page<Book> findAllByInstitutionId(UUID institutionId, Pageable pageable);

    Optional<Book> findByPublicIdAndInstitutionId(String publicId, UUID institutionId);

    boolean existsByPublicId(String publicId);

    boolean existsByIsbnAndInstitutionId(String isbn, UUID institutionId);

}
