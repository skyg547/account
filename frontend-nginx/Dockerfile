# [엔터프라이즈 프론트엔드 서빙 (Nginx Reverse Proxy)]
# Next.js의 SSR/동적 라우팅을 지원하기 위해 Node.js 서버 앞단에 Nginx를 배치합니다.
# 보안, Gzip 압축, 정적 자원 캐싱을 Nginx가 전담하여 성능을 극대화합니다.
FROM nginx:alpine

# Docker/Podman 내부 런타임 DNS 해석기 자동 감지 활성화
ENV NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1

# Nginx 템플릿 파일 복사 (컨테이너 기동 시 NGINX_LOCAL_RESOLVERS 자동 치환)
COPY nginx.conf /etc/nginx/templates/default.conf.template

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
