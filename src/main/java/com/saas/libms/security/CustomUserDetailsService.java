package com.saas.libms.security;

import com.saas.libms.exception.UnauthorizedException;
import com.saas.libms.user.User;
import com.saas.libms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security calls this automatically during the login process.
 * It says: "give me the user with this email so I can check the password."
 *
 * We load the user + their institution in one query (JOIN FETCH)
 * so we don't trigger a lazy-load exception when building CustomUserDetails.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithInstitution(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        return new CustomUserDetails(user);
    }
}
