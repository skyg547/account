# ==============================================================================
# [엔터프라이즈 프론트엔드 빌드 Dockerfile (Next.js)]
# ------------------------------------------------------------------------------
# 💡 교육적 설계 포인트 (Pedagogical Design Notes):
# 1. Node.js LTS Runtime Pinning:
#    - node:20-alpine (Node 20 LTS) 버전을 사용하여 로컬 개발 환경(.nvmrc, .node-version: 20.18.0)
#      및 package.json engines 계약과 100% 일치시킵니다.
# 2. 개발 및 컨테이너 환경 간 런타임 정합성 (Dev / Container Parity):
#    - 로컬과 컨테이너 환경 간의 Node.js/V8 엔진 불일치로 인한 런타임 오동작을 예방합니다.
# 3. Lockfile v3 결정론적 재현성 (Reproducibility):
#    - npm ci 명령을 통해 package-lock.json (Lockfile v3) 명세를 엄격히 준수하여
#      의존성 설치 시 결정론적(Deterministic) 재현성을 보장합니다.
# ==============================================================================
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV GATEWAY_INTERNAL_URL=http://gateway:8000
COPY --from=builder /app/package*.json ./
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/public ./public
COPY --from=builder /app/node_modules ./node_modules
# Next.js 기본 포트
EXPOSE 3000
CMD ["npm", "start"]
