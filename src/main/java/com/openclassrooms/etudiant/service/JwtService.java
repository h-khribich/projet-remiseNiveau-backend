package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

  // 24h token (in milliseconds)
  private static final long EXPIRATION_TIME = 86400000;
  private final Key secretKey;

  public JwtService(@Value("${jwt.secret}") String secret) {
      this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
  }

  public String generateToken(UserDetails userDetails) {
      return Jwts.builder()
              .setSubject(userDetails.getUsername())
              .setIssuedAt(new Date(System.currentTimeMillis()))
              .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
              .signWith(secretKey)
              .compact();
  }

  public String extractUsername(String token) {
      return extractAllClaims(token).getBody().getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
      String username = extractUsername(token);
      return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
      return extractAllClaims(token).getBody().getExpiration().before(new Date());
  }

  private Jws<Claims> extractAllClaims(String token) throws JwtException {
      return Jwts.parserBuilder()
              .setSigningKey(secretKey)
              .build()
              .parseClaimsJws(token);
  }
}
