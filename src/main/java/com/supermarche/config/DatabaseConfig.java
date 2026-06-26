package com.supermarche.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Centralise la creation du pool de connexions JDBC vers MariaDB.
 * Un pool est indispensable ici car plusieurs caisses se connectent
 * simultanement sur le reseau local : ouvrir une connexion par requete
 * serait couteux et limiterait la concurrence.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    private static volatile HikariDataSource dataSource;

    private DatabaseConfig() {
    }

    public static synchronized HikariDataSource getDataSource() {
        if (dataSource == null) {
            AppConfig config = AppConfig.getInstance();

            HikariConfig hikariConfig = new HikariConfig();
            String jdbcUrl = String.format(
                    "jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC",
                    config.getDbHost(), config.getDbPort(), config.getDbName());

            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(config.getDbUser());
            hikariConfig.setPassword(config.getDbPassword());
            hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize());
            hikariConfig.setMinimumIdle(config.getDbPoolMinIdle());
            hikariConfig.setConnectionTimeout(config.getDbConnectionTimeoutMs());
            hikariConfig.setPoolName("supermarche-pool");

            // Validation de la connexion avant utilisation (evite les erreurs
            // si le serveur MariaDB redemarre pendant que l'appli tourne)
            hikariConfig.setConnectionTestQuery("SELECT 1");

            dataSource = new HikariDataSource(hikariConfig);
            log.info("Pool de connexions initialise vers {}:{}/{}", config.getDbHost(), config.getDbPort(), config.getDbName());
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static void fermer() {
        if (dataSource != null) {
            dataSource.close();
            log.info("Pool de connexions ferme");
        }
    }
}
