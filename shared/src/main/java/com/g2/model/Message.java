package com.g2.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Data
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(name = "contenu", nullable = false, length = 1000)
    private String contenu;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private Statut statut;


    public enum Statut {
        ENVOYE, RECU, LU
    }

    // -------------------------
    // Constructeurs


    public Message() {
    }

    public Message(User sender, User receiver, String contenu) {
        this.sender = sender;
        this.receiver = receiver;
        this.contenu = contenu;
        this.dateEnvoi = LocalDateTime.now();
        this.statut = Statut.ENVOYE;
    }

    // -------------------------
    // Lifecycle JPA
    // -------------------------

    @PrePersist
    protected void onCreate() {
        if (this.dateEnvoi == null) {
            this.dateEnvoi = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = Statut.ENVOYE;
        }
    }
}