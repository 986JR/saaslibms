package com.saas.libms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LibmsApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibmsApplication.class, args);
        System.out.println("LIBRARY MANAGEMENT SYSTEM API IS RUNNING :)");
	}

}
