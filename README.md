# Exam G2 Messagerie Java

A simple messaging application written in Java.  
This repository contains the source code for the exam project “messagerie” used in the
G2 module. The aim is to demonstrate a basic client‑server messaging system using Maven
for build and dependency management.

## Features

- Java 11+ compatible
- Maven-based project structure (`pom.xml`)
- Basic messaging functionality (send/receive)
- Unit tests with JUnit
- CLI or Swing client (depending on implementation)

## Prerequisites

- JDK 11 or later installed
- [Apache Maven](https://maven.apache.org/) installed and available on `PATH`
- Git (optional, for cloning the repo)

## Setup

Clone the repository or create the directory structure:

```bash
git clone <repo-url> 
cd exam-g2-messagerie-java
```

## Build and Test

Run the following Maven commands from the project root:

```bash
# download dependencies, compile sources, run tests, package JAR
mvn clean install

# compile only
mvn compile

# run unit tests
mvn test

# create a runnable jar (if configured)
mvn package
```

## Running the Application

After building:

```bash
# run the main class via Maven
mvn exec:java -Dexec.mainClass="com.example.messagerie.Main"

# or execute the jar
java -jar target/messagerie‑1.0‑SNAPSHOT.jar
```

Adjust package and class names as necessary.

## Project Structure

```
src/
    main/java        – application sources
    main/resources   – configuration, assets
    test/java        – unit tests
pom.xml            – Maven configuration
```

## Contributing

1. Fork the repo.
2. Create a feature branch.
3. Commit and push changes.
4. Submit a pull request.

## License

Specify your license here (e.g. MIT, Apache 2.0).

---

This README provides basic information to get the project up and running using Maven. Add more
details as development progresses.