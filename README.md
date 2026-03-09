# Exam G2 Messagerie Java

A simple messaging application written in Java.  
This repository contains the source code for the exam project “messagerie” used in the G2 module. The aim is to demonstrate a basic client‑server messaging system using Maven for build and dependency management.

## Features

- Java 17+ compatible
- Maven-based project structure (`pom.xml`)
- Basic messaging functionality (send/receive)
- JavaFX client (depending on implementation)
- Hibernate (ORM for database interaction, PostgreSQL in our case)

## Prerequisites

- JDK 17 or later installed
<!-- - [Apache Maven](https://maven.apache.org/) installed and available on `PATH` -->
- Git (optional, for cloning the repo)
- PostgreSQL installed on your machine.

## Setup

Clone the repository or create the directory structure:

```bash
git clone https://github.com/Cad-prof/exam-g2-messagerie-java.git
cd exam-g2-messagerie-java
```

## Build and Test

Run the following Maven commands from the project root:

```bash
# download dependencies, compile sources, run tests, package JAR
mvn clean install

# compile only
mvn compile

# create a runnable jar (if configured)
mvn package
```

## Running the Application

After building:

```bash
# run the main class via Maven
# mvn exec:java -Dexec.mainClass="com.example.messagerie.Main"

# or execute the jar
# java -jar target/messagerie‑1.0‑SNAPSHOT.jar
```

Adjust package and class names as necessary.

## Project Structure

```txt
g2-messagerie/
├── pom.xml                  <- parent
├── shared/
│   ├── pom.xml
│   └── src/main/java/
│       ├── model/           <- User.java, Message.java
│       ├── dao/             <- UserDAO.java, MessageDAO.java
│       ├── dto/             <- Packet.java
│       └── util/            <- HibernateUtil.java, PasswordUtil.java
├── server/
│   ├── pom.xml
│   └── src/main/java/
│       └── server/          <- Server.java, ClientHandler.java, ServerLogger.java
└── client/
    ├── pom.xml
    └── src/main/
        ├── java/
        │   └── client/      <- ClientApp.java, network/, ui/
        └── resources/       <- login.fxml, chat.fxml, config.properties

```

## Contributing

1. Fork the repo.
2. Create a feature branch.
3. Commit and push changes.
4. Submit a pull request.

## License

---

This README provides basic information to get the project up and running using Maven. Add more details as development progresses.
