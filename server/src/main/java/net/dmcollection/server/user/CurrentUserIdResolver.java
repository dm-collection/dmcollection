package net.dmcollection.server.user;

import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

// Argument resolver that handles the @CurrentUserId annotation
public class CurrentUserIdResolver implements HandlerMethodArgumentResolver {

  private final UserRepository userRepository;

  public CurrentUserIdResolver(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.hasParameterAnnotation(CurrentUserId.class)
        && (parameter.getParameterType().equals(UUID.class));
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    String username =
        ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
            .getUsername();

    return userRepository
        .findByUsername(username)
        .map(User::getId)
        .orElseThrow(() -> new IllegalStateException("Current user not found"));
  }
}
