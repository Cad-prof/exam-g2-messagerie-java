package com.g2;

import com.g2.model.User;
import com.g2.util.HibernateUtil;

import javax.persistence.EntityManager;

public class TestDay1 {

    public static void main(String[] args) {

        // récupération EntityManager via HibernateUtil
        EntityManager em = HibernateUtil.getEntityManager();

        try {

            // INSERT
            em.getTransaction().begin();

            User user = new User();
            user.setUsername("alice");
            user.setPassword("hash_temporaire"); // BCrypt plus tard
            user.setRole(User.Role.MEMBRE);

            em.persist(user);

            em.getTransaction().commit();

            System.out.println(" User inséré avec ID : " + user.getId());

            // SELECT
            User found = em.find(User.class, user.getId());

            System.out.println(" User retrouvé : " + found.getUsername());

        } catch (Exception e) {

            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            e.printStackTrace();

        } finally {

            em.close();
            HibernateUtil.close();

        }
    }
}