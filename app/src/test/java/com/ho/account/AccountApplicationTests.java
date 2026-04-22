package com.ho.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
		classes = AccountApplication.class,
		properties = {
				"spring.cloud.config.enabled=false",
				"spring.cloud.config.import-check.enabled=false"
		}
)
class AccountApplicationTests {

	@Test
	void contextLoads() {
	}

}
