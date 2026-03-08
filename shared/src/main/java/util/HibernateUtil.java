package util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Singleton qui fournit l'EntityManagerFactory.
 * On ne crée la factory qu'une seule fois (coûteux), puis on la réutilise.
 */
public class HibernateUtil {

    // "static final" = créé une seule fois au démarrage
    private static final EntityManagerFactory FACTORY =
        Persistence.createEntityManagerFactory("messagerie-pu");

    // Méthode statique → accessible sans instancier la classe
    public static EntityManagerFactory getFactory() {
        return FACTORY;
    }

    // À appeler à la fin du programme pour libérer les ressources
    public static void close() {
        FACTORY.close();
    }
}