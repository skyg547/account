# 🔑 Auth Service (인증 및 출입증 발급 센터)

## 1. 초보자를 위한 개념 설명
MSA 환경에서는 모든 서버가 각자 사용자의 로그인을 검사하면 너무 비효율적입니다.
**Auth Service**는 오직 "사용자의 아이디/비밀번호가 맞는지 확인"하고, 위조가 불가능한 **'JWT 토큰(출입증)'**을 발급해 주는 일만 전담하는 특수 부서입니다.

고객이 이 출입증을 받아 대문(Gateway)에 보여주면, 게이트웨이는 "어? Auth 부서에서 발급한 진짜 출입증 맞네!" 하고 문을 열어줍니다.

## 2. 주요 기능
- `POST /api/auth/login`: 아이디와 비밀번호를 받아 JWT 토큰을 반환합니다.

## 3. 실행 방법
```bash
./gradlew :auth:bootRun
```

## 4. Docker 로 실행하기
```bash
docker build -t account/auth-service .
docker run -p 8084:8084 account/auth-service
```
