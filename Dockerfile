# --- Build stage: compile and package the application ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Resolve dependencies first so they are cached in their own layer and only
# re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn -B -ntp -DskipTests dependency:go-offline

# Then copy sources and build the executable jar.
COPY src ./src
RUN mvn -B -ntp -DskipTests clean package

# --- Runtime stage: ship the JRE + jar only (no JDK, no Maven, no source) ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as an unprivileged user rather than root.
RUN groupadd --system spring && useradd --system --gid spring spring

COPY --from=build /app/target/*.jar app.jar
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
