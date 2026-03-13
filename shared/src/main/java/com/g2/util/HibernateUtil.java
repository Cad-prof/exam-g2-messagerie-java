package com.g2.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Singleton qui fournit l'EntityManagerFactory.
 * On ne crée la factory qu'une seule fois (coûteux), puis on la réutilise.
 */
public class HibernateUtil {

    /**
     * Utilitaire Hibernate — fournit un EntityManagerFactory unique (singleton).
     * Le nom "messageriePU" doit correspondre à celui dans persistence.xml.
     */

    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("messageriePU");

    private HibernateUtil() {}

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}