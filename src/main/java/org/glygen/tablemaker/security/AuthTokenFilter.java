package org.glygen.tablemaker.security;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
	final static Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);
    
    public AuthTokenFilter(TokenProvider tokenProvider, UserDetailsService userService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    private final TokenProvider tokenProvider;
    private final UserDetailsService userService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        Optional<String> jwt = resolveHeaderToken(request);
        try {
            if (jwt.isPresent() && !tokenProvider.isTokenExpired(jwt.get())) {

                String username = tokenProvider.extractUserName(jwt.get());
                if (StringUtils.isNotEmpty(username) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (UsernameNotFoundException | JwtException | IllegalArgumentException | NoSuchAlgorithmException
                | InvalidKeySpecException e) {
            logger.error("Authorization Error", e);
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> resolveHeaderToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        return StringUtils.isNotEmpty(bearerToken) && bearerToken.startsWith("Bearer ") ? Optional.of(bearerToken.substring(7)) : Optional.empty();
    }
}