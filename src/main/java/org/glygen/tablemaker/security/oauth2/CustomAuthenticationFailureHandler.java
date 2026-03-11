package org.glygen.tablemaker.security.oauth2;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
	
	@Value("${glygen.frontend.basePath}")
    String frontEndbasePath;
    
    @Value("${glygen.frontend.host}")
    String frontEndHost;
    
    @Value("${glygen.frontend.scheme}")
    String frontEndScheme;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
    	
    	String errorMessage = "Authentication failed";
    	
    	if (exception instanceof OAuth2AuthenticationException) {
    		errorMessage = ((OAuth2AuthenticationException) exception).getError().getErrorCode();
    	} else if (exception.getMessage() != null) {
    		errorMessage = exception.getMessage();
    	}
        response.sendRedirect(frontEndScheme + frontEndHost + frontEndbasePath +"login?error=" + errorMessage);
    }
}
