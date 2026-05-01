package com.saas.libms.auth.session;

import com.saas.libms.institution.Institution;
import com.saas.libms.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class RefreshSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id",nullable = false)
    private Institution institutionId;


    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired(){
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
