package com.ho.account.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * MSA(Microservice Architecture) 환경에서 내부 서비스 간 통신을 위한 WebClient 구성 클래스입니다.
 *
 * <p>[교육적 주석: 서비스 디스커버리와 로드밸런싱의 필요성]</p>
 * <p>1. 서비스 디스커버리 (Service Discovery):</p>
 * <ul>
 *   <li>MSA 환경에서는 컨테이너(Docker, Kubernetes)의 생성·소멸, 자동 확장(Auto-scaling) 또는 무중단 배포로 인해
 *       각 미크로서비스 인스턴스의 IP 주소와 포트 번호가 동적으로 변합니다.</li>
 *   <li>Netflix Eureka 등의 Service Discovery Server는 네트워크 상에 존재하는 인스턴스들의 위치(IP:Port)를 중앙 등록소에 실시간 관리합니다.</li>
 *   <li>이를 통해 클라이언트는 고정 IP 대신 서비스 이름(e.g., 'auth-service')으로 대상 서비스를 추상화하여 호출할 수 있습니다.</li>
 * </ul>
 *
 * <p>2. 클라이언트 측 로드밸런싱 (Client-Side Load Balancing):</p>
 * <ul>
 *   <li>{@link LoadBalanced} 어노테이션이 지정된 {@link WebClient.Builder}는 Spring Cloud LoadBalancer 필터를 체이닝합니다.</li>
 *   <li>'lb://auth-service/api/...'와 같은 논리적 서비스 URL이 요청되면, Discovery Client를 통해 등록된
 *       'auth-service'의 활성 인스턴스 목록(IP:Port)을 실시간으로 가져옵니다.</li>
 *   <li>라운드로빈(Round-Robin) 등의 알고리즘을 사용해 여러 인스턴스로 요청 트래픽을 부하 분산하고, 실제 주소로 URL을 변환(Resolve)하여 호출합니다.</li>
 * </ul>
 */
@Configuration
public class WebClientConfig {

    /**
     * Eureka 서비스 디스커버리와 연동되어 lb:// 식별자를 동적으로 인터셉트 및 라우팅하는
     * LoadBalanced WebClient.Builder Bean을 생성합니다.
     *
     * @return 로드밸런싱 기능이 활성화된 WebClient.Builder
     */
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
