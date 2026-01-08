# ---- Stage 1: Build ----
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---- Stage 2: Run ----
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# Copiamos el jar resultado del build
COPY --from=builder /app/target/auth_service-0.0.1-SNAPSHOT.jar app.jar

# Exponemos el puerto default Spring Boot
EXPOSE 8080

# Variables de entorno para customización (ajustables al lanzar el contenedor)
ENV SPRING_PROFILES_ACTIVE=prod
# (No declares aqui JWT_SECRET, DB_PASSWORD... ¡eso va en el entorno de producción!)

# Arranque de la app
ENTRYPOINT ["java", "-jar", "app.jar"]
