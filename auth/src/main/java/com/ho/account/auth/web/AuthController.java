package com.ho.account.auth.web;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // 출입증에 도장을 찍을 공통 비밀키 (Gateway와 동일해야 함)
    private final Key key = Keys.hmacShaKeyFor("kbank-account-system-super-secret-key-1234567890".getBytes());

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        // 더미 인증 로직 (나중에는 DB와 연동)
        if ("admin".equals(username) && "1234".equals(password)) {
            String jwt = Jwts.builder()
                    .setSubject(username)
                    .claim("role", "ROLE_ADMIN")
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1시간 뒤 만료
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

            Map<String, String> response = new HashMap<>();
            response.put("token", jwt);
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).build();
        }
    }
}
