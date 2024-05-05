FROM --platform=linux/amd64 eclipse-temurin:21-jre-alpine
RUN mkdir /app
COPY build/libs/auth-1.0.jar /app
CMD ["java", "-jar", "/app/auth-1.0.jar"]