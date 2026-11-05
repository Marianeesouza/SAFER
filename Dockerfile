# Imagem única parametrizada pelo módulo: docker build --build-arg MODULE=usuarios-service .
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /app
COPY . .
RUN ./gradlew :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
