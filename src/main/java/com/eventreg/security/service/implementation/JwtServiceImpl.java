package com.eventreg.security.service.implementation;

import com.eventreg.model.User;
import com.eventreg.security.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

  @Value("${security.secret-key}")
  private String SECRET_KEY;

  @Override
  public String generateToken(User user) {
    Date date =
        Date.from(LocalDateTime.now().plusMinutes(60).atZone(ZoneId.systemDefault()).toInstant());
    return Jwts.builder()
        .subject(user.getUsername())
        .claim("role", user.getRole().name())
        .expiration(date)
        .signWith(getSignInKey())
        .compact();
  }

  @Override
  public String extractUserName(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) getSignInKey())
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  @Override
  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith((SecretKey) getSignInKey()).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Key getSignInKey() {
    byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
