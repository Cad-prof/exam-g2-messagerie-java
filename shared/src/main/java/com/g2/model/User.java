package com.g2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Représente un membre de l'association.
 * Cette classe est mappée à la table "users" en base de données.
 */
@Entity
@Table(name = "users")
public class User {

    // Clé primaire auto-incrémentée
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // username UNIQUE : RG1
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    // Mot de passe haché : RG9
    @Column(nullable = false)
    private String password;

    // Rôle parmi 3 valeurs fixes → @Enumerated stocke le nom ("MEMBRE", etc.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Statut en ligne / hors ligne : RG4
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.OFFLINE;

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Énumérations définies dans la même classe pour simplicité
    public enum Role { ORGANISATEUR, MEMBRE, BENEVOLE }
    public enum Status { ONLINE, OFFLINE }

    // Getters et Setters (obligatoires pour Hibernate)
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public LocalDateTime getDateCreation() { return dateCreation; }
}