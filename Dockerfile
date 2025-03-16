# ----- STAGE 1: Build -----
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
    
COPY . .
    
# First, clean and install the project (runs tests and installs to local repo)
RUN mvn clean install -DskipTests=false
    
# Then, package the application (this step ensures the jar is created)
RUN mvn package -DskipTests=false

# Expose the default Spring Boot port
EXPOSE 8080
    
# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "target/demospring-0.0.1-SNAPSHOT.jar"]
    