# Build the Vue assets with the project-supported Node version.
FROM node:22-bookworm AS frontend-build
WORKDIR /workspace/frontend

RUN corepack enable && corepack prepare pnpm@10.15.0 --activate
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm run build

# Package the Spring Boot application with Java 25 and the Maven Wrapper.
FROM eclipse-temurin:25-jdk AS backend-build
WORKDIR /workspace/backend
COPY backend/ ./
COPY --from=frontend-build /workspace/frontend/dist ../frontend/dist
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -Dfrontend.skip=true -DskipTests package

# Build a trimmed Java runtime instead of shipping the complete JRE image.
RUN jlink \
    --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.rmi,java.scripting,java.security.jgss,java.sql,java.transaction.xa,java.xml,jdk.crypto.ec,jdk.unsupported \
    --strip-debug --no-man-pages --no-header-files --compress=2 \
    --output /opt/java-runtime

# Keep the runtime image small and run without root privileges.
FROM debian:bookworm-slim AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install --no-install-recommends --yes ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system spring && useradd --system --gid spring \
    --home-dir /app --shell /usr/sbin/nologin spring
COPY --from=backend-build /opt/java-runtime /opt/java-runtime
COPY --from=backend-build /workspace/backend/target/personajes-*.jar /app/app.jar
RUN chown spring:spring /app/app.jar

ENV JAVA_HOME=/opt/java-runtime
ENV PATH="/opt/java-runtime/bin:${PATH}"
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
USER spring
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:InitialRAMPercentage=10.0", "-XX:MaxRAMPercentage=70.0", "-XX:ActiveProcessorCount=2", "-XX:CICompilerCount=2", "-jar", "/app/app.jar"]
