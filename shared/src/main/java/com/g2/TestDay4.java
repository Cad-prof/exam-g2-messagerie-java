package com.g2;// TestDay4.java
import com.g2.dto.Packet;
import com.g2.model.User;
import java.io.*;

public class TestDay4 {
    public static void main(String[] args) throws Exception {

        // Test : sérialisation → fichier → désérialisation
        Packet original = Packet.register("carol", "pass123", User.Role.MEMBRE);

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("test_packet.bin"))) {
            oos.writeObject(original);
            System.out.println("Packet sérialisé");
        }

        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream("test_packet.bin"))) {
            Packet restored = (Packet) ois.readObject();
            System.out.println("Type     : " + restored.getType());
            System.out.println("Username : " + restored.getUsername());
            System.out.println("Role     : " + restored.getRole());
            System.out.println("Timestamp: " + restored.getTimestamp());
        }

        // Test : tous les types
        System.out.println("--- Types ---");
        System.out.println(Packet.login("u","p").getType());              // LOGIN
        System.out.println(Packet.error("oops").getType());              // ERROR
        System.out.println(Packet.success("ok").getType());              // SUCCESS
        System.out.println(Packet.sendMessage("bob","hi").getType());    // SEND_MSG
        System.out.println(Packet.logout().getType());                   // LOGOUT
        System.out.println(Packet.userConnected("alice").getType());     // USER_CONNECTED

        // Nettoyage
        new File("test_packet.bin").delete();
    }
}