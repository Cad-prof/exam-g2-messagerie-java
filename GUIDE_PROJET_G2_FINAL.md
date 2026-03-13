# 📘 Guide Complet — Projet G2 : Messagerie pour Associations
> **Stack** : Java 17 · Hibernate 5 (javax) · PostgreSQL · JavaFX 21 · Maven Multi-Module  
> **Réseau** : localhost + LAN local supportés  
> **Durée** : 10 jours

---

## 📅 Plan sur 10 jours

| Jour | Tâche | Test du jour |
|------|-------|-------------|
| 1 | Setup Maven multi-module + BDD + Entités JPA | Hibernate crée les tables, INSERT/SELECT OK |
| 2 | Couche DAO + PasswordUtil | DAO CRUD + BCrypt + RG1 |
| 3 | Serveur TCP (ServerSocket + threads) | Connexion Netcat, multi-clients, logs |
| 4 | Protocole Packet (sérialisation) | Sérialisation fichier, tous les types |
| 5 | Client TCP (ServerConnection) | Échange Packets, RG3 double session |
| 6 | UI JavaFX — Écran Login/Inscription | Fenêtre s'affiche, inscription fonctionne |
| 7 | UI JavaFX — Écran Chat | Envoi/réception temps réel, RG6, RG7 |
| 8 | Intégration complète | 4 clients simultanés, redémarrage serveur |
| 9 | Journalisation + tests LAN | server.log complet, test 2 machines |
| 10 | Corrections + checklist finale | Toutes les RG vérifiées |

---

## 🗂️ Structure du Projet Maven Multi-Module

```
g2-messagerie/                   ← projet parent (pom.xml racine)
├── pom.xml
├── shared/                      ← code commun client + serveur
│   ├── pom.xml
│   └── src/main/
│       ├── java/
│       │   ├── model/
│       │   │   ├── User.java
│       │   │   └── Message.java
│       │   ├── dao/
│       │   │   ├── UserDAO.java
│       │   │   └── MessageDAO.java
│       │   ├── dto/
│       │   │   └── Packet.java
│       │   └── util/
│       │       ├── HibernateUtil.java
│       │       └── PasswordUtil.java
│       └── resources/
│           └── META-INF/
│               └── persistence.xml
├── server/                      ← application serveur
│   ├── pom.xml
│   └── src/main/java/server/
│       ├── Server.java
│       ├── ClientHandler.java
│       └── ServerLogger.java
└── client/                      ← application client JavaFX
    ├── pom.xml
    └── src/main/
        ├── java/client/
        │   ├── ClientApp.java
        │   ├── network/
        │   │   └── ServerConnection.java
        │   └── ui/
        │       ├── LoginController.java
        │       └── ChatController.java
        └── resources/
            ├── login.fxml
            ├── chat.fxml
            └── config.properties
```

---

## ⚙️ JOUR 1 — Setup Maven + Base de données + Entités JPA

### Création du projet (IntelliJ)

```
File → New → Project → Maven
  Name       : g2-messagerie
  GroupId    : com.g2
  ArtifactId : g2-messagerie

Clic droit sur le projet → New → Module → Maven → Name: shared
Clic droit sur le projet → New → Module → Maven → Name: server
Clic droit sur le projet → New → Module → Maven → Name: client
```

---

### `pom.xml` — Racine (projet parent)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.g2</groupId>
    <artifactId>g2-messagerie</artifactId>
    <version>1.0-SNAPSHOT</version>

    <!--
        packaging=pom : ce module ne contient pas de code,
        c'est uniquement un conteneur qui regroupe les 3 modules.
        IMPORTANT : shared doit être listé EN PREMIER car server
        et client en dépendent. Maven compile dans cet ordre.
    -->
    <packaging>pom</packaging>
    <modules>
        <module>shared</module>
        <module>server</module>
        <module>client</module>
    </modules>

    <!-- Propriétés partagées par tous les modules -->
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!--
        dependencyManagement : déclare les versions centralement.
        Les modules enfants héritent de ces versions sans les répéter.
    -->
    <dependencyManagement>
        <dependencies>
            <!-- Hibernate 5.x → utilise javax.persistence -->
            <dependency>
                <groupId>org.hibernate</groupId>
                <artifactId>hibernate-core</artifactId>
                <version>5.6.15.Final</version>
            </dependency>
            <!-- Connecteur PostgreSQL -->
            <dependency>
                <groupId>org.postgresql</groupId>
                <artifactId>postgresql</artifactId>
                <version>42.7.1</version>
            </dependency>
            <!-- BCrypt pour hacher les mots de passe -->
            <dependency>
                <groupId>at.favre.lib</groupId>
                <artifactId>bcrypt</artifactId>
                <version>0.10.2</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

---

### `shared/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Hérite du parent : récupère les propriétés et versions -->
    <parent>
        <groupId>com.g2</groupId>
        <artifactId>g2-messagerie</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>shared</artifactId>

    <!-- shared contient les entités et DAO : a besoin de Hibernate + PostgreSQL + BCrypt -->
    <dependencies>
        <dependency>
            <groupId>org.hibernate</groupId>
            <artifactId>hibernate-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>at.favre.lib</groupId>
            <artifactId>bcrypt</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

### `server/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.g2</groupId>
        <artifactId>g2-messagerie</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>server</artifactId>

    <dependencies>
        <!--
            Le serveur utilise tout ce qui est dans shared :
            User, Message, UserDAO, MessageDAO, Packet, etc.
        -->
        <dependency>
            <groupId>com.g2</groupId>
            <artifactId>shared</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

---

### `client/pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.g2</groupId>
        <artifactId>g2-messagerie</artifactId>
        <version>1.0-SNAPSHOT</version>
    </parent>

    <artifactId>client</artifactId>

    <dependencies>
        <!-- Le client utilise shared pour Packet, User, Message -->
        <dependency>
            <groupId>com.g2</groupId>
            <artifactId>shared</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- JavaFX : uniquement pour le client -->
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-controls</artifactId>
            <version>21</version>
        </dependency>
        <dependency>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-fxml</artifactId>
            <version>21</version>
        </dependency>
    </dependencies>
</project>
```

---

### Base de données PostgreSQL

```sql
-- Crée la base (à exécuter une seule fois dans psql ou pgAdmin)
CREATE DATABASE messagerie_g2
    ENCODING 'UTF8'
    LC_COLLATE = 'fr_FR.UTF-8'
    LC_CTYPE   = 'fr_FR.UTF-8';

-- Hibernate crée les tables automatiquement (hbm2ddl.auto=update).
-- Ce script est fourni pour référence et compréhension du schéma.

\c messagerie_g2

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password      VARCHAR(255) NOT NULL,       -- hash BCrypt (RG9)
    role          VARCHAR(20)  NOT NULL
                  CHECK (role IN ('ORGANISATEUR','MEMBRE','BENEVOLE')),
    status        VARCHAR(10)  NOT NULL DEFAULT 'OFFLINE'
                  CHECK (status IN ('ONLINE','OFFLINE')),
    date_creation TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS messages (
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT NOT NULL REFERENCES users(id),
    receiver_id BIGINT NOT NULL REFERENCES users(id),
    contenu     VARCHAR(1000) NOT NULL,         -- max 1000 chars (RG7)
    date_envoi  TIMESTAMP NOT NULL,
    statut      VARCHAR(10) NOT NULL DEFAULT 'ENVOYE'
                CHECK (statut IN ('ENVOYE','RECU','LU'))
);
```

> **Note PostgreSQL vs MySQL** : `BIGSERIAL` = auto-incrément PostgreSQL (équivalent de `BIGINT AUTO_INCREMENT` MySQL). Les contraintes `CHECK` remplacent les `ENUM` MySQL car PostgreSQL les gère différemment.

---

### `shared/src/main/resources/META-INF/persistence.xml`

```xml
<!--
    persistence.xml : fichier de configuration JPA/Hibernate.
    Doit être dans META-INF/ dans le classpath (resources/).
    On utilise le namespace javax (Hibernate 5.x).
