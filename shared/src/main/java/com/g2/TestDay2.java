package com.g2;// TestDay2.java
import com.g2.dao.MessageDAO;
import com.g2.dao.UserDAO;
import com.g2.model.Message;
import com.g2.model.User;
import com.g2.util.PasswordUtil;
import java.util.List;

public class TestDay2 {
    public static void main(String[] args) {

        UserDAO    userDAO = new UserDAO();
        MessageDAO msgDAO  = new MessageDAO();
        
        // ensure schema clean: previous versions left old column `dateenvoi` that
        // conflicts with current mapping. drop it if present so inserts succeed.
        javax.persistence.EntityManager ddlEm = com.g2.util.HibernateUtil.getEntityManager();
        try {
            ddlEm.getTransaction().begin();
            ddlEm.createNativeQuery("ALTER TABLE messages DROP COLUMN IF EXISTS dateenvoi").executeUpdate();
            ddlEm.getTransaction().commit();
            System.out.println("Removed legacy column dateenvoi (if existed)");
        } catch (Exception e) {
            if (ddlEm.getTransaction().isActive()) ddlEm.getTransaction().rollback();
            // ignore; maybe table doesn't exist yet
        } finally {
            ddlEm.close();
        }

        // --- Test 2.1 : inscription ---
        User dudu = new User();
        dudu.setUsername("dudu");
        dudu.setPassword(PasswordUtil.hash("motdepasse233"));
        dudu.setRole(User.Role.BENEVOLE);
        try {
            userDAO.save(dudu);
            System.out.println("dudu inscrit !");
        } catch (Exception e) {
            System.out.println("dudu déjà présent, on continue");
        }

        // --- Test 2.2 : recherche ---
        User found = userDAO.findByUsername("dudu");
        System.out.println("Trouvé : " + found.getUsername() + " / " + found.getRole());
        System.out.println("Introuvable retourne null : " + (userDAO.findByUsername("xxx") == null));

        // --- Test 2.3 : RG1 doublon ---
        try {
            User doublon = new User();
            doublon.setUsername("dudu");
            doublon.setPassword(PasswordUtil.hash("autre"));
            doublon.setRole(User.Role.MEMBRE);
            userDAO.save(doublon);
            System.out.println("Doublon accepté — RG1 ECHOUEE !");
        } catch (Exception e) {
            System.out.println("RG1 OK : doublon refusé");
        }

        // --- Test 2.4 : BCrypt ---
        String h1 = PasswordUtil.hash("secret");
        String h2 = PasswordUtil.hash("secret");
        System.out.println("Hashs différents (sel) : " + !h1.equals(h2));
        System.out.println("Vérif correcte : " + PasswordUtil.verify("secret", h1));
        System.out.println("Vérif fausse   : " + !PasswordUtil.verify("wrong", h1));

        // --- Test 2.5 : messages ---
        // dump table metadata so we can see column order/type and verify mapping
        javax.persistence.EntityManager em = null;
        try {
            em = com.g2.util.HibernateUtil.getEntityManager();
            org.hibernate.Session session = em.unwrap(org.hibernate.Session.class);
            session.doWork(conn -> {
                java.sql.ResultSet rs = conn.getMetaData().getColumns(null, null, "messages", null);
                System.out.println("=== messages columns ===");
                while (rs.next()) {
                    System.out.printf("%s %s pos=%d default=%s nullable=%s\n",
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getInt("ORDINAL_POSITION"),
                            rs.getString("COLUMN_DEF"),
                            rs.getString("IS_NULLABLE"));
                }
                rs.close();
            });
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (em != null) em.close();
        }

        User alice = userDAO.findByUsername("alice"); // inséré Jour 1
        // make sure we use managed dudu instance
        dudu = userDAO.findByUsername("dudu");
        Message msg = new Message(alice, dudu, "Salut dudu !");
        System.out.println("(debug) message avant save dateEnvoi=" + msg.getDateEnvoi());
        msgDAO.save(msg);
        System.out.println("Message sauvegardé ID : " + msg.getId());

        List<Message> conv = msgDAO.findConversation(alice, dudu);
        System.out.println("Conversation : " + conv.size() + " message(s)");
        conv.forEach(m -> System.out.println("  → " + m.getSender().getUsername()
                + " : " + m.getContenu() + " [" + m.getStatut() + "]"));
    }
}