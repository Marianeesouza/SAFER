# Imagem única parametrizada pelo módulo: docker build --build-arg MODULE=usuarios-service .
FROM eclipse-temurin:21-jdk AS build
ARG MODULE
WORKDIR /app
COPY . .
# chmod: o gradlew pode estar sem permissão de execução quando o repositório vem de um checkout no Windows.
RUN chmod +x gradlew && ./gradlew :${MODULE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre
ARG MODULE
WORKDIR /app
COPY --from=build /app/${MODULE}/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
