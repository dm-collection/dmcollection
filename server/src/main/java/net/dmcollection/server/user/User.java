package net.dmcollection.server.user;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class User implements UserDetails, CredentialsContainer {
  private final UUID id;
  private final String username;
  private String password;
  private final String displayName;
  private final String avatarPath;
  private final boolean admin;

  public User(
      UUID id,
      String username,
      String password,
      String displayName,
      String avatarPath,
      boolean admin) {
    this.id = id;
    this.username = username;
    this.password = password;
    this.displayName = displayName;
    this.avatarPath = avatarPath;
    this.admin = admin;
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

  public String getDisplayName() {
    return displayName;
  }

  public String getAvatarPath() {
    return avatarPath;
  }

  public boolean isAdmin() {
    return admin;
  }

  @Override
  public boolean isEnabled() {
    return true;
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
    return Objects.equals(id, user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "User{" + "id=" + id + ", username='" + username + "', admin=" + admin + '}';
  }
}
