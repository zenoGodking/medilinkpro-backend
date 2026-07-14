# ---------- Etape 1 : build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etape 2 : image finale ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/target/medilinkpro-backend.jar app.jar
# Cree le dossier des fichiers uploades (photos d'etablissements...) et donne la
# propriete de /app a l'utilisateur non-root, sinon l'upload echoue (permission refusee).
RUN mkdir -p /app/uploads && chown -R spring:spring /app
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
