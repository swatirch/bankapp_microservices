package com.banking.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication
@EnableCaching
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
@ComponentScan(basePackages = {
        "com.banking.account",
        "com.banking.common"
})
public class BankingAccountServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankingAccountServiceApplication.class, args);
    }
}