-->
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
             http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd"
             version="2.2">

    <persistence-unit name="messagerie-pu">
        <!-- Déclare toutes les entités JPA -->
        <class>model.User</class>
        <class>model.Message</class>

        <properties>
            <!-- Connexion PostgreSQL -->
            <property name="javax.persistence.jdbc.url"
                      value="jdbc:postgresql://localhost:5432/messagerie_g2"/>
            <property name="javax.persistence.jdbc.user"     value="postgres"/>
            <property name="javax.persistence.jdbc.password" value="votre_mdp"/>
            <property name="javax.persistence.jdbc.driver"   value="org.postgresql.Driver"/>

            <!-- Dialecte : traduit le JPQL en SQL PostgreSQL spécifique -->
            <property name="hibernate.dialect"
                      value="org.hibernate.dialect.PostgreSQLDialect"/>

            <!--
                hbm2ddl.auto=update : Hibernate crée/met à jour les tables
                automatiquement au démarrage. Ne pas utiliser "create" en
                production (efface tout !). Options : create | update | validate | none
            -->
            <property name="hibernate.hbm2ddl.auto" value="update"/>

            <!-- Affiche le SQL généré dans la console (pratique pour déboguer) -->
            <property name="hibernate.show_sql"      value="true"/>
            <property name="hibernate.format_sql"    value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```

---

### `model/User.java`

```java
package model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un membre de l'association.
 * Mappée à la table "users" en base de données.
 *
 * Serializable est requis car User est inclus dans des Packets
 * envoyés sur le réseau via ObjectOutputStream.
 */
@Entity
@Table(name = "users")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // @Id = clé primaire, IDENTITY = délègue l'auto-incrément à PostgreSQL (BIGSERIAL)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // unique=true → contrainte SQL UNIQUE ; RG1
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // Stocke le hash BCrypt (~60 caractères) ; RG9
    @Column(nullable = false)
    private String password;

    // @Enumerated(STRING) stocke "MEMBRE" au lieu de 0, 1, 2 (plus lisible en BDD)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Statut par défaut OFFLINE ; mis à jour à chaque connexion/déconnexion (RG4)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OFFLINE;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Énumérations définies ici pour garder tout au même endroit
    public enum Role   { ORGANISATEUR, MEMBRE, BENEVOLE }
    public enum Status { ONLINE, OFFLINE }

    // Constructeur vide obligatoire pour Hibernate
    public User() {}

    // Getters et Setters
    public Long          getId()          { return id; }
    public String        getUsername()    { return username; }
    public void          setUsername(String username) { this.username = username; }
    public String        getPassword()    { return password; }
    public void          setPassword(String password) { this.password = password; }
    public Role          getRole()        { return role; }
    public void          setRole(Role role) { this.role = role; }
    public Status        getStatus()      { return status; }
    public void          setStatus(Status status) { this.status = status; }
    public LocalDateTime getDateCreation(){ return dateCreation; }
}
```

---

### `model/Message.java`

```java
package model;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entité JPA représentant un message entre deux utilisateurs.
 * Mappée à la table "messages".
 */
@Entity
@Table(name = "messages")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne = clé étrangère vers users (plusieurs messages, un expéditeur)
    // @JoinColumn précise le nom de la colonne FK en base
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // Max 1000 caractères : RG7
    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    // Statut initial ENVOYE → passe à RECU à la livraison, LU à la lecture
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ENVOYE;

    public enum Statut { ENVOYE, RECU, LU }

    // Constructeur vide obligatoire pour Hibernate
    public Message() {}

    // Getters et Setters
    public Long          getId()       { return id; }
    public User          getSender()   { return sender; }
    public void          setSender(User sender) { this.sender = sender; }
    public User          getReceiver() { return receiver; }
    public void          setReceiver(User receiver) { this.receiver = receiver; }
    public String        getContenu()  { return contenu; }
    public void          setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi(){ return dateEnvoi; }
    public Statut        getStatut()   { return statut; }
    public void          setStatut(Statut statut) { this.statut = statut; }
}
```

---

### `util/HibernateUtil.java`

```java
package util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Singleton qui fournit l'EntityManagerFactory.
 *
 * Pourquoi un singleton ?
 * Créer une EntityManagerFactory est une opération coûteuse (connexion pool,
 * parsing des entités, etc.). On la crée UNE FOIS au démarrage et on la
 * réutilise pour toute la durée de vie de l'application.
 *
 * EntityManager (créé depuis la factory) est léger et créé/fermé
 * pour chaque opération dans les DAO.
 */
public class HibernateUtil {

    // Initialisé une seule fois au chargement de la classe
    private static final EntityManagerFactory FACTORY =
        Persistence.createEntityManagerFactory("messagerie-pu");

    public static EntityManagerFactory getFactory() {
        return FACTORY;
    }

    // Appelé à l'arrêt de l'application pour libérer le pool de connexions
    public static void close() {
        if (FACTORY != null && FACTORY.isOpen()) {
            FACTORY.close();
        }
    }
}
```

---

### Test du Jour 1

```java
// shared/src/main/java/TestDay1.java  (à supprimer après le test)
import model.User;
import util.HibernateUtil;
import javax.persistence.EntityManager;

public class TestDay1 {
    public static void main(String[] args) {

        EntityManager em = HibernateUtil.getFactory().createEntityManager();

        // Test 1 : connexion à PostgreSQL
        System.out.println("✅ Connexion à la base réussie !");

        // Test 2 : INSERT
        em.getTransaction().begin();
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hash_temporaire");
        user.setRole(User.Role.MEMBRE);
        em.persist(user);
        em.getTransaction().commit();
        System.out.println("✅ User inséré avec ID : " + user.getId());

        // Test 3 : SELECT
        User found = em.find(User.class, user.getId());
        System.out.println("✅ User retrouvé : " + found.getUsername());

        em.close();
        HibernateUtil.close();
    }
}
```

**Checklist Jour 1 :**
- [ ] Pas d'exception au démarrage
- [ ] Tables `users` et `messages` visibles dans pgAdmin / `\dt`
- [ ] INSERT et SELECT fonctionnent en console

---

## 🗃️ JOUR 2 — Couche DAO + PasswordUtil

**Concept DAO (Data Access Object)** : on regroupe tout le code d'accès à la base dans des classes dédiées. Le reste du code (serveur, UI) ne sait pas si on utilise PostgreSQL, MySQL ou autre chose — il appelle juste `userDAO.findByUsername()`.

**EntityManager** : objet JPA principal. Opérations principales :
- `persist(obj)` → INSERT
- `find(Class, id)` → SELECT par ID
- `merge(obj)` → UPDATE
- `remove(obj)` → DELETE
- `createQuery(jpql)` → SELECT personnalisé

---

### `dao/UserDAO.java`

```java
package dao;

