package com.g2.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "sender",   cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messagesSent;

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> messagesReceived;

    public enum Role   { ORGANISATEUR, MEMBRE, BENEVOLE }
    public enum Status { ONLINE, OFFLINE }

    public User() {}

    public User(String username, String password, Role role) {
        this.username     = username;
        this.password     = password;
        this.role         = role;
        this.status       = Status.OFFLINE;
        this.dateCreation = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) this.dateCreation = LocalDateTime.now();
        if (this.status == null)       this.status       = Status.OFFLINE;
    }

    // Getters & Setters
    public Long          getId()              { return id; }
    public void          setId(Long id)       { this.id = id; }

    public String        getUsername()        { return username; }
    public void          setUsername(String u){ this.username = u; }

    public String        getPassword()        { return password; }
    public void          setPassword(String p){ this.password = p; }

    public Role          getRole()            { return role; }
    public void          setRole(Role r)      { this.role = r; }

    public Status        getStatus()          { return status; }
    public void          setStatus(Status s)  { this.status = s; }

    public LocalDateTime getDateCreation()           { return dateCreation; }
    public void          setDateCreation(LocalDateTime d){ this.dateCreation = d; }

    public List<Message> getMessagesSent()           { return messagesSent; }
    public void          setMessagesSent(List<Message> m){ this.messagesSent = m; }

    public List<Message> getMessagesReceived()           { return messagesReceived; }
    public void          setMessagesReceived(List<Message> m){ this.messagesReceived = m; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role
                + ", status=" + status + "}";
    }
}