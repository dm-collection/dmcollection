package net.dmcollection.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jooq.impl.DSL.field;

import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(classes = {
		DataSourceAutoConfiguration.class,
		FlywayAutoConfiguration.class,
		JooqAutoConfiguration.class
})
class JooqSmokeIntegrationTest {

	static final PostgreSQLContainer<?> PG = PostgresTestBase.PG;

	@DynamicPropertySource
	static void overrideProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", PG::getJdbcUrl);
		registry.add("spring.datasource.username", PG::getUsername);
		registry.add("spring.datasource.password", PG::getPassword);
		registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.flyway.locations", () -> "classpath:db/migration");
	}

	@Autowired
	DSLContext dsl;

	@Test
	void schemaAppliedAndJooqWorks() {
		Result<Record1<Object>> result = dsl.select(field("table_name"))
				.from("information_schema.tables")
				.where(field("table_schema").eq("public"))
				.and(field("table_name").eq("card"))
				.fetch();
		assertThat(result).hasSize(1);
	}

	@Test
	void uuidv7FunctionExists() {
		Object uuid = dsl.select(field("uuidv7()")).fetchOne().value1();
		assertThat(uuid).isNotNull();
	}
}
