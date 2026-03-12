package com.g2.dao;

import com.g2.util.HibernateUtil;
import com.g2.model.Message;
import com.g2.model.User;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

/**
DAO pour l'entité Message.
        * Fournit les opérations CRUD et les requêtes métier.
        */
public class MessageDAO {

    private static final Logger logger = Logger.getLogger(MessageDAO.class.getName());

    // -------------------------
    // Sauvegarder un message
    // -------------------------

    public void save(Message message) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
            logger.info("Message sauvegardé : " + message.getSender().getUsername()
                    + " → " + message.getReceiver().getUsername());
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur save Message : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Mettre à jour un message (ex: changer le statut)
    // -------------------------

    public void update(Message message) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(message);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur update Message : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Historique de conversation entre deux utilisateurs (RG8 — ordre chronologique)
    // -------------------------

    public List<Message> findConversation(User user1, User user2) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<Message> query = em.createQuery(
                    "SELECT m FROM Message m " +
                            "JOIN FETCH m.sender " +
                            "JOIN FETCH m.receiver " +
                            "WHERE (m.sender = :user1 AND m.receiver = :user2) " +
                            "   OR (m.sender = :user2 AND m.receiver = :user1) " +
                            "ORDER BY m.dateEnvoi ASC",
                    Message.class
            );
            query.setParameter("user1", user1);
            query.setParameter("user2", user2);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Messages en attente pour un utilisateur (RG6 — statut ENVOYE)
    // -------------------------

    public List<Message> findPendingMessages(User receiver) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m " +
                                    "JOIN FETCH m.sender " +
                                    "JOIN FETCH m.receiver " +
                                    "WHERE m.receiver = :receiver AND m.statut = :statut " +
                                    "ORDER BY m.dateEnvoi ASC",
                            Message.class
                    ).setParameter("receiver", receiver)
                    .setParameter("statut", Message.Statut.ENVOYE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // Tous les messages envoyés par un utilisateur

    public List<Message> findMessagesBySender(User sender) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m WHERE m.sender = :sender ORDER BY m.dateEnvoi ASC",
                            Message.class
                    ).setParameter("sender", sender)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Tous les messages reçus par un utilisateur
    // -------------------------

    public List<Message> findMessagesByReceiver(User receiver) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT m FROM Message m WHERE m.receiver = :receiver ORDER BY m.dateEnvoi ASC",
                            Message.class
                    ).setParameter("receiver", receiver)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Marquer tous les messages d'une conversation comme LU
    // -------------------------

    public void markAsRead(User sender, User receiver) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery(
                            "UPDATE Message m SET m.statut = :statut " +
                                    "WHERE m.sender = :sender AND m.receiver = :receiver AND m.statut = :recu"
                    ).setParameter("statut", Message.Statut.LU)
                    .setParameter("sender", sender)
                    .setParameter("receiver", receiver)
                    .setParameter("recu", Message.Statut.RECU)
                    .executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur markAsRead : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }
}