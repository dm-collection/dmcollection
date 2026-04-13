package net.dmcollection.server;

import static net.dmcollection.server.jooq.generated.Tables.APP_USER;

import java.util.UUID;
import net.dmcollection.server.card.internal.query.CardTypeResolver;
import net.dmcollection.server.user.User;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

  @ServiceConnection
  public static final PostgreSQLContainer PG =
      new PostgreSQLContainer("postgres:18-alpine")
          .withDatabaseName("dmcollection_test")
          .withUsername("test")
          .withPassword("test");

  static {
    PG.start();
  }

  @Autowired protected DSLContext dsl;
  @Autowired protected CardTypeResolver cardTypeResolver;

  protected User createUser(String namePrefix) {
    return dsl.insertInto(APP_USER)
        .set(APP_USER.USERNAME, namePrefix + UUID.randomUUID())
        .set(APP_USER.PASSWORD_HASH, "$2a$10$test")
        .set(APP_USER.DISPLAY_NAME, "Test User")
        .returning()
        .fetchOneInto(User.class);
  }
}
