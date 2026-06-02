FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=dev

COPY --from=build /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
