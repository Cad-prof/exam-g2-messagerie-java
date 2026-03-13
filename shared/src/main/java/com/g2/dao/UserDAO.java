package com.g2.dao;

import com.g2.model.User;
import com.g2.util.HibernateUtil;


import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;
import java.util.logging.Logger;

public class UserDAO {

    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());

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
        } finally { em.close(); }
    }

    public void update(User user) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(user);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally { em.close(); }
    }

    public User findById(Long id) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.find(User.class, id);
        } finally { em.close(); }
    }

    public User findByUsername(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally { em.close(); }
    }

    // Tous les utilisateurs inscrits — RG13 (ORGANISATEUR)
    public List<User> findAll() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u ORDER BY u.role, u.username", User.class)
                    .getResultList();
        } finally { em.close(); }
    }

    // Tous les utilisateurs sauf soi-même — pour les contacts
    public List<User> findAllExcept(String username) {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.username <> :username ORDER BY u.username", User.class)
                    .setParameter("username", username)
                    .getResultList();
        } finally { em.close(); }
    }

    // Utilisateurs ONLINE
    public List<User> findOnlineUsers() {
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.status = :status ORDER BY u.username", User.class)
                    .setParameter("status", User.Status.ONLINE)
                    .getResultList();
        } finally { em.close(); }
    }

    // Remettre tout le monde OFFLINE au démarrage — RG4
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
            throw e;
        } finally { em.close(); }
    }
}