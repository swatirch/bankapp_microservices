package com.banking.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // ← add this
class BankingAuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}
}