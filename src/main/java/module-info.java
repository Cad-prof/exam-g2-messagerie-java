module com.g2.g2messagerie {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.g2.g2messagerie to javafx.fxml;
    exports com.g2.g2messagerie;
}