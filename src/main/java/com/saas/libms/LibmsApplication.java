package com.saas.libms;

import com.saas.libms.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
public class LibmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibmsApplication.class, args);
        System.out.println("LIBRARY MANAGEMENT SYSTEM API IS RUNNING :)");
	}

}
