package com.banking.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.banking.auth", // ← scan this service
		"com.banking.common" // ← scan shared library too
})
public class BankingAuthServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingAuthServiceApplication.class, args);
	}

}
