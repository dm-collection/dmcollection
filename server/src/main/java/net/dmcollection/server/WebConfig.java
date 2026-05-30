package net.dmcollection.server;

import static java.util.Objects.nonNull;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.AbstractResourceResolver;
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
    if (!appProperties.imageService().enabled()) {
      String imagePath = "file:" + appProperties.imageStoragePath();
      String location = imagePath.endsWith("/") ? imagePath : imagePath + "/";
      registry
          .addResourceHandler("/image/**")
          .addResourceLocations(location)
          .resourceChain(false)
          .addResolver(new ImageFilenameResolver())
          .addResolver(new PathResourceResolver());
    }
  }

  private static class ImageFilenameResolver extends AbstractResourceResolver {

    private static final Pattern IMAGE_FILENAME =
        Pattern.compile("^(?:.+/)?([^/]+\\.(?:jpg|webp))$");

    @Override
    @Nullable
    protected Resource resolveResourceInternal(
        @Nullable HttpServletRequest request,
        @NonNull String requestPath,
        @NonNull List<? extends Resource> locations,
        @NonNull ResourceResolverChain chain) {
      Matcher m = IMAGE_FILENAME.matcher(requestPath);
      return chain.resolveResource(request, m.matches() ? m.group(1) : requestPath, locations);
    }

    @Override
    @Nullable
    protected String resolveUrlPathInternal(
        @NonNull String resourceUrlPath,
        @NonNull List<? extends Resource> locations,
        @NonNull ResourceResolverChain chain) {
      Matcher m = IMAGE_FILENAME.matcher(resourceUrlPath);
      return chain.resolveUrlPath(m.matches() ? m.group(1) : resourceUrlPath, locations);
    }
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
