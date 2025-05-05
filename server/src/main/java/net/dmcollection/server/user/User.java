package net.dmcollection.server.user;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Table("USERS")
public class User implements UserDetails, CredentialsContainer {
  private final @Id UUID id;
  private final String username;
  private String password;
  private boolean enabled;
  private final @CreatedDate LocalDateTime createdAt;

  @PersistenceCreator
  public User(UUID id, String username, String password, boolean enabled, LocalDateTime createdAt) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.enabled = enabled;
    this.createdAt = createdAt;
  }

  public User(UUID id, String username) {
    this.id = id;
    this.username = username;
    this.createdAt = LocalDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.emptyList();
  }

  @Override
  public void eraseCredentials() {
    this.password = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    // ID is the primary key, sufficient for equality
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "User{"
        + "id="
        + id
        + ", username='"
        + username
        + "', enabled="
        + enabled
        + ", createdAt="
        + createdAt
        + '}';
  }
}
