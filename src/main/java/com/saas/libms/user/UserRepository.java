package com.saas.libms.user;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    // JOIN FETCH loads the institution in the same query.
    // This prevents LazyInitializationException when CustomUserDetails
    // tries to read user.getInstitution() outside a transaction.
    @Query("SELECT u FROM User u JOIN FETCH u.institution WHERE u.email = :email")
    Optional<User> findByEmailWithInstitution(@Param("email") String email);

    // Used to check if an institution already has an admin user registered
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.institution.id = :institutionId AND u.role = 'ADMIN'")
    boolean existsAdminForInstitution(@Param("institutionId") UUID institutionId);
}