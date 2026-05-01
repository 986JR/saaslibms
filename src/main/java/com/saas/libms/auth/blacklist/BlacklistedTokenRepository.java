package com.saas.libms.auth.blacklist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;


public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, UUID> {

    boolean existsByTokenHash(String tokenHash);


    @Modifying
    @Query("DELETE FROM BlacklistedToken bt WHERE bt.expiresAt < :now")
    void deleteAllExpiredBefore(@Param("now")LocalDateTime now);
}
