module com.g2.g2messagerie {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;

    // Hibernate & JPA
    requires javax.persistence;
    requires org.hibernate.orm.core;

    // BCrypt
    requires jbcrypt;

    // Logging
    requires java.logging;

    // Hibernate doit accéder aux entités par réflexion
    opens com.g2.shared to org.hibernate.orm.core, javafx.base;
    opens com.g2.dao     to org.hibernate.orm.core;

    // FXMLLoader doit accéder aux contrôleurs
    opens com.g2.server.contoller to javafx.fxml;

    // Ressources FXML (le package où sont les .fxml)
    opens com.g2 to javafx.fxml;

    // Exports
    exports com.g2;
    exports com.g2.shared;
    exports com.g2.dao;
    exports com.g2.server;
    exports com.g2.server.contoller;
    exports com.g2.network;
}