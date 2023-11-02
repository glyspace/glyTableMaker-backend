package org.glygen.tablemaker.security;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.glygen.tablemaker.persistence.GlygenUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenProvider {
    
    Logger log = LoggerFactory.getLogger(TokenProvider.class);

    @Value("${token.signing.key}")
    private String jwtSigningKey;
    
    public String extractUserName(String token) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateToken(GlygenUser userDetails) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return generateToken(new HashMap<>(), userDetails);
    }

    public boolean isTokenValid(String token, GlygenUser userDetails) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolvers) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private String generateToken(Map<String, Object> extraClaims, GlygenUser userDetails) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Jwts.builder().subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + org.glygen.tablemaker.config.SecurityConstants.EXPIRATION_TIME))
                .signWith((SecretKey)getSigningKey()).compact();
    }

    public boolean isTokenExpired(String token) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) throws JwtException, IllegalArgumentException, NoSuchAlgorithmException, InvalidKeySpecException {
        //return Jwts.parser().decryptWith((SecretKey) getSigningKey()).build().parseEncryptedClaims(token).getPayload();
        
        return Jwts.parser()
            .verifyWith((SecretKey) getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
    }

    private Key getSigningKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Keys.hmacShaKeyFor(jwtSigningKey.getBytes());
        
       /* SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(jwtSigningKey.toCharArray(), jwtSigningKey.getBytes(), 65536, 256);
        SecretKey originalKey = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return originalKey;*/
    }
}