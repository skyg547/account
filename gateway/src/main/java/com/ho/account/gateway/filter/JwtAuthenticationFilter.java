package com.ho.account.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.security.Key;

/**
 * JWT 검문소 (Gateway Filter)
 * 대문(Gateway)을 통과하려는 모든 API 요청의 헤더를 검사하여 출입증(JWT 토큰)이 있는지, 진짜인지 확인합니다.
 */
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    // governance 서비스에서 토큰을 만들 때 썼던 비밀키와 똑같은 키로 검사합니다.
    private final Key key = Keys.hmacShaKeyFor("kbank-account-system-super-secret-key-1234567890".getBytes());

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. 헤더에 'Authorization' 이라는 이름의 출입증이 있는지 확인
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange.getResponse(), "출입증(Authorization Header)이 없습니다.", HttpStatus.UNAUTHORIZED);
            }

            String authorizationHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);

            // 2. 출입증이 "Bearer " 로 시작하는지 확인
            if (!authorizationHeader.startsWith("Bearer ")) {
                return onError(exchange.getResponse(), "잘못된 형태의 출입증입니다.", HttpStatus.UNAUTHORIZED);
            }

            // 3. 토큰만 쏙 빼냅니다.
            String token = authorizationHeader.replace("Bearer ", "");

            try {
                // 4. 비밀키를 이용해 토큰의 진위 여부 및 만료일을 검사합니다.
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                
                // (선택) 여기서 claims.getSubject() 로 유저 아이디를 꺼내서 헤더에 다시 실어 뒷단 서버로 넘겨줄 수도 있습니다.

            } catch (Exception e) {
                // 토큰이 위조되었거나 유효기간이 지났다면 쫓아냅니다!
                return onError(exchange.getResponse(), "출입증이 위조되었거나 만료되었습니다.", HttpStatus.UNAUTHORIZED);
            }

            // 무사히 검문을 통과했으면 다음 단계(라우팅)로 보냅니다.
            return chain.filter(exchange);
        };
    }

    private Mono<Void> onError(ServerHttpResponse response, String errMessage, HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        // 에러 메시지를 바디에 담아 보내는 것도 좋지만, 예제에서는 단순 상태코드만 반환합니다.
        return response.setComplete();
    }

    public static class Config {
        // 필터에 추가적인 설정값을 넣고 싶을 때 사용 (현재는 비워둠)
    }
}
