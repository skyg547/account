import re
import os

with open('docker-compose.yml', 'r', encoding='utf-8') as f:
    content = f.read()

# Pattern to find the build configuration
pattern = r'build:\s*\{context:\s*\.,\s*dockerfile:\s*Containerfile,\s*args:\s*\{GRADLE_PROJECT:\s*"([^"]+)",\s*JAR_DIRECTORY:\s*([^\}]+)\}\}'

matches = re.finditer(pattern, content)

template = """# Stage 1: Build
FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN ./gradlew {gradle_project}:bootJar --console=plain --no-daemon

# Stage 2: Runtime
FROM docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/{jar_directory}/build/libs/*-SNAPSHOT.jar /app/app.jar

USER app:app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
"""

for match in matches:
    gradle_project = match.group(1)
    jar_directory = match.group(2).strip('"')
    
    # Generate Dockerfile
    dockerfile_path = os.path.join(jar_directory, 'Dockerfile')
    os.makedirs(os.path.dirname(dockerfile_path), exist_ok=True)
    
    with open(dockerfile_path, 'w', encoding='utf-8') as f:
        f.write(template.format(gradle_project=gradle_project, jar_directory=jar_directory))
    
    print(f"Created {dockerfile_path}")

# Replace in docker-compose.yml
new_content = re.sub(pattern, lambda m: f'build: {{context: ., dockerfile: {m.group(2).strip(\'"\')}/Dockerfile}}', content)

with open('docker-compose.yml', 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Updated docker-compose.yml")