import model.User;
import util.HibernateUtil;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Toutes les opérations BDD liées aux utilisateurs.
 * Chaque méthode ouvre son propre EntityManager et le ferme dans le finally.
 */
public class UserDAO {

    /**
     * Enregistre un nouvel utilisateur (INSERT).
     * Lève une exception si le username est déjà pris (RG1 → contrainte UNIQUE).
     */
    public void save(User user) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback(); // Annule si erreur (ex : doublon)
            throw e;                        // Remonte l'exception pour la gérer plus haut
        } finally {
            em.close(); // Toujours fermer pour libérer la connexion
        }
    }

    /**
     * Cherche un utilisateur par username via JPQL.
     * JPQL (Java Persistence Query Language) ressemble à SQL mais
     * s'applique aux classes Java, pas aux tables.
     * "User u" → la classe User avec alias u
     * "u.username" → la propriété Java (pas le nom de colonne)
     * Retourne null si introuvable.
     */
    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .getSingleResult();
        } catch (Exception e) {
            return null; // NoResultException → aucun user trouvé
        } finally {
            em.close();
        }
    }

    /**
     * Met à jour le statut ONLINE/OFFLINE (RG4).
     * em.find() retourne une entité "managée" (surveillée par Hibernate).
     * Modifier une entité managée suffit : Hibernate détecte le changement
     * et génère l'UPDATE au commit.
     */
    public void updateStatus(User user, User.Status status) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            User managed = em.find(User.class, user.getId());
            managed.setStatus(status);
            em.getTransaction().commit(); // → UPDATE users SET status=... WHERE id=...
        } finally {
            em.close();
        }
    }

    /**
     * Tous les membres inscrits — pour ORGANISATEUR (RG13).
     */
    public List<User> findAll() {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Membres actuellement connectés (pour la liste dans l'UI).
     */
    public List<User> findOnlineUsers() {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            return em.createQuery(
                    "SELECT u FROM User u WHERE u.status = :s ORDER BY u.username", User.class)
                .setParameter("s", User.Status.ONLINE)
                .getResultList();
        } finally {
            em.close();
        }
    }
}
```

---

### `dao/MessageDAO.java`

```java
package dao;

import model.Message;
import model.User;
import util.HibernateUtil;
import javax.persistence.EntityManager;
import java.util.List;

/**
 * Toutes les opérations BDD liées aux messages.
 */
public class MessageDAO {

    /** Sauvegarde un nouveau message (statut ENVOYE par défaut). */
    public void save(Message message) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Historique de conversation entre deux utilisateurs (RG8).
     * La requête récupère les messages dans les 2 sens (A→B et B→A).
     * ORDER BY dateEnvoi ASC = ordre chronologique (RG8).
     */
    public List<Message> findConversation(User userA, User userB) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            return em.createQuery(
                "SELECT m FROM Message m WHERE " +
                "(m.sender = :a AND m.receiver = :b) OR " +
                "(m.sender = :b AND m.receiver = :a) " +
                "ORDER BY m.dateEnvoi ASC", Message.class)
                .setParameter("a", userA)
                .setParameter("b", userB)
                .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Messages non livrés pour un utilisateur hors-ligne (RG6).
     * Statut ENVOYE = sauvegardé mais pas encore livré au destinataire.
     */
    public List<Message> findPendingMessages(User receiver) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            return em.createQuery(
                "SELECT m FROM Message m WHERE m.receiver = :r " +
                "AND m.statut = :s ORDER BY m.dateEnvoi ASC", Message.class)
                .setParameter("r", receiver)
                .setParameter("s", Message.Statut.ENVOYE)
                .getResultList();
        } finally {
            em.close();
        }
    }

    /** Passe le statut du message à RECU après livraison. */
    public void markAsReceived(Message message) {
        EntityManager em = HibernateUtil.getFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            Message managed = em.find(Message.class, message.getId());
            managed.setStatut(Message.Statut.RECU);
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }
}
```

---

### `util/PasswordUtil.java`

```java
package util;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utilitaire de hachage et vérification des mots de passe (RG9).
 *
 * BCrypt est un algorithme conçu spécifiquement pour les mots de passe :
 * - Intègre un "sel" aléatoire → même mot de passe = hashs différents
 * - Paramètre de coût (cost=12) → ralentit les attaques par force brute
 * - Unidirectionnel → impossible de retrouver le mot de passe original
 *
 * On ne stocke JAMAIS le mot de passe en clair en base.
 */
public class PasswordUtil {

    private static final int COST = 12; // Entre 10 et 14 recommandé

    /** Retourne le hash BCrypt du mot de passe. */
    public static String hash(String password) {
        return BCrypt.withDefaults().hashToString(COST, password.toCharArray());
    }

    /**
     * Vérifie que le mot de passe correspond au hash stocké.
     * Retourne true si correspondance, false sinon.
     */
    public static boolean verify(String password, String storedHash) {
        return BCrypt.verifyer()
            .verify(password.toCharArray(), storedHash)
            .verified;
    }
}
```

---

### Test du Jour 2

```java
// TestDay2.java
import dao.MessageDAO;
import dao.UserDAO;
import model.Message;
import model.User;
import util.PasswordUtil;
import java.util.List;

public class TestDay2 {
    public static void main(String[] args) {

        UserDAO    userDAO = new UserDAO();
        MessageDAO msgDAO  = new MessageDAO();

        // --- Test 2.1 : inscription ---
        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword(PasswordUtil.hash("motdepasse123"));
        bob.setRole(User.Role.ORGANISATEUR);
        userDAO.save(bob);
        System.out.println("✅ bob inscrit");

        // --- Test 2.2 : recherche ---
        User found = userDAO.findByUsername("bob");
        System.out.println("✅ Trouvé : " + found.getUsername() + " / " + found.getRole());
        System.out.println("✅ Introuvable retourne null : " + (userDAO.findByUsername("xxx") == null));

        // --- Test 2.3 : RG1 doublon ---
        try {
            User doublon = new User();
            doublon.setUsername("bob");
            doublon.setPassword(PasswordUtil.hash("autre"));
            doublon.setRole(User.Role.MEMBRE);
            userDAO.save(doublon);
            System.out.println("❌ Doublon accepté — RG1 ECHOUEE !");
        } catch (Exception e) {
            System.out.println("✅ RG1 OK : doublon refusé");
        }

        // --- Test 2.4 : BCrypt ---
        String h1 = PasswordUtil.hash("secret");
        String h2 = PasswordUtil.hash("secret");
        System.out.println("✅ Hashs différents (sel) : " + !h1.equals(h2));
        System.out.println("✅ Vérif correcte : " + PasswordUtil.verify("secret", h1));
        System.out.println("✅ Vérif fausse   : " + !PasswordUtil.verify("wrong", h1));

        // --- Test 2.5 : messages ---
        User alice = userDAO.findByUsername("alice"); // inséré Jour 1
        Message msg = new Message();
        msg.setSender(alice);
        msg.setReceiver(bob);
        msg.setContenu("Salut Bob !");
        msgDAO.save(msg);
        System.out.println("✅ Message sauvegardé ID : " + msg.getId());

        List<Message> conv = msgDAO.findConversation(alice, bob);
        System.out.println("✅ Conversation : " + conv.size() + " message(s)");
        conv.forEach(m -> System.out.println("  → " + m.getSender().getUsername()
            + " : " + m.getContenu() + " [" + m.getStatut() + "]"));
    }
}
```

**Checklist Jour 2 :**
- [ ] `UserDAO.save()` fonctionne
- [ ] `findByUsername()` retourne null si inexistant
- [ ] RG1 : doublon déclenche une exception
- [ ] BCrypt : hashs différents, vérification correcte
- [ ] `MessageDAO` : save et findConversation fonctionnent

---

## 🌐 JOUR 3 — Serveur TCP

**Concepts clés :**
- `ServerSocket(port)` : ouvre un port et écoute les connexions. Par défaut écoute sur **toutes les interfaces** (localhost + IP LAN) — rien à faire de spécial pour le réseau local.
- `serverSocket.accept()` : bloque jusqu'à ce qu'un client se connecte, puis retourne un `Socket`.
- `Thread` : unité d'exécution parallèle. RG11 exige un thread par client.
- `ConcurrentHashMap` : Map thread-safe — plusieurs threads peuvent la lire/modifier simultanément sans corruption.

---

### `server/ServerLogger.java`

```java
package server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Journalise les événements dans la console ET dans server.log (RG12).
 *
 * synchronized : garantit qu'un seul thread écrit à la fois.
 * Sans synchronized, plusieurs ClientHandler pourraient écrire
 * simultanément et mélanger les lignes du fichier.
 */
