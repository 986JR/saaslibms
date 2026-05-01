package com.saas.libms.config;



import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {

    private String secret;
    private long accessTokenExpiryMs;   // 300_000   = 5 min
    private long refreshTokenExpiryMs;  // 36_000_000 = 10 hrs
}
