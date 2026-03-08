package model;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Représente un message entre deux utilisateurs.
 * Mappée à la table "messages".
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clé étrangère vers users (expéditeur)
    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // Clé étrangère vers users (destinataire)
    @ManyToOne
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // Message max 1000 caractères : RG7
    @Column(nullable = false, length = 1000)
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ENVOYE;

    public enum Statut { ENVOYE, RECU, LU }

    // Getters et Setters
    public Long getId() { return id; }
    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }
    public User getReceiver() { return receiver; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }
}