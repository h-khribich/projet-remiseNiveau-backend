package com.openclassrooms.etudiant.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

  private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
  
  // 24h token (in milliseconds)
  private static final long EXPIRATION_TIME = 86400000; 

  public String generateToken(UserDetails userDetails) {
      return Jwts.builder()
              .setSubject(userDetails.getUsername())
              .setIssuedAt(new Date(System.currentTimeMillis()))
              .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
              .signWith(SECRET_KEY)
              .compact();
  }
}
