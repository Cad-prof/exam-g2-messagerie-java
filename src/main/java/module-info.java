module com.g2.g2messagerie {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.g2.g2messagerie to javafx.fxml;
    exports com.g2.g2messagerie;
    // Hibernate & JPA
    requires javax.persistence;
    requires org.hibernate.orm.core;

    // BCrypt
    requires jbcrypt;

    // Logging Java
    requires java.logging;
    requires static lombok;

    // Nécessaire pour Hibernate (réflexion sur les entités)
    opens com.g2.model.entity to org.hibernate.orm.core, javafx.base;
    opens com.g2.dao to org.hibernate.orm.core;


    // Export des packages principaux
    exports com.g2;
    exports com.g2.entity;
    exports com.g2.dao;
    exports com.g2.server;
}