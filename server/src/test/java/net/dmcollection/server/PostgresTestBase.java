package net.dmcollection.server;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class PostgresTestBase {

  @ServiceConnection
  public static final PostgreSQLContainer<?> PG =
      new PostgreSQLContainer<>("postgres:18-alpine")
          .withDatabaseName("dmcollection_test")
          .withUsername("test")
          .withPassword("test");

  static {
    PG.start();
  }

  @Autowired protected DSLContext dsl;
}
