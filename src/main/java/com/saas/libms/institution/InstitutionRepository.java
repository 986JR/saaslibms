package com.saas.libms.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    boolean existsByEmail(String email);

    Optional<Institution> findByPublicId(String publicId);

    Optional<Institution> findByEmail(String email);

    @Query("SELECT COUNT(i) > 0 FROM Institution i WHERE i.email = :email AND i.id != :excludeId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") UUID excludeId);


}
