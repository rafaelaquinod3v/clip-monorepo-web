package sv.com.clip.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

  @Bean
  fun passwordEncoder() = BCryptPasswordEncoder()

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http.csrf { it.disable() } // Deshabilitado solo para pruebas rápidas con Postman
      .headers { it.frameOptions { frame -> frame.sameOrigin() } } // Permitir Frames pgadmin
      .authorizeHttpRequests { auth ->
        auth.requestMatchers("/api/users/register").permitAll() // Público para crear el primer usuario
          .requestMatchers("/api/users/list").hasRole("ADMIN") // Solo admins ven la lista
          .anyRequest().authenticated()
      }
      .httpBasic { } // Autenticación básica para pruebas
    return http.build()
  }
}
