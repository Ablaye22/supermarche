package com.supermarche.service;

import com.supermarche.dao.UtilisateurDao;
import com.supermarche.exception.AuthentificationException;
import com.supermarche.exception.AutorisationException;
import com.supermarche.model.CodeRole;
import com.supermarche.model.Employe;
import com.supermarche.model.Role;
import com.supermarche.model.Utilisateur;
import com.supermarche.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de la logique d'authentification et de controle d'acces.
 * UtilisateurDao est remplace par un mock Mockito injecte via le
 * constructeur dedie aux tests, ce qui evite toute dependance a une
 * vraie connexion MariaDB.
 */
class AuthServiceTest {

    private AuthService authService;
    private UtilisateurDao utilisateurDaoMock;

    @BeforeEach
    void setUp() {
        utilisateurDaoMock = mock(UtilisateurDao.class);
        authService = new AuthService(utilisateurDaoMock, new AuditService());
    }

    private Utilisateur creerUtilisateurTest(String motDePasseClair, CodeRole codeRole, boolean actif, boolean verrouille) {
        Utilisateur u = new Utilisateur();
        u.setId(1);
        u.setNomUtilisateur("jdupont");
        u.setMotDePasseHash(PasswordHasher.hacher(motDePasseClair));
        u.setActif(actif);
        u.setCompteVerrouille(verrouille);
        u.setTentativesEchouees(0);

        Employe employe = new Employe();
        employe.setId(1);
        employe.setNom("Dupont");
        employe.setPrenom("Jean");
        u.setEmploye(employe);

        Role role = new Role(1, codeRole, codeRole.name(), null);
        role.setPermissions(Set.of("VENTE_CREER", "CAISSE_OUVRIR_FERMER"));
        u.setRole(role);

        return u;
    }

    @Test
    void connexionReussieAvecBonIdentifiantEtMotDePasse() {
        Utilisateur attendu = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(attendu));

        Utilisateur resultat = authService.seConnecter("jdupont", "MotDePasse123");

        assertNotNull(resultat);
        assertEquals("jdupont", resultat.getNomUtilisateur());
        assertTrue(authService.estConnecte());
        verify(utilisateurDaoMock).enregistrerConnexionReussie(1);
    }

    @Test
    void connexionEchoueAvecMauvaisMotDePasse() {
        Utilisateur utilisateur = creerUtilisateurTest("BonMotDePasse1", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));

        AuthentificationException exception = assertThrows(AuthentificationException.class,
                () -> authService.seConnecter("jdupont", "MauvaisMotDePasse"));

        assertFalse(authService.estConnecte());
        // Le message ne doit jamais reveler si c'est le login ou le mot de passe qui est en cause.
        assertEquals("Identifiant ou mot de passe incorrect.", exception.getMessage());
        verify(utilisateurDaoMock).incrementerTentativesEchouees(eq(1), anyInt());
    }

    @Test
    void connexionEchoueAvecNomUtilisateurInconnuMemeMessageQueMauvaisMotDePasse() {
        when(utilisateurDaoMock.trouverParNomUtilisateur("inconnu")).thenReturn(Optional.empty());

        AuthentificationException exception = assertThrows(AuthentificationException.class,
                () -> authService.seConnecter("inconnu", "PeuImporte123"));

        assertEquals("Identifiant ou mot de passe incorrect.", exception.getMessage());
    }

    @Test
    void connexionRefuseeSiCompteVerrouille() {
        Utilisateur utilisateur = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, true, true);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));

        AuthentificationException exception = assertThrows(AuthentificationException.class,
                () -> authService.seConnecter("jdupont", "MotDePasse123"));

        assertTrue(exception.getMessage().toLowerCase().contains("verrouill"));
        // Le mot de passe ne doit meme pas etre verifie sur un compte verrouille : aucune tentative ne doit etre comptee.
        verify(utilisateurDaoMock, never()).incrementerTentativesEchouees(anyInt(), anyInt());
    }

    @Test
    void connexionRefuseeSiCompteInactif() {
        Utilisateur utilisateur = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, false, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));

        assertThrows(AuthentificationException.class, () -> authService.seConnecter("jdupont", "MotDePasse123"));
    }

    @Test
    void exigerPermissionEchoueSiAucunUtilisateurConnecte() {
        assertThrows(AuthentificationException.class, () -> authService.exigerPermission("VENTE_CREER"));
    }

    @Test
    void exigerPermissionEchoueSiUtilisateurNePossedePasLaPermission() {
        Utilisateur utilisateur = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "MotDePasse123");

        // Le role CAISSIER de test ne possede pas UTILISATEUR_GERER.
        assertThrows(AutorisationException.class, () -> authService.exigerPermission("UTILISATEUR_GERER"));
    }

    @Test
    void exigerPermissionReussitSiUtilisateurPossedeLaPermission() {
        Utilisateur utilisateur = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "MotDePasse123");

        assertDoesNotThrow(() -> authService.exigerPermission("VENTE_CREER"));
    }

    @Test
    void deconnexionReinitialiseUtilisateurConnecte() {
        Utilisateur utilisateur = creerUtilisateurTest("MotDePasse123", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "MotDePasse123");
        assertTrue(authService.estConnecte());

        authService.seDeconnecter();

        assertFalse(authService.estConnecte());
        assertNull(authService.getUtilisateurConnecte());
    }

    @Test
    void changementMotDePasseEchoueSiAncienMotDePasseIncorrect() {
        Utilisateur utilisateur = creerUtilisateurTest("AncienMotDePasse1", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "AncienMotDePasse1");

        assertThrows(AuthentificationException.class,
                () -> authService.changerMonMotDePasse("MauvaisAncien1", "NouveauMotDePasse1"));
    }

    @Test
    void changementMotDePasseEchoueSiNouveauMotDePasseTropFaible() {
        Utilisateur utilisateur = creerUtilisateurTest("AncienMotDePasse1", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "AncienMotDePasse1");

        // Mot de passe trop court et sans chiffre : doit etre rejete.
        assertThrows(IllegalArgumentException.class,
                () -> authService.changerMonMotDePasse("AncienMotDePasse1", "faible"));
    }

    @Test
    void changementMotDePasseReussitAvecMotDePasseValide() {
        Utilisateur utilisateur = creerUtilisateurTest("AncienMotDePasse1", CodeRole.CAISSIER, true, false);
        when(utilisateurDaoMock.trouverParNomUtilisateur("jdupont")).thenReturn(Optional.of(utilisateur));
        authService.seConnecter("jdupont", "AncienMotDePasse1");

        assertDoesNotThrow(() -> authService.changerMonMotDePasse("AncienMotDePasse1", "NouveauMotDePasse1"));
        verify(utilisateurDaoMock).changerMotDePasse(eq(1), anyString());
    }
}