public class ServerLogger {

    private static final String LOG_FILE = "server.log";
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String message) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + message;

        // Affiche dans la console
        System.out.println(entry);

        // Écrit dans le fichier (true = append, ne pas écraser le fichier existant)
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(entry);
        } catch (IOException e) {
            System.err.println("Erreur écriture log : " + e.getMessage());
        }
    }
}
```

---

### `server/Server.java`

```java
package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point d'entrée du serveur.
 *
 * ServerSocket(5000) écoute sur TOUTES les interfaces réseau :
 * → localhost (127.0.0.1) pour les tests locaux
 * → IP LAN (192.168.x.x) pour les clients sur le réseau local
 * Aucune configuration supplémentaire nécessaire pour le LAN.
 *
 * connectedClients : annuaire des clients connectés.
 * "static" car partagé entre tous les ClientHandler (un par client).
 */
public class Server {

    public static final int PORT = 5000;

    // username → handler du client (pour envoyer un message à un user précis)
    public static final ConcurrentHashMap<String, ClientHandler> connectedClients =
        new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // try-with-resources : ferme automatiquement le ServerSocket à la fin
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            ServerLogger.log("Serveur démarré sur le port " + PORT);
            ServerLogger.log("IP locale : " + java.net.InetAddress.getLocalHost().getHostAddress());

            while (true) {
                // accept() bloque ici jusqu'à la prochaine connexion
                Socket clientSocket = serverSocket.accept();
                ServerLogger.log("Nouvelle connexion : " + clientSocket.getInetAddress());

                // Crée un handler pour ce client et le lance dans un nouveau thread (RG11)
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        }
    }
}
```

---

### `server/ClientHandler.java`

```java
package server;

import dao.MessageDAO;
import dao.UserDAO;
import dto.Packet;
import model.Message;
import model.User;
import util.PasswordUtil;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Gère la communication avec UN client connecté.
 * Implémente Runnable pour s'exécuter dans un Thread séparé (RG11).
 *
 * ObjectInputStream / ObjectOutputStream : sérialisent/désérialisent
 * les objets Java (Packet) pour les envoyer sur le réseau sous forme de bytes.
 *
 * IMPORTANT : initialiser out AVANT in — sinon les deux côtés
 * attendent que l'autre commence → deadlock.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private User currentUser; // null tant que non authentifié

    private final UserDAO    userDAO = new UserDAO();
    private final MessageDAO msgDAO  = new MessageDAO();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // out en premier (évite le deadlock)
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            Packet packet;
            // Boucle de lecture : attend le prochain Packet du client
            while ((packet = (Packet) in.readObject()) != null) {
                handlePacket(packet);
            }

        } catch (EOFException | IOException e) {
            // Client déconnecté (fermeture normale ou perte réseau)
            handleDisconnection();
        } catch (ClassNotFoundException e) {
            ServerLogger.log("Erreur désérialisation : " + e.getMessage());
        }
    }

    /** Dirige le Packet vers le bon traitement selon son type. */
    private void handlePacket(Packet packet) throws IOException {
        switch (packet.getType()) {
            case REGISTER -> handleRegister(packet);
            case LOGIN    -> handleLogin(packet);
            case SEND_MSG -> handleSendMessage(packet);
            case LOGOUT   -> handleDisconnection();
        }
    }

    /** Inscription (RG1, RG9). */
    private void handleRegister(Packet packet) {
        String username = packet.getUsername();

        // RG1 : username unique
        if (userDAO.findByUsername(username) != null) {
            sendPacket(Packet.error("Ce nom d'utilisateur est déjà pris."));
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.hash(packet.getPassword())); // RG9
        user.setRole(packet.getRole());
        userDAO.save(user);

        sendPacket(Packet.success("Inscription réussie !"));
        ServerLogger.log("Nouvel inscrit : " + username + " (" + packet.getRole() + ")");
    }

    /** Connexion (RG2, RG3, RG4). */
    private void handleLogin(Packet packet) {
        String username = packet.getUsername();
        User user = userDAO.findByUsername(username);

        // Identifiants incorrects
        if (user == null || !PasswordUtil.verify(packet.getPassword(), user.getPassword())) {
            sendPacket(Packet.error("Identifiants incorrects."));
            return;
        }

        // RG3 : une seule session par utilisateur
        if (Server.connectedClients.containsKey(username)) {
            sendPacket(Packet.error("Cet utilisateur est déjà connecté."));
            return;
        }

        // RG4 : passe ONLINE
        this.currentUser = user;
        userDAO.updateStatus(user, User.Status.ONLINE);
        Server.connectedClients.put(username, this);

        sendPacket(Packet.loginSuccess(user));
        ServerLogger.log("Connexion : " + username);

        // RG6 : livrer les messages reçus pendant la déconnexion
        deliverPendingMessages();

        // Notifier tous les clients de la nouvelle connexion
        broadcast(Packet.userConnected(username));
    }

    /** Livraison des messages en attente à la connexion (RG6). */
    private void deliverPendingMessages() {
        List<Message> pending = msgDAO.findPendingMessages(currentUser);
        for (Message msg : pending) {
            sendPacket(Packet.incomingMessage(msg));
            msgDAO.markAsReceived(msg);
        }
        if (!pending.isEmpty()) {
            ServerLogger.log(pending.size() + " message(s) en attente livrés à " + currentUser.getUsername());
        }
    }

    /** Envoi d'un message (RG5, RG6, RG7). */
    private void handleSendMessage(Packet packet) {
        // RG2 : doit être authentifié
        if (currentUser == null) {
            sendPacket(Packet.error("Non authentifié."));
            return;
        }

        String contenu = packet.getContenu();

        // RG7 : contenu valide
        if (contenu == null || contenu.isBlank()) {
            sendPacket(Packet.error("Le message ne peut pas être vide."));
            return;
        }
        if (contenu.length() > 1000) {
            sendPacket(Packet.error("Message trop long (max 1000 caractères)."));
            return;
        }

        String receiverName = packet.getReceiverUsername();
        User receiver = userDAO.findByUsername(receiverName);

        // RG5 : le destinataire doit exister
        if (receiver == null) {
            sendPacket(Packet.error("Destinataire '" + receiverName + "' introuvable."));
            return;
        }

        // Sauvegarde en base
        Message message = new Message();
        message.setSender(currentUser);
        message.setReceiver(receiver);
        message.setContenu(contenu);
        msgDAO.save(message);

        ServerLogger.log(currentUser.getUsername() + " → " + receiverName + " : " + contenu);

        // Livraison immédiate si destinataire connecté (sinon RG6 : livraison différée)
        ClientHandler receiverHandler = Server.connectedClients.get(receiverName);
        if (receiverHandler != null) {
            receiverHandler.sendPacket(Packet.incomingMessage(message));
            msgDAO.markAsReceived(message);
        }
    }

    /** Déconnexion propre ou suite à une perte réseau (RG4, RG10). */
    private void handleDisconnection() {
        if (currentUser != null) {
            Server.connectedClients.remove(currentUser.getUsername());
            userDAO.updateStatus(currentUser, User.Status.OFFLINE); // RG4
            broadcast(Packet.userDisconnected(currentUser.getUsername()));
            ServerLogger.log("Déconnexion : " + currentUser.getUsername());
            currentUser = null;
        }
        try { socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Envoie un Packet à CE client.
     * synchronized : évite que 2 threads écrivent simultanément
     * sur le même flux (ex : broadcast + livraison directe en même temps).
     * out.reset() : vide le cache d'objets — sans ça, Hibernate peut
     * envoyer une référence au lieu de l'objet complet.
     */
    public synchronized void sendPacket(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            handleDisconnection();
        }
    }

    /** Envoie un Packet à TOUS les clients connectés. */
    private void broadcast(Packet packet) {
        Server.connectedClients.values().forEach(h -> h.sendPacket(packet));
    }
}
```

---

### Test du Jour 3

```bash
# Terminal 1 : lance le serveur
# → Console : "[...] Serveur démarré sur le port 5000"
# → Console : "[...] IP locale : 192.168.1.X"

# Terminal 2, 3, 4 : connexions simultanées (RG11)
nc localhost 5000          # Linux/Mac
# ou
Test-NetConnection localhost -Port 5000   # Windows PowerShell

# Ferme un terminal → serveur continue sans crash

# Vérifie server.log
cat server.log
```

**Checklist Jour 3 :**
- [ ] Serveur démarre, affiche l'IP LAN
- [ ] Connexion Netcat acceptée
- [ ] 3 connexions simultanées sans crash
- [ ] Déconnexion d'un client ne plante pas les autres
- [ ] `server.log` créé et rempli

---

## 📦 JOUR 4 — Protocole Packet

**Concept** : `Packet` est l'objet échangé entre client et serveur. Il doit implémenter `Serializable` pour traverser le réseau via `ObjectOutputStream`.

**Factory methods** : méthodes statiques qui créent des Packets préconfigurés. Plus lisible que `new Packet()` + 5 setters.

---

### `dto/Packet.java`

```java
package dto;

import model.Message;
import model.User;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Objet échangé entre client et serveur sur le réseau.
 *
 * Serializable : permet la conversion en bytes pour ObjectOutputStream.
 * serialVersionUID : version de sérialisation — si la classe change,
 * changer ce numéro évite des erreurs de désérialisation avec
 * d'anciens fichiers/connexions.
 *
 * On utilise des factory methods statiques plutôt qu'un constructeur public :
 * plus clair à lire (Packet.login(...) vs new Packet(LOGIN, ...)).
 */
public class Packet implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        // Depuis le client
        REGISTER, LOGIN, LOGOUT, SEND_MSG, GET_HISTORY,
        // Depuis le serveur
        LOGIN_SUCCESS, SUCCESS, ERROR,
        INCOMING_MSG, HISTORY_RESPONSE,
        USER_CONNECTED, USER_DISCONNECTED
    }

    private Type          type;
    private String        username;
    private String        password;
    private User.Role     role;
    private String        receiverUsername;
    private String        contenu;
    private String        statusMessage; // message de succès ou d'erreur
    private User          user;          // objet User complet (à la connexion)
    private Message       message;       // objet Message complet
    private LocalDateTime timestamp;

    // Constructeur privé → on passe par les factory methods
    private Packet(Type type) {
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // ── Factory methods ──────────────────────────────────────────────

    public static Packet register(String username, String password, User.Role role) {
        Packet p = new Packet(Type.REGISTER);
        p.username = username;
        p.password = password;
        p.role = role;
        return p;
    }

    public static Packet login(String username, String password) {
        Packet p = new Packet(Type.LOGIN);
        p.username = username;
        p.password = password;
        return p;
    }

    public static Packet logout() {
        return new Packet(Type.LOGOUT);
    }

    public static Packet sendMessage(String receiverUsername, String contenu) {
        Packet p = new Packet(Type.SEND_MSG);
        p.receiverUsername = receiverUsername;
        p.contenu = contenu;
        return p;
    }

    public static Packet loginSuccess(User user) {
        Packet p = new Packet(Type.LOGIN_SUCCESS);
        p.user = user;
        return p;
    }

    public static Packet incomingMessage(Message message) {
        Packet p = new Packet(Type.INCOMING_MSG);
        p.message = message;
        return p;
    }

    public static Packet success(String message) {
        Packet p = new Packet(Type.SUCCESS);
        p.statusMessage = message;
        return p;
    }

    public static Packet error(String message) {
        Packet p = new Packet(Type.ERROR);
        p.statusMessage = message;
        return p;
    }

    public static Packet userConnected(String username) {
        Packet p = new Packet(Type.USER_CONNECTED);
        p.username = username;
        return p;
    }

    public static Packet userDisconnected(String username) {
        Packet p = new Packet(Type.USER_DISCONNECTED);
        p.username = username;
        return p;
    }

    // ── Getters ──────────────────────────────────────────────────────

    public Type          getType()            { return type; }
    public String        getUsername()        { return username; }
    public String        getPassword()        { return password; }
    public User.Role     getRole()            { return role; }
    public String        getReceiverUsername(){ return receiverUsername; }
    public String        getContenu()         { return contenu; }
    public String        getStatusMessage()   { return statusMessage; }
    public User          getUser()            { return user; }
    public Message       getMessage()         { return message; }
    public LocalDateTime getTimestamp()       { return timestamp; }
}
```

---

### Test du Jour 4

```java
// TestDay4.java
import dto.Packet;
import model.User;
import java.io.*;

public class TestDay4 {
    public static void main(String[] args) throws Exception {

        // Test : sérialisation → fichier → désérialisation
        Packet original = Packet.register("carol", "pass123", User.Role.MEMBRE);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("test_packet.bin"))) {
            oos.writeObject(original);
            System.out.println("✅ Packet sérialisé");
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("test_packet.bin"))) {
            Packet restored = (Packet) ois.readObject();
            System.out.println("✅ Type     : " + restored.getType());
            System.out.println("✅ Username : " + restored.getUsername());
            System.out.println("✅ Role     : " + restored.getRole());
            System.out.println("✅ Timestamp: " + restored.getTimestamp());
        }

        // Test : tous les types
        System.out.println("--- Types ---");
        System.out.println(Packet.login("u","p").getType());              // LOGIN
        System.out.println(Packet.error("oops").getType());              // ERROR
        System.out.println(Packet.success("ok").getType());              // SUCCESS
        System.out.println(Packet.sendMessage("bob","hi").getType());    // SEND_MSG
        System.out.println(Packet.logout().getType());                   // LOGOUT
        System.out.println(Packet.userConnected("alice").getType());     // USER_CONNECTED

        // Nettoyage
        new File("test_packet.bin").delete();
    }
}
```

**Checklist Jour 4 :**
- [ ] Sérialisation/désérialisation sans perte
- [ ] Tous les types créés correctement
- [ ] `LocalDateTime` bien sérialisable

---

## 🖥️ JOUR 5 — Client TCP

**Concept Consumer\<T\>** : interface fonctionnelle Java qui représente une action à effectuer avec un objet de type T. Ici, quand un Packet arrive du serveur, on "consomme" ce Packet (on l'affiche, on met à jour l'UI, etc.). Cela permet de séparer le code réseau du code UI.

**Thread daemon** : thread qui s'arrête automatiquement quand tous les threads principaux sont terminés. Idéal pour les threads d'écoute en arrière-plan.

---

### `client/network/ServerConnection.java`

```java
package client.network;

