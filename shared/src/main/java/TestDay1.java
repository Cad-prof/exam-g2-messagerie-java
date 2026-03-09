import model.User;

import util.HibernateUtil;
import javax.persistence.EntityManager;

public class TestDay1 {
    public static void main(String[] args) {
        
        EntityManager em = HibernateUtil.getFactory().createEntityManager();

        // INSERT
        em.getTransaction().begin();
        User user = new User();
        user.setUsername("alice");
        user.setPassword("hash_temporaire"); // pas encore BCrypt
        user.setRole(User.Role.MEMBRE);
        em.persist(user);
        em.getTransaction().commit();
        System.out.println("✅ User inséré avec ID : " + user.getId());

        // SELECT
        User found = em.find(User.class, user.getId());
        System.out.println("✅ User retrouvé : " + found.getUsername());

        em.close();
        HibernateUtil.close();
    }
}