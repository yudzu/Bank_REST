FROM maven:3.8.4-openjdk-17 as builder
WORKDIR /app
COPY pom.xml /app
RUN mvn dependency:resolve
COPY . /app
RUN mvn clean
RUN mvn package

FROM openjdk:17-jdk-alpine
EXPOSE 8080
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/*.jar
ENTRYPOINT ["java","-jar","/app/*.jar"]