import dto.Packet;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gère la connexion socket côté client.
 *
 * Le constructeur reçoit un Consumer<Packet> : une fonction qui sera
 * appelée à chaque Packet reçu du serveur. Le code réseau ne connaît
 * pas l'UI — il délègue le traitement au caller (LoginController, ChatController).
 */
public class ServerConnection {

    private Socket             socket;
    private ObjectOutputStream out;
    private ObjectInputStream  in;
    private boolean            connected = false;

    private final Consumer<Packet> onPacketReceived;

    public ServerConnection(Consumer<Packet> onPacketReceived) {
        this.onPacketReceived = onPacketReceived;
    }

    /**
     * Établit la connexion avec le serveur.
     * host : "localhost" en local, "192.168.x.x" sur le LAN.
     * port : 5000 (doit correspondre au serveur).
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // out AVANT in (même règle que côté serveur)
        out = new ObjectOutputStream(socket.getOutputStream());
        in  = new ObjectInputStream(socket.getInputStream());
        connected = true;

        // Thread d'écoute en arrière-plan
        Thread listener = new Thread(this::listenLoop);
        listener.setDaemon(true); // S'arrête quand l'app JavaFX se ferme
        listener.start();
    }

    /** Boucle de réception des Packets entrants. */
    private void listenLoop() {
        try {
            Packet packet;
            while ((packet = (Packet) in.readObject()) != null) {
                onPacketReceived.accept(packet); // Délègue au controller
            }
        } catch (IOException | ClassNotFoundException e) {
            // RG10 : connexion perdue → notifie l'UI
            connected = false;
            onPacketReceived.accept(Packet.error("Connexion au serveur perdue."));
        }
    }

    /** Envoie un Packet au serveur. */
    public synchronized void send(Packet packet) {
        if (!connected) return;
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            connected = false;
            onPacketReceived.accept(Packet.error("Impossible d'envoyer le message."));
        }
    }

    /** Ferme proprement la connexion. */
    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
}
```

---

### `client/resources/config.properties`

```properties
# Adresse IP du serveur
# Changer "localhost" par l'IP LAN du serveur pour le réseau local
# Exemple LAN : server.host=192.168.1.10
server.host=localhost
server.port=5000
```

---

### Test du Jour 5

```java
// TestDay5.java (dans le module client)
import client.network.ServerConnection;
import dto.Packet;
import model.User;

public class TestDay5 {
    public static void main(String[] args) throws Exception {

        ServerConnection conn = new ServerConnection(packet ->
            System.out.println("📨 " + packet.getType()
                + (packet.getStatusMessage() != null
                    ? " → " + packet.getStatusMessage() : ""))
        );

        conn.connect("localhost", 5000);
        System.out.println("✅ Connecté");
        Thread.sleep(300);

        conn.send(Packet.register("testuser", "pass123", User.Role.MEMBRE));
        Thread.sleep(300); // → SUCCESS

        conn.send(Packet.login("testuser", "pass123"));
        Thread.sleep(300); // → LOGIN_SUCCESS

        conn.send(Packet.login("testuser", "mauvais"));
        Thread.sleep(300); // → ERROR

        // Test RG3 : double session
        ServerConnection conn2 = new ServerConnection(p ->
            System.out.println("📨 C2: " + p.getType()
                + (p.getStatusMessage() != null ? " → " + p.getStatusMessage() : ""))
        );
        conn2.connect("localhost", 5000);
        Thread.sleep(300);
        conn2.send(Packet.login("testuser", "pass123"));
        Thread.sleep(300); // → ERROR "déjà connecté"

        conn.disconnect();
        conn2.disconnect();
        System.out.println("✅ Test terminé");
    }
}
```

**Checklist Jour 5 :**
- [ ] Inscription → SUCCESS
- [ ] Connexion correcte → LOGIN_SUCCESS
- [ ] Mauvais mot de passe → ERROR
- [ ] RG3 : double session → ERROR
- [ ] Test depuis une autre machine du LAN (changer `localhost` par l'IP)

---

## 🎨 JOURS 6-7 — Interface JavaFX

**Concepts JavaFX :**
- **FXML** : fichier XML qui décrit la mise en page (séparation UI / logique).
- **@FXML** : lie un élément du fichier FXML à une variable Java.
- **Platform.runLater()** : les mises à jour de l'UI **ne peuvent se faire que depuis le thread JavaFX**. Le thread réseau doit passer par cette méthode.
- **ObservableList** : liste surveillée par JavaFX — toute modification met à jour l'affichage automatiquement.

---

### `client/resources/login.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<VBox xmlns:fx="http://javafx.com/fxml"
      fx:controller="client.ui.LoginController"
      spacing="10" style="-fx-padding: 20;">

    <Label text="G2 — Messagerie Association" style="-fx-font-size: 18; -fx-font-weight: bold;"/>

    <!-- Champ IP du serveur pour support LAN -->
    <HBox spacing="5" alignment="CENTER_LEFT">
        <Label text="Serveur :"/>
        <TextField fx:id="serverIpField" promptText="localhost ou 192.168.x.x" prefWidth="200"/>
        <Label text=":5000"/>
    </HBox>

    <TabPane fx:id="tabPane">
        <!-- Onglet Connexion -->
        <Tab text="Connexion" closable="false">
            <VBox spacing="8" style="-fx-padding: 10;">
                <TextField fx:id="loginUsernameField" promptText="Nom d'utilisateur"/>
                <PasswordField fx:id="loginPasswordField" promptText="Mot de passe"/>
                <Button text="Se connecter" onAction="#onLogin" maxWidth="Infinity"/>
            </VBox>
        </Tab>

        <!-- Onglet Inscription -->
        <Tab text="Inscription" closable="false">
            <VBox spacing="8" style="-fx-padding: 10;">
                <TextField fx:id="registerUsernameField" promptText="Nom d'utilisateur"/>
                <PasswordField fx:id="registerPasswordField" promptText="Mot de passe"/>
                <ComboBox fx:id="roleComboBox" maxWidth="Infinity"/>
                <Button text="S'inscrire" onAction="#onRegister" maxWidth="Infinity"/>
            </VBox>
        </Tab>
    </TabPane>

    <Label fx:id="statusLabel" text="" wrapText="true"/>
</VBox>
```

---

### `client/ui/LoginController.java`

```java
package client.ui;

import client.network.ServerConnection;
import dto.Packet;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.User;

import java.io.FileInputStream;
import java.util.Properties;

