module BattleShipsJavaFx {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.sql;
    requires org.postgresql.jdbc;

    opens application to javafx.graphics;
    opens model to javafx.base;
    opens Images;

    exports application;
    exports model;
}