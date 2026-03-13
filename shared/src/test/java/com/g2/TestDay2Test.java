package com.g2;

import com.g2.dao.MessageDAO;
import com.g2.dao.UserDAO;
import com.g2.model.Message;
import com.g2.model.User;
import com.g2.util.HibernateUtil;
import com.g2.util.PasswordUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestDay2Test {

    private static UserDAO userDAO;
    private static MessageDAO msgDAO;

    @BeforeAll
    public static void setup() {
        userDAO = new UserDAO();
        msgDAO = new MessageDAO();
        // drop legacy column and clear tables to get a clean database
        EntityManager em = HibernateUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("ALTER TABLE messages DROP COLUMN IF EXISTS dateenvoi").executeUpdate();
            em.createNativeQuery("DELETE FROM messages").executeUpdate();
            em.createNativeQuery("DELETE FROM users").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }

        // create a second user "alice" representing day1 setup
        User alice = new User("alice", PasswordUtil.hash("foo"), User.Role.MEMBRE);
        userDAO.save(alice);
    }

    @AfterAll
    public static void tearDown() {
        // close any resources if necessary (HibernateUtil does statics)
    }

    @Test
    public void userRegistrationAndLookup() {
        User dudu = new User();
        dudu.setUsername("dudu");
        dudu.setPassword(PasswordUtil.hash("motdepasse233"));
        dudu.setRole(User.Role.BENEVOLE);
        userDAO.save(dudu);

        User found = userDAO.findByUsername("dudu");
        assertNotNull(found);
        assertEquals("dudu", found.getUsername());
        assertEquals(User.Role.BENEVOLE, found.getRole());

        assertNull(userDAO.findByUsername("xxx"));
    }

    @Test
    public void duplicateUserIsRejected() {
        User u = new User("dudu", PasswordUtil.hash("bar"), User.Role.MEMBRE);
        Exception exception = assertThrows(Exception.class, () -> userDAO.save(u));
        String msg = exception.getMessage();
        assertTrue(msg.contains("ConstraintViolationException") || msg.contains("duplicate"));
    }

    @Test
    public void bcryptHashingWorks() {
        String h1 = PasswordUtil.hash("secret");
        String h2 = PasswordUtil.hash("secret");
        assertNotEquals(h1, h2); // salted
        assertTrue(PasswordUtil.verify("secret", h1));
        assertFalse(PasswordUtil.verify("wrong", h1));
    }

    @Test
    public void messageFlow() {
        User alice = userDAO.findByUsername("alice");
        assertNotNull(alice);
        User dudu = userDAO.findByUsername("dudu");
        assertNotNull(dudu);

        Message msg = new Message(alice, dudu, "Salut dudu !");
        msgDAO.save(msg);
        assertNotNull(msg.getId());
        assertNotNull(msg.getDateEnvoi());
        assertEquals(Message.Statut.ENVOYE, msg.getStatut());

        List<Message> conv = msgDAO.findConversation(alice, dudu);
        assertEquals(1, conv.size());
        Message m = conv.get(0);
        assertEquals("Salut dudu !", m.getContenu());
        assertEquals(alice.getUsername(), m.getSender().getUsername());
        assertEquals(Message.Statut.ENVOYE, m.getStatut());
    }
}