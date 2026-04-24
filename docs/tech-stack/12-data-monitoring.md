# 🎭 [Tech 29] Jackson (JSON Marshalling)

### 👶 초보자를 위한 개념 설명
> **Jackson이란 무엇인가요?**
> 마치 **"자동 번역기 및 포장 기계"**와 같습니다. 자바라는 언어로 된 데이터(Object)를 인터넷 세상에서 돌아다니기 편한 공용 언어인 JSON으로 번역해서 포장해주고, 반대로 들어온 포장지를 뜯어 다시 자바 데이터로 바꿔줍니다.

### 💡 주요 특징 및 예시
1. **Seamless Integration**: 우리가 아무것도 안 해도 스프링 부트가 뒤에서 알아서 데이터를 주물러줍니다.
2. **Annotation Control**: `@JsonProperty`나 `@JsonIgnore`를 써서 특정 데이터만 골라 포장하거나 숨길 수 있습니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[데이터 이름 바꿔서 내보내기]**
1. **어노테이션 추가**: 자바 변수명은 `userId`지만, 밖으로 나갈 땐 `user_id`라고 하고 싶다면?
   ```java
   @JsonProperty("user_id")
   private String userId;
   ```
2. **날짜 형식 지정**: `@JsonFormat`을 써서 날짜를 예쁘게 포장합니다.
3. **확인**: API를 호출해보면 내가 원하는 모양대로 JSON 데이터가 출력됩니다.

---

# 💓 [Tech 30] Spring Boot Actuator

### 👶 초보자를 위한 개념 설명
> **Actuator란 무엇인가요?**
> 마치 **"서버의 실시간 건강 진단기(애플워치)"**와 같습니다. 서버가 현재 심박수(CPU)는 어떤지, 잠은 잘 자고 있는지(Memory), 숨은 잘 쉬고 있는지(Health Check)를 알려주는 관리 전용 모니터링 도구입니다.

### 💡 주요 특징 및 예시
1. **Health Check**: `/actuator/health` 주소 하나로 서버가 살아있는지 즉시 알 수 있습니다.
2. **Metrics**: 지금까지 얼마나 많은 사용자가 들어왔는지 숫자로 보여줍니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[우리 서버 건강검진 하기]**
1. **경로 접속**: 브라우저에서 `localhost:8080/actuator/health`를 쳐봅니다.
2. **상태 확인**: `"status": "UP"`이라고 뜨면 아주 건강하다는 뜻입니다.
3. **상태 수집**: 이 수치들을 프로메테우스(Prometheus)가 쏙쏙 가져가서 나중에 예쁜 그래프로 그려줍니다.
