package com.saas.libms.security;

import com.saas.libms.institution.InstitutionStatus;
import com.saas.libms.user.User;
import com.saas.libms.user.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security doesn't know about our User entity.
 * This class wraps it and tells Spring Security what it needs:
 *  - username (we use email)
 *  - password (hashed)
 *  - authorities/roles
 *  - account status flags
 * We also carry extra fields (userId, institutionId) so we
 * can access them from any controller via the SecurityContext
 * without an extra DB call.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final UUID institutionId;
    private final String email;
    private final String password;
    private final String role;
    private final boolean enabled;
    private final UserStatus status;
    private final InstitutionStatus institutionStatus;
    private final User user;

    public CustomUserDetails(User user) {
        this.userId        = user.getId();
        this.institutionId = user.getInstitution().getId();
        this.email         = user.getEmail();
        this.password      = user.getPassword();
        this.role          = user.getRole().name();
        // User is enabled only if their account is ACTIVE
        this.enabled       = switch (user.getStatus()) {
            case ACTIVE   -> true;
            case DISABLED -> false;
        };
        this.status=user.getStatus();
        this.institutionStatus=user.getInstitution().getStatus();
        this.user = user;
    }

    // Spring Security uses this to check the password during login
    @Override
    public String getPassword() {
        return password;
    }

    // We use email as the login identifier
    @Override
    public String getUsername() {
        return email;
    }

    // Roles must be prefixed with "ROLE_" for Spring Security
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && institutionStatus == InstitutionStatus.ACTIVE;
    }

    // We manage expiry ourselves via the session table — these are always true
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return enabled; // DISABLED users are effectively locked
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


}
