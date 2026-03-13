package com.g2.model;


import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password; // Mot de passe haché (BCrypt)

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "datecreation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "sender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messagesSent;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messagesReceived;

    // -------------------------
    // Enums
    // -------------------------

    public enum Role {
        ORGANISATEUR, MEMBRE, BENEVOLE
    }

    public enum Status {
        ONLINE, OFFLINE
    }

    public User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = Status.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    // -------------------------
    // Lifecycle JPA
    // -------------------------

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = Status.OFFLINE;
        }
    }
}