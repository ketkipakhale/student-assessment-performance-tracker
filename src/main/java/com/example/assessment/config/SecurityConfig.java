package com.example.assessment.config;

import com.example.assessment.entity.Student;
import com.example.assessment.repository.StudentRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(StudentRepository students) {
        return username -> students.findByEmail(username.toLowerCase())
                .map(this::toUser)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    private UserDetails toUser(Student student) {
        return User.withUsername(student.getEmail())
                .password(student.getPasswordHash())
                .roles(student.getRole())
                .build();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(UserDetailsService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            SecurityContextRepository securityContextRepository,
                                            @Value("${app.security.enabled:true}") boolean securityEnabled) throws Exception {
        if (!securityEnabled) {
            http.csrf(csrfConfig -> csrfConfig.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
        CookieCsrfTokenRepository csrf = CookieCsrfTokenRepository.withHttpOnlyFalse();
        http
                .csrf(csrfConfig -> csrfConfig.csrfTokenRepository(csrf))
                .securityContext(context -> context.securityContextRepository(securityContextRepository))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/assets/**",
                                "/api/auth/csrf", "/api/auth/register", "/api/auth/login",
                                "/api/auth/forgot-password", "/api/auth/reset-password",
                                "/api/auth/development-reset-link",
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/assessments/**", "/api/submissions/**",
                                "/api/students/*", "/api/students/*/results",
                                "/api/students/*/progress").authenticated()
                        .requestMatchers("/api/students").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(org.springframework.http.HttpStatus.NO_CONTENT))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> writeError(response, 401,
                                "Authentication is required."))
                        .accessDeniedHandler((request, response, exception) -> writeError(response, 403,
                                "You do not have permission to perform this action.")));
        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"error\":\""
                + (status == 401 ? "Unauthorized" : "Forbidden") + "\",\"message\":\"" + message + "\"}");
    }
}
