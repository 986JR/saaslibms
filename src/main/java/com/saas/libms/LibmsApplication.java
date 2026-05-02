package com.saas.libms;

import com.saas.libms.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
@EnableAsync
@EnableScheduling
public class LibmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibmsApplication.class, args);
        System.out.println("LIBRARY MANAGEMENT SYSTEM API IS RUNNING :)");
	}

}
