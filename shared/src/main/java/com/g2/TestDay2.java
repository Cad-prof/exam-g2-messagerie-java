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

        // --- Test 2.1 : inscription ---
        User bob = new User();
        bob.setUsername("bob");
        bob.setPassword(PasswordUtil.hash("motdepasse123"));
        bob.setRole(User.Role.ORGANISATEUR);
        userDAO.save(bob);
        System.out.println("bob inscrit !");

        // --- Test 2.2 : recherche ---
        User found = userDAO.findByUsername("bob");
        System.out.println("Trouvé : " + found.getUsername() + " / " + found.getRole());
        System.out.println("Introuvable retourne null : " + (userDAO.findByUsername("xxx") == null));

        // --- Test 2.3 : RG1 doublon ---
        try {
            User doublon = new User();
            doublon.setUsername("bob");
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
        User alice = userDAO.findByUsername("alice"); // inséré Jour 1
        Message msg = new Message();
        msg.setSender(alice);
        msg.setReceiver(bob);
        msg.setContenu("Salut Bob !");
        msgDAO.save(msg);
        System.out.println("Message sauvegardé ID : " + msg.getId());

        List<Message> conv = msgDAO.findConversation(alice, bob);
        System.out.println("Conversation : " + conv.size() + " message(s)");
        conv.forEach(m -> System.out.println("  → " + m.getSender().getUsername()
                + " : " + m.getContenu() + " [" + m.getStatut() + "]"));
    }
}