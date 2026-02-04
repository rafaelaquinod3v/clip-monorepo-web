package sv.com.clip.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import sv.com.clip.config.JwtFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

  @Bean
  fun passwordEncoder() = BCryptPasswordEncoder()

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http.csrf { it.disable() } // Deshabilitado solo para pruebas rápidas con Postman
      .cors {  } // importante para angular / electron
      .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
      .headers { it.frameOptions { frame -> frame.sameOrigin() } } // Permitir Frames pgadmin
      .authorizeHttpRequests { auth ->
        auth.requestMatchers("/api/users/register", "/api/users/login").permitAll() // Público para crear el primer usuario
          .requestMatchers("/api/users/list").hasRole("ADMIN") // Solo admins ven la lista
          .anyRequest().authenticated()
      }
      .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
      //.httpBasic { } // Autenticación básica para pruebas
    return http.build()
  }
}
