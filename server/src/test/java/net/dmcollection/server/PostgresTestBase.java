package net.dmcollection.server;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresTestBase {

	static final PostgreSQLContainer<?> PG =
			new PostgreSQLContainer<>("postgres:18-alpine")
					.withDatabaseName("dmcollection_test")
					.withUsername("test")
					.withPassword("test");

	static {
		PG.start();
	}

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", PG::getJdbcUrl);
		registry.add("spring.datasource.username", PG::getUsername);
		registry.add("spring.datasource.password", PG::getPassword);
		registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration");
		registry.add("spring.liquibase.enabled", () -> "false");
		registry.add("spring.h2.console.enabled", () -> "false");
	}

	@Autowired
	protected DSLContext dsl;
}
