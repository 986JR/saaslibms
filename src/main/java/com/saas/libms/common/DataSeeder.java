package com.saas.libms.common;

import com.saas.libms.institution.Institution;
import com.saas.libms.institution.InstitutionRepository;
import com.saas.libms.institution.InstitutionStatus;
import com.saas.libms.user.User;
import com.saas.libms.user.UserRepository;
import com.saas.libms.user.UserRole;
import com.saas.libms.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("[DataSeeder] Running database seed checks...");

        // Drop the enum check constraint to allow the new SYSTEM role
        try {
            log.info("[DataSeeder] Dropping users_role_check constraint if exists...");
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            log.info("[DataSeeder] Constraint users_role_check dropped successfully.");
        } catch (Exception e) {
            log.warn("[DataSeeder] Failed to drop constraint users_role_check: {}", e.getMessage());
        }

        // 1. Seed System Institution
        Institution systemInstitution = institutionRepository.findByPublicId("INST-SYSTEM")
                .orElseGet(() -> {
                    log.info("[DataSeeder] System institution not found. Seeding default System institution...");
                    Institution inst = Institution.builder()
                            .publicId("INST-SYSTEM")
                            .name("System Admin")
                            .email("joshuaridgers986@gmail.com")
                            .phone("0000000000")
                            .address("System Cloud")
                            .status(InstitutionStatus.ACTIVE)
                            .isVerified(true)
                            .build();
                    return institutionRepository.save(inst);
                });

        // 2. Seed Default System User (DevOne)
        userRepository.findByEmail("joshuaridgers986@gmail.com")
                .orElseGet(() -> {
                    log.info("[DataSeeder] Default system user DevOne not found. Seeding...");
                    User user = User.builder()
                            .publicId(PublicIdGenerator.generate("USER"))
                            .username("DevOne")
                            .email("joshuaridgers986@gmail.com")
                            .password(passwordEncoder.encode("Password123"))
                            .role(UserRole.SYSTEM)
                            .status(UserStatus.ACTIVE)
                            .isEmailVerified(true)
                            .institution(systemInstitution)
                            .build();
                    return userRepository.save(user);
                });

        log.info("[DataSeeder] Database seeding check completed.");
    }
}
