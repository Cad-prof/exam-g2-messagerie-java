package com.g2.dao;


import com.g2.util.HibernateUtil;
import com.g2.model.User;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.logging.Logger;

/**
 * DAO pour l'entité User.
 * Fournit les opérations CRUD et les requêtes métier.
 */
public class UserDAO {

    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

    // -------------------------
    // Créer un utilisateur
    // -------------------------

    public void save(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            logger.info("Utilisateur sauvegardé : " + user.getUsername());
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur save User : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Mettre à jour un utilisateur
    // -------------------------

    public void update(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur update User : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Supprimer un utilisateur
    // -------------------------

    public void delete(Long id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
                logger.info("Utilisateur supprimé : id=" + id);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur delete User : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Trouver par ID
    // -------------------------

    public User findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.find(User.class, id);
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Trouver par username (RG1 — unicité)
    // -------------------------

    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class
            );
            query.setParameter("username", username);
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // Utilisateur inexistant
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Lister tous les utilisateurs (RG13)
    // -------------------------

    public List<User> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u ORDER BY u.username", User.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Lister les utilisateurs connectés
    // -------------------------

    public List<User> findOnlineUsers() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.status = :status ORDER BY u.username", User.class
                    ).setParameter("status", User.Status.ONLINE)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // -------------------------
    // Passer tous les utilisateurs OFFLINE
    // (utile au redémarrage du serveur — RG4)
    // -------------------------

    public void resetAllToOffline() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE User u SET u.status = :status")
                    .setParameter("status", User.Status.OFFLINE)
                    .executeUpdate();
            em.getTransaction().commit();
            logger.info("Tous les utilisateurs remis OFFLINE.");
        } catch (Exception e) {
            em.getTransaction().rollback();
            logger.severe("Erreur resetAllToOffline : " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }
}
