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
COPY docker-entrypoint.sh /docker-entrypoint.sh

RUN mkdir -p /app/uploads/menus \
    && sed -i 's/\r$//' /docker-entrypoint.sh \
    && chmod +x /docker-entrypoint.sh \
    && chown -R appuser:appuser /app

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
