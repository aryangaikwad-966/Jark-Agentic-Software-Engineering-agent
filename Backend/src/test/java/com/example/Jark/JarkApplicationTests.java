package com.example.Jark;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

@Disabled("Disabled until Docker / MySQL container is available")
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JarkApplicationTests {

	@Test
	@Disabled("Disabled until VXC-011 (Database and Migrations) is completed")
	void contextLoads() {
	}

}