/**
 * Contrôleur de l'écran de connexion/inscription.
 * @FXML : annotation qui lie les variables aux éléments du fichier login.fxml.
 */
public class LoginController {

    @FXML private TextField    serverIpField;
    @FXML private TabPane      tabPane;
    @FXML private TextField    loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private TextField    registerUsernameField;
    @FXML private PasswordField registerPasswordField;
    @FXML private ComboBox<User.Role> roleComboBox;
    @FXML private Label        statusLabel;

    private ServerConnection connection;

    /**
     * Appelé automatiquement par JavaFX après le chargement du FXML.
     * Équivalent d'un constructeur pour les contrôleurs FXML.
     */
    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll(User.Role.values());
        roleComboBox.setValue(User.Role.MEMBRE);

        // Charge l'IP depuis config.properties
        serverIpField.setText(loadServerHost());

        // Crée la connexion avec le callback de réception
        connection = new ServerConnection(this::handleServerResponse);
    }

    /** Lit le fichier config.properties pour l'IP du serveur. */
    private String loadServerHost() {
        try {
            Properties config = new Properties();
            config.load(new FileInputStream("config.properties"));
            return config.getProperty("server.host", "localhost");
        } catch (Exception e) {
            return "localhost"; // Valeur par défaut si fichier absent
        }
    }

    /** Connexion au serveur puis envoi du Packet LOGIN. */
    @FXML
    private void onLogin() {
        String host     = serverIpField.getText().trim();
        String username = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("❌ Veuillez remplir tous les champs.", true);
            return;
        }

        connectIfNeeded(host);
        connection.send(Packet.login(username, password));
    }

    /** Connexion au serveur puis envoi du Packet REGISTER. */
    @FXML
    private void onRegister() {
        String host     = serverIpField.getText().trim();
        String username = registerUsernameField.getText().trim();
        String password = registerPasswordField.getText();
        User.Role role  = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            showStatus("❌ Veuillez remplir tous les champs.", true);
            return;
        }

        connectIfNeeded(host);
        connection.send(Packet.register(username, password, role));
    }

    /** Connecte au serveur si pas encore connecté. */
    private void connectIfNeeded(String host) {
        if (!connection.isConnected()) {
            try {
                connection.connect(host, 5000);
            } catch (Exception e) {
                showStatus("❌ Impossible de joindre " + host + ":5000", true);
            }
        }
    }

    /**
     * Traite les Packets reçus du serveur.
     * APPELÉ DEPUIS LE THREAD RÉSEAU → Platform.runLater() obligatoire
     * pour modifier l'UI depuis le thread JavaFX.
     */
    private void handleServerResponse(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                case LOGIN_SUCCESS -> openChatScreen(packet.getUser());
                case SUCCESS       -> showStatus("✅ " + packet.getStatusMessage(), false);
                case ERROR         -> showStatus("❌ " + packet.getStatusMessage(), true);
            }
        });
    }

    /** Ouvre l'écran de chat et passe l'utilisateur + la connexion. */
    private void openChatScreen(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/chat.fxml"));
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 800, 600));

            // Passe les données au ChatController
            ChatController chatController = loader.getController();
            chatController.init(user, connection);

            stage.setTitle("G2 Messagerie — " + user.getUsername());
        } catch (Exception e) {
            showStatus("❌ Erreur ouverture chat : " + e.getMessage(), true);
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: red;" : "-fx-text-fill: green;");
    }
}
```

---

### `client/resources/chat.fxml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.*?>
<?import javafx.scene.layout.*?>

<BorderPane xmlns:fx="http://javafx.com/fxml"
            fx:controller="client.ui.ChatController">

    <!-- Gauche : liste des membres connectés -->
    <left>
        <VBox spacing="5" style="-fx-padding: 10; -fx-min-width: 150;">
            <Label text="Membres connectés" style="-fx-font-weight: bold;"/>
            <ListView fx:id="membersListView" VBox.vgrow="ALWAYS"
                      onMouseClicked="#onMemberSelected"/>
        </VBox>
    </left>

    <!-- Centre : conversation -->
    <center>
        <VBox spacing="5" style="-fx-padding: 10;">
            <Label fx:id="chatWithLabel" text="Sélectionnez un membre"/>
            <ListView fx:id="messagesListView" VBox.vgrow="ALWAYS"/>
            <HBox spacing="5">
                <TextArea fx:id="messageInput" promptText="Votre message..."
                          HBox.hgrow="ALWAYS" prefRowCount="2" wrapText="true"/>
                <VBox spacing="5">
                    <Button text="Envoyer" onAction="#onSend" maxWidth="Infinity"/>
                    <Button text="Déconnexion" onAction="#onLogout" maxWidth="Infinity"/>
                </VBox>
            </HBox>
            <Label fx:id="errorLabel" text="" style="-fx-text-fill: red;"/>
        </VBox>
    </center>
</BorderPane>
```

---

### `client/ui/ChatController.java`

```java
package client.ui;

import client.network.ServerConnection;
import dto.Packet;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import model.Message;
import model.User;

/**
 * Contrôleur de l'écran de messagerie.
 *
 * ObservableList : liste spéciale JavaFX.
 * Quand on ajoute/retire un élément, le ListView se met à jour
 * automatiquement — pas besoin d'appeler refresh() manuellement.
 */
public class ChatController {

    @FXML private ListView<String>  membersListView;
    @FXML private ListView<String>  messagesListView;
    @FXML private TextArea          messageInput;
    @FXML private Label             chatWithLabel;
    @FXML private Label             errorLabel;

    private User             currentUser;
    private ServerConnection connection;
    private String           selectedMember; // Destinataire actuellement sélectionné

    // ObservableList : liée au ListView, mise à jour automatique de l'UI
    private final ObservableList<String> membersList  = FXCollections.observableArrayList();
    private final ObservableList<String> messagesList = FXCollections.observableArrayList();

    /**
     * Appelé par LoginController après l'ouverture de cet écran.
     * Remplace le initialize() car on a besoin de données externes.
     */
    public void init(User user, ServerConnection connection) {
        this.currentUser = user;
        this.connection  = connection;

        // Rebranche le callback vers CE controller
        connection.setOnPacketReceived(this::handleServerResponse);

        membersListView.setItems(membersList);
        messagesListView.setItems(messagesList);
    }

    /** Sélection d'un membre dans la liste → charge la conversation. */
    @FXML
    private void onMemberSelected() {
        String selected = membersListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals(currentUser.getUsername())) return;

