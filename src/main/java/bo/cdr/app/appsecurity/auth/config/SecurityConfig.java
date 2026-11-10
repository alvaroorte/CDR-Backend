package bo.cdr.app.appsecurity.auth.config;

import bo.cdr.app.appsecurity.auth.jwt.AuthorizationFilter;
import bo.cdr.app.appsecurity.auth.jwt.CustomAccessDeniedHandler;
import bo.cdr.app.appsecurity.auth.jwt.CustomAuthenticationEntryPoint;
import bo.cdr.app.appsecurity.auth.jwt.JwtAuthenticationFilter;
import bo.cdr.app.util.RequestMatcherUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cadena de seguridad de la referencia sin LicenseFilter, ExternalApiKeyFilter, UserActivityFilter
 * ni proveedor LDAP: solo JWT + autorización por permisos en BD.
 */
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final AuthorizationFilter authorizationFilter;
    private final CustomAuthenticationEntryPoint authEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint) // 401
                        .accessDeniedHandler(accessDeniedHandler) // 403
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(RequestMatcherUtil.TOKEN_NOT_REQUIRED).permitAll()
                        .requestMatchers(RequestMatcherUtil.PERMISSION_NOT_REQUIRED).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authorizationFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider daoProvider) {
        return new ProviderManager(daoProvider);
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
