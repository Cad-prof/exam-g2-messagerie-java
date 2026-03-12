package com.g2.util;

import org.mindrot.jbcrypt.BCrypt;
public class PasswordUtil {
    /**
     * Utilitaire pour le hachage et la vérification des mots de passe (RG9).
     * Utilise BCrypt via la dépendance : org.mindrot:jbcrypt:0.4
     */

    private PasswordUtil() {}

    /**
     * Hache un mot de passe en clair avec BCrypt.
     */
    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    /**
     * Vérifie si un mot de passe en clair correspond au hash stocké.
     */
    public static boolean verify(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}