        selectedMember = selected;
        chatWithLabel.setText("Conversation avec " + selectedMember);
        messagesList.clear();
        // TODO Jour 8 : charger l'historique depuis le serveur (GET_HISTORY)
    }

    /** Envoi d'un message (RG7). */
    @FXML
    private void onSend() {
        if (selectedMember == null) {
            showError("Sélectionnez un destinataire.");
            return;
        }

        String contenu = messageInput.getText().trim();

        // RG7 : validations côté client (le serveur valide aussi)
        if (contenu.isEmpty()) {
            showError("Le message ne peut pas être vide.");
            return;
        }
        if (contenu.length() > 1000) {
            showError("Message trop long (max 1000 caractères).");
            return;
        }

        connection.send(Packet.sendMessage(selectedMember, contenu));
        // Affiche le message dans la conversation locale
        messagesList.add("Moi → " + selectedMember + " : " + contenu);
        messageInput.clear();
        errorLabel.setText("");
    }

    /** Déconnexion propre (RG4). */
    @FXML
    private void onLogout() {
        connection.send(Packet.logout());
        connection.disconnect();
        // Retour à l'écran de login
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/login.fxml"));
            javafx.stage.Stage stage =
                (javafx.stage.Stage) messageInput.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(loader.load(), 400, 350));
            stage.setTitle("G2 — Connexion");
        } catch (Exception e) {
            showError("Erreur retour login : " + e.getMessage());
        }
    }

    /**
     * Traite les Packets reçus du serveur.
     * Platform.runLater() → modifications UI depuis le thread JavaFX.
     */
    private void handleServerResponse(Packet packet) {
        Platform.runLater(() -> {
            switch (packet.getType()) {
                // Nouveau message reçu
                case INCOMING_MSG -> {
                    Message msg = packet.getMessage();
                    String sender = msg.getSender().getUsername();
                    messagesList.add(sender + " : " + msg.getContenu());
                    // Scroll automatique vers le bas
                    messagesListView.scrollTo(messagesList.size() - 1);
                }

                // Un utilisateur s'est connecté → ajoute à la liste
                case USER_CONNECTED -> {
                    String username = packet.getUsername();
                    if (!membersList.contains(username)) {
                        membersList.add(username);
                    }
                }

                // Un utilisateur s'est déconnecté → retire de la liste
                case USER_DISCONNECTED ->
                    membersList.remove(packet.getUsername());

                // RG10 : connexion perdue
                case ERROR ->
                    showError(packet.getStatusMessage());
            }
        });
    }

    private void showError(String msg) {
        errorLabel.setText("❌ " + msg);
    }
}
```

---

### `client/ClientApp.java`

```java
package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application client JavaFX.
 * Application.launch() démarre le moteur JavaFX et appelle start().
 */
public class ClientApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/login.fxml"));
        primaryStage.setScene(new Scene(loader.load(), 400, 350));
        primaryStage.setTitle("G2 — Connexion");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

---

### Test des Jours 6-7

Lance serveur + 2 clients JavaFX :

| # | Action | Résultat attendu |
|---|--------|-----------------|
| 1 | S'inscrire "alice" MEMBRE | "✅ Inscription réussie !" |
| 2 | S'inscrire "charlie" MEMBRE | "✅ Inscription réussie !" |
| 3 | Connexion alice | Écran chat s'ouvre |
| 4 | Connexion charlie | Écran chat s'ouvre |
| 5 | alice voit charlie dans la liste | ✅ RG4 |
| 6 | alice envoie "Bonjour !" à charlie | Message apparaît chez charlie |
| 7 | charlie répond "Salut !" | Message apparaît chez alice |
| 8 | charlie se déconnecte | Disparaît de la liste d'alice |
| 9 | alice envoie message à charlie (hors ligne) | Pas d'erreur |
| 10 | charlie se reconnecte | Reçoit le message ✅ RG6 |
| 11 | Tenter message vide | ❌ Erreur affichée |
| 12 | Couper le serveur | ❌ "Connexion perdue" ✅ RG10 |

**Checklist Jours 6-7 :**
- [ ] UI s'affiche sans exception
- [ ] Inscription et connexion via l'UI
- [ ] Liste connectés mise à jour en temps réel
- [ ] Messages reçus en temps réel
- [ ] RG6 : livraison différée fonctionne
- [ ] RG7 : message vide/trop long bloqué
- [ ] RG10 : erreur réseau affichée

---

## 🔗 JOUR 8 — Intégration complète

### Test 8.1 — 4 clients simultanés

Lance serveur + 4 clients, chacun avec un user différent, tous s'envoient des messages.
Résultat attendu : aucun crash, tous les messages arrivés.

### Test 8.2 — Redémarrage serveur

1. 2 clients connectés
2. Couper serveur (`Ctrl+C`) → clients affichent "Connexion perdue" ✅ RG10
3. Redémarrer serveur → clients se reconnectent manuellement
4. Historique toujours disponible (BDD intacte) ✅

### Test 8.3 — RG13

Compte ORGANISATEUR → voit tous les membres inscrits (pas seulement connectés).

---

## 📝 JOUR 9 — Journalisation + Test LAN

### Vérification `server.log`

```bash
# Surveille les logs en temps réel pendant les tests
tail -f server.log          # Linux/Mac
Get-Content server.log -Wait  # Windows PowerShell
```

Format attendu :
```
[2024-01-15 10:00:00] Serveur démarré sur le port 5000
[2024-01-15 10:00:00] IP locale : 192.168.1.10
[2024-01-15 10:01:02] Connexion : alice
[2024-01-15 10:01:10] alice → charlie : Bonjour !
[2024-01-15 10:02:00] Déconnexion : charlie
```

### Test LAN réel

```bash
# Machine A (serveur) : récupère l'IP
ip a           # Linux/Mac
ipconfig       # Windows
# → note l'IP type 192.168.x.x

# Pare-feu machine A :
sudo ufw allow 5000/tcp   # Linux
# Windows : Pare-feu → Règles entrantes → Nouveau → Port 5000 TCP

# Machine B (client) :
# Modifier config.properties : server.host=192.168.x.x
# Lancer ClientApp.java → se connecter → envoyer un message ✅
```

---

## ✅ JOUR 10 — Checklist finale

**Règles de gestion :**
- [ ] RG1 — Username unique
- [ ] RG2 — Auth requise pour envoyer
- [ ] RG3 — Une seule session par user
- [ ] RG4 — Statut ONLINE/OFFLINE mis à jour
- [ ] RG5 — Destinataire doit exister
- [ ] RG6 — Livraison différée hors-ligne
- [ ] RG7 — Message non vide et ≤ 1000 caractères
- [ ] RG8 — Historique chronologique
- [ ] RG9 — Mots de passe hachés (BCrypt)
- [ ] RG10 — Erreur réseau gérée côté client
- [ ] RG11 — 1 thread par client côté serveur
- [ ] RG12 — Journalisation dans server.log
- [ ] RG13 — ORGANISATEUR voit tous les membres

**Qualité :**
- [ ] Aucune exception non gérée en console
- [ ] Fonctionne sur localhost
- [ ] Fonctionne en LAN (2 machines)
- [ ] Serveur stable sur durée prolongée

---

## 🚀 Commandes Maven

```bash
# Depuis la racine du projet

# Compiler tout
mvn compile

# Lancer le serveur
mvn exec:java -pl server -Dexec.mainClass="server.Server"

# Lancer le client
mvn exec:java -pl client -Dexec.mainClass="client.ClientApp"

# Compiler + package (crée les .jar)
mvn package

# Nettoyer les fichiers compilés
mvn clean
```

---

## 📚 Ressources

| Sujet | Lien |
|-------|------|
| Java Sockets | https://docs.oracle.com/en/java/tutorial/networking/sockets/ |
| Hibernate 5 (javax) | https://hibernate.org/orm/documentation/5.6/ |
| JPA / JPQL | https://www.baeldung.com/jpql-hql |
| JavaFX + FXML | https://openjfx.io/openjfx-docs/ |
| JavaFX Platform.runLater | https://www.baeldung.com/javafx |
| Maven Multi-Module | https://maven.apache.org/guides/mini/guide-multiple-modules.html |
| BCrypt Java | https://www.baeldung.com/java-password-hashing |
| PostgreSQL JDBC | https://jdbc.postgresql.org/documentation/ |
| Sockets Baeldung | https://www.baeldung.com/a-guide-to-java-sockets |
