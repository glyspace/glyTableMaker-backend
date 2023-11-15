package org.glygen.tablemaker.config;

import org.glygen.tablemaker.security.AuthTokenFilter;
import org.glygen.tablemaker.security.HandlerAccessDeniedHandler;
import org.glygen.tablemaker.security.HandlerAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@EnableWebSecurity
@Configuration
public class SecurityConfig {
    
    private final UserDetailsService userDetailsService;
    private final AuthTokenFilter authTokenFilter;
    
    public SecurityConfig(UserDetailsService userDetailsService, AuthTokenFilter authTokenFilter) {
        this.userDetailsService = userDetailsService;
        this.authTokenFilter = authTokenFilter;
    }
    
    @Bean // (1)
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean // (2)
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean // (3)
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @Bean // (4)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        RequestMatcher PUBLIC_URLS = new OrRequestMatcher( 
                new AntPathRequestMatcher("/error"),
                new AntPathRequestMatcher("/api/account/authenticate**"),
                //new AntPathRequestMatcher(basePath + "login**"),
                //new AntPathRequestMatcher("**/login**"),
                new AntPathRequestMatcher("/api/account/register"),
                new AntPathRequestMatcher("/api/account/availableUsername"),
                new AntPathRequestMatcher("/api/account/recover"),
                new AntPathRequestMatcher("/api/account/**/password", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/account/registrationConfirm"));
        // @formatter:off
        http
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider())
                // Set unauthorized requests exception handler
                .exceptionHandling((exception) -> 
                    exception.authenticationEntryPoint(new HandlerAuthenticationEntryPoint())
                    .accessDeniedHandler(new HandlerAccessDeniedHandler()))
                
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Set permissions on endpoints
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/configuration/ui",
                        "/swagger-resources/**",
                        "/configuration/security",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html").permitAll()
                .requestMatchers("/api/**").authenticated());
               // .oauth2ResourceServer(configure -> configure.jwt(Customizer.withDefaults()));
        // @formatter:on
        return http.build();
    }

}
