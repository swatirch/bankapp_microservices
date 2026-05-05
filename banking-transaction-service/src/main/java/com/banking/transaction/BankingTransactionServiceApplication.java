package com.banking.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@ComponentScan(basePackages = {
		"com.banking.transaction",
		"com.banking.common"
})
public class BankingTransactionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BankingTransactionServiceApplication.class, args);
	}

}
