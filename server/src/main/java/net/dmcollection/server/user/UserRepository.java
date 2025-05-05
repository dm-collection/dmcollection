package net.dmcollection.server.user;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends ListCrudRepository<User, UUID> {
  @Query("SELECT * FROM USERS WHERE username = :username")
  Optional<User> findByUsername(@Param("username") String username);

  boolean existsByUsernameIgnoringCase(String username);
}
