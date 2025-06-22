FROM maven:3.9-eclipse-temurin-21 AS builder
COPY ./ /tmp
RUN mvn -f /tmp/pom.xml clean package -DskipTests

FROM eclipse-temurin:21-jre
COPY --from=builder /tmp/xtbot-telegram/target/xtbot-telegram-0.3.0-SNAPSHOT.jar /app/xtbot.jar
ENTRYPOINT ["java", "-jar", "/app/xtbot.jar"]
