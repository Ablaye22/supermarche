package com.supermarche.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

/**
 * Charge la configuration applicative depuis application.yml.
 * Chaque valeur peut etre surchargee par une variable d'environnement
 * pour eviter de stocker des secrets (mot de passe BD) en clair dans le
 * fichier versionne : ex. DB_PASSWORD surcharge database.password.
 */
public final class AppConfig {

    private static final AppConfig INSTANCE = new AppConfig();

    private final Map<String, Object> racine;

    // @SuppressWarnings("unchecked")
    private AppConfig() {
        try (InputStream flux = AppConfig.class.getClassLoader()
                .getResourceAsStream("config/application.yml")) {
            if (flux == null) {
                throw new IllegalStateException("Fichier config/application.yml introuvable dans le classpath");
            }
            Yaml yaml = new Yaml();
            this.racine = yaml.load(flux);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger la configuration applicative", e);
        }
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sousSection(String cle) {
        Object valeur = racine.get(cle);
        if (valeur instanceof Map) {
            return (Map<String, Object>) valeur;
        }
        throw new IllegalStateException("Section de configuration manquante : " + cle);
    }

    /**
     * Recherche une valeur en priorisant la variable d'environnement,
     * puis a defaut, la valeur du fichier YAML.
     */
    private String valeur(String envVar, Map<String, Object> section, String cleYaml) {
        String depuisEnv = System.getenv(envVar);
        if (depuisEnv != null && !depuisEnv.isBlank()) {
            return depuisEnv;
        }
        Object v = section.get(cleYaml);
        return v == null ? null : v.toString();
    }

    // ----------------------- Base de donnees -----------------------

    public String getDbHost() {
        return valeur("DB_HOST", sousSection("database"), "host");
    }

    public int getDbPort() {
        return Integer.parseInt(valeur("DB_PORT", sousSection("database"), "port"));
    }

    public String getDbName() {
        return valeur("DB_NAME", sousSection("database"), "name");
    }

    public String getDbUser() {
        return valeur("DB_USER", sousSection("database"), "user");
    }

    public String getDbPassword() {
        return valeur("DB_PASSWORD", sousSection("database"), "password");
    }

    @SuppressWarnings("unchecked")
    public int getDbPoolMaxSize() {
        Map<String, Object> pool = (Map<String, Object>) sousSection("database").get("pool");
        return (int) pool.get("maxSize");
    }

    @SuppressWarnings("unchecked")
    public int getDbPoolMinIdle() {
        Map<String, Object> pool = (Map<String, Object>) sousSection("database").get("pool");
        return (int) pool.get("minIdle");
    }

    @SuppressWarnings("unchecked")
    public long getDbConnectionTimeoutMs() {
        Map<String, Object> pool = (Map<String, Object>) sousSection("database").get("pool");
        return Long.parseLong(pool.get("connectionTimeoutMs").toString());
    }

    // ----------------------- Securite -----------------------

    public int getBcryptStrength() {
        return (int) sousSection("security").get("bcryptStrength");
    }

    public int getMaxLoginAttempts() {
        return (int) sousSection("security").get("maxLoginAttempts");
    }

    public int getLockoutDurationMinutes() {
        return (int) sousSection("security").get("lockoutDurationMinutes");
    }

    // ----------------------- Application -----------------------

    public String getNomEntreprise() {
        return sousSection("application").get("nomEntreprise").toString();
    }

    public String getDevise() {
        return sousSection("application").get("devise").toString();
    }

    public String getDossierPhotos() {
        
        return  "/home/ablaye/Projects/supermarche/supermarche/src/main/resources/images/produits" ;
    }
}