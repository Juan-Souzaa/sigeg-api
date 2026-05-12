FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src

RUN sed -i 's/\r$//' mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:17-jre-jammy AS runtime

RUN groupadd --system appuser && useradd --system --gid appuser --home-dir /app --shell /usr/sbin/nologin appuser

WORKDIR /app

COPY --from=build /workspace/target/sigeg-api-*.jar /app/app.jar

RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
