# [엔터프라이즈 프론트엔드 서빙 (Nginx Reverse Proxy)]
# Next.js의 SSR/동적 라우팅을 지원하기 위해 Node.js 서버 앞단에 Nginx를 배치합니다.
# 보안, Gzip 압축, 정적 자원 캐싱을 Nginx가 전담하여 성능을 극대화합니다.
FROM nginx:alpine

# Nginx 기본 설정 파일 덮어쓰기
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 443
CMD ["nginx", "-g", "daemon off;"]
