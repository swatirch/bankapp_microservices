package com.banking.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
		"com.banking.notification",
		"com.banking.common"
})
public class BankingNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingNotificationServiceApplication.class, args);
	}

}
