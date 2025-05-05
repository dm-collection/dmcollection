package net.dmcollection.server;

import static java.util.Objects.nonNull;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final AppProperties appProperties;

  public WebConfig(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    // Forward "/" to "/index.html"
    registry.addViewController("/").setViewName("forward:/index.html");
  }

  @Override
  public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
    this.serveDirectory(registry, "/", "classpath:/static/");
    String imagePath = "file:" + appProperties.imageStoragePath();
    serveDirectory(registry, "/image/", imagePath);
  }

  private void serveDirectory(ResourceHandlerRegistry registry, String endpoint, String location) {
    String[] endpointPatterns =
        endpoint.endsWith("/")
            ? new String[] {endpoint.substring(0, endpoint.length() - 1), endpoint, endpoint + "**"}
            : new String[] {endpoint, endpoint + "/", endpoint + "/**"};
    registry
        .addResourceHandler(endpointPatterns)
        .addResourceLocations(location.endsWith("/") ? location : location + "/")
        .resourceChain(false)
        .addResolver(
            new PathResourceResolver() {

              @Override
              public Resource resolveResource(
                  HttpServletRequest request,
                  @NonNull String requestPath,
                  @NonNull List<? extends Resource> locations,
                  @NonNull ResourceResolverChain chain) {
                Resource resource = super.resolveResource(request, requestPath, locations, chain);
                if (nonNull(resource)) {
                  return resource;
                }
                return super.resolveResource(request, "/index.html", locations, chain);
              }
            });
  }
}
