package com.saas.libms.auth.session;

import com.saas.libms.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionRepository extends JpaRepository<RefreshSession, UUID> {

    @Modifying
    @Query("DELETE FROM RefreshSession rs WHERE rs.user = :user")
    void deleteByUser(@Param("user")User user);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM RefreshSession rs WHERE rs.expiresAt < :now")
    void deleteAllExpiredBefore(@Param("now")LocalDateTime now);

    @Query("SELECT COUNT(rs) > 0 FROM RefreshSession rs WHERE rs.user = :user AND rs.expiresAt > :now")
    boolean existsActiveSessionForUser(@Param("user") User user, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshSession r WHERE r.user.id = :userId")
    void deleteByUserId(UUID userId);




}
