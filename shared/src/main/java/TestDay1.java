import model.User;

import util.HibernateUtil;
import javax.persistence.EntityManager;

public class TestDay1 {
    public static void main(String[] args) {

        EntityManager em;
        em = HibernateUtil.getFactory().createEntityManager();

        // Si aucune exception → Hibernate s'est connecté à PostgreSQL
        System.out.println(" Connexion à la base réussie !");

        em.close();
        HibernateUtil.close();
    }
}