package net.dmcollection.server.user;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Configuration to register the argument resolver
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final UserRepository userRepository;

  public WebMvcConfig(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new CurrentUserIdResolver(userRepository));
  }
}
