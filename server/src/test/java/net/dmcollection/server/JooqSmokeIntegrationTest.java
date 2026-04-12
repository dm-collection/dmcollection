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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    classes = {
      DataSourceAutoConfiguration.class,
      FlywayAutoConfiguration.class,
      JooqAutoConfiguration.class
    })
class JooqSmokeIntegrationTest {

  @ServiceConnection static final PostgreSQLContainer<?> PG = PostgresTestBase.PG;

  @Autowired DSLContext dsl;

  @Test
  void schemaAppliedAndJooqWorks() {
    Result<Record1<Object>> result =
        dsl.select(field("table_name"))
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
