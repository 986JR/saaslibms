package com.saas.libms.category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findByName(String categoryName);

    Optional<Category> findByPublicId(String publicId);

    boolean existsByNameAndInstitutionId(String name, UUID institutionId);

    Page<Category> findAllByInstitutionId(UUID institutionId, Pageable pageable);

    Optional<Category> findByNameAndInstitutionId(String name, UUID institutionId);

    @Modifying
    @Query("UPDATE Book b SET b.category = null WHERE b.category.id = :categoryId")
    void detachBookBooksFromCategory(@Param("categoryId") UUID categoryId);

    Optional<Category> findByPublicIdAndInstitutionId(String publicId, UUID institutionId);


}
