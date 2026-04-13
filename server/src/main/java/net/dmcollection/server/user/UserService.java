package net.dmcollection.server.user;

import static net.dmcollection.server.jooq.generated.tables.AppUser.APP_USER;

import org.jooq.DSLContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService implements UserDetailsService {
  private final DSLContext dsl;
  private final PasswordEncoder passwordEncoder;

  public UserService(DSLContext dsl) {
    this.dsl = dsl;
    this.passwordEncoder = passwordEncoder();
  }

  @Override
  public User loadUserByUsername(String username) throws UsernameNotFoundException {
    return dsl.selectFrom(APP_USER)
        .where(APP_USER.USERNAME.eq(username))
        .fetchOptional(
            r ->
                new User(
                    r.get(APP_USER.ID),
                    r.get(APP_USER.USERNAME),
                    r.get(APP_USER.PASSWORD_HASH),
                    r.get(APP_USER.DISPLAY_NAME),
                    r.get(APP_USER.AVATAR_PATH),
                    r.get(APP_USER.IS_ADMIN)))
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  public User createUser(String username, String password) {
    return dsl.insertInto(APP_USER)
        .set(APP_USER.USERNAME, username)
        .set(APP_USER.PASSWORD_HASH, passwordEncoder.encode(password))
        .set(APP_USER.DISPLAY_NAME, username)
        .returning()
        .fetchOne(
            r ->
                new User(
                    r.get(APP_USER.ID),
                    r.get(APP_USER.USERNAME),
                    r.get(APP_USER.PASSWORD_HASH),
                    r.get(APP_USER.DISPLAY_NAME),
                    r.get(APP_USER.AVATAR_PATH),
                    r.get(APP_USER.IS_ADMIN)));
  }

  public boolean existsByUsername(String username) {
    return dsl.fetchExists(
        dsl.selectOne().from(APP_USER).where(APP_USER.USERNAME.equalIgnoreCase(username)));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
