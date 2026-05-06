package com.saas.libms.book;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthorRepository extends JpaRepository<Author, UUID> {

    Page<Author> findAllByInstitutionId(UUID institutionId, Pageable pageable);

    Optional<Author> findByPublicIdAndInstitutionId(String publicId, UUID institutionId);

    boolean existsByNameAndInstitutionId(String name, UUID institutionIs);
}
