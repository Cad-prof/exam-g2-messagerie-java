package com.g2.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
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

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private Statut statut;

    @Column(name = "date_envoi", nullable = false, updatable = false)
    private LocalDateTime dateEnvoi;

    public enum Statut { ENVOYE, RECU, LU }

    public Message() {}

    public Message(User sender, User receiver, String contenu) {
        this.sender   = sender;
        this.receiver = receiver;
        this.contenu  = contenu;
        this.statut   = Statut.ENVOYE;
        this.dateEnvoi = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateEnvoi == null) this.dateEnvoi = LocalDateTime.now();
        if (this.statut == null)    this.statut    = Statut.ENVOYE;
    }

    public Long          getId()              { return id; }
    public void          setId(Long id)       { this.id = id; }

    public User          getSender()          { return sender; }
    public void          setSender(User s)    { this.sender = s; }

    public User          getReceiver()        { return receiver; }
    public void          setReceiver(User r)  { this.receiver = r; }

    public String        getContenu()         { return contenu; }
    public void          setContenu(String c) { this.contenu = c; }

    public Statut        getStatut()          { return statut; }
    public void          setStatut(Statut s)  { this.statut = s; }

    public LocalDateTime getDateEnvoi()              { return dateEnvoi; }
    public void          setDateEnvoi(LocalDateTime d){ this.dateEnvoi = d; }
}