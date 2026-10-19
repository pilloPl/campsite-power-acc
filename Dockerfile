# One image: the operations console (Vite) built into the static resources of the Spring Boot server.
FROM node:22-alpine AS console
WORKDIR /console
COPY campsite-console/package.json campsite-console/package-lock.json ./
RUN npm ci
COPY campsite-console/ ./
RUN npx vite build

FROM maven:3.9-eclipse-temurin-21 AS server
WORKDIR /build
COPY pom.xml ./
COPY campsite-commons/pom.xml campsite-commons/
COPY campsite-value-objects/pom.xml campsite-value-objects/
COPY campsite-transfer-objects/pom.xml campsite-transfer-objects/
COPY campsite-api/pom.xml campsite-api/
COPY campsite-db-migrations/pom.xml campsite-db-migrations/
COPY campsite-core/pom.xml campsite-core/
COPY campsite-endpoints/pom.xml campsite-endpoints/
COPY campsite-server/pom.xml campsite-server/
COPY campsite-console/package.json campsite-console/
RUN mvn -q -B dependency:go-offline -DexcludeReactor=true || true
COPY . .
# the console is served by the server; client-side routes (deep links) fall back to index.html via the 404 page
COPY --from=console /console/dist campsite-server/src/main/resources/static
RUN mkdir -p campsite-server/src/main/resources/static/error \
    && cp campsite-server/src/main/resources/static/index.html campsite-server/src/main/resources/static/error/404.html
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=server /build/campsite-server/target/campsite-server-*.jar app.jar
ENV MALLOC_ARENA_MAX=2 \
    JAVA_TOOL_OPTIONS="-Xmx240m -Xss256k -XX:MaxMetaspaceSize=170m -XX:ReservedCodeCacheSize=40m -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080} --spring.profiles.active=${SPRING_PROFILES_ACTIVE:-demo}"]
