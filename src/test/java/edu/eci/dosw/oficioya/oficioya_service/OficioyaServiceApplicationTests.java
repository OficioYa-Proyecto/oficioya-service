package edu.eci.dosw.oficioya.oficioya_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=false")
class OficioyaServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}