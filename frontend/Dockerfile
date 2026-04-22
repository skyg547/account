# 1. 빌드 스테이지 (집 짓는 단계)
FROM node:21-alpine AS builder

# 작업 공간 설정 (상자 안의 위치)
WORKDIR /app

# 패키지 명세서 복사 및 설치
COPY package.json package-lock.json ./
RUN npm install

# 소스 코드 전체 복사 (설계도 가져오기)
COPY . .

# 실제 웹사이트 빌드 (건축 작업)
RUN npm run build

# 2. 실행 스테이지 (완공된 집에 입주하는 단계)
FROM node:21-alpine AS runner

WORKDIR /app

# 운영 환경에 꼭 필요한 파일들만 builder에서 가져오기 (가벼운 배낭 하나만 챙기기)
COPY --from=builder /app/next.config.ts ./
COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json

# 외부와 소통할 창구(포트) 열기
EXPOSE 3000

# 서버 실행!
CMD ["npm", "start"]
