package sv.com.clip.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig : WebMvcConfigurer {
  override fun addCorsMappings(registry: CorsRegistry) {
    registry.
    addMapping("/**").
    allowedOrigins("http://localhost:4200").
    allowedMethods("*").
    allowedHeaders("*").
    allowCredentials(true)
  }

  override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
    registry.addResourceHandler("/favicon.ico")
      .addResourceLocations("classpath:/static/")
  }
}
