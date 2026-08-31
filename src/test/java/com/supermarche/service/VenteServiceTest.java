package com.supermarche.service;

import com.supermarche.dao.ClientDao;
import com.supermarche.dao.SessionCaisseDao;
import com.supermarche.dao.StockDao;
import com.supermarche.dao.VenteDao;
import com.supermarche.exception.AutorisationException;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.LigneVente;
import com.supermarche.model.ModePaiement;
import com.supermarche.model.Paiement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Tests de la validation metier de VenteService, AVANT toute interaction
 * avec la base de donnees (panier vide, permission manquante, paiement
 * insuffisant). Ces controles doivent rejeter la demande immediatement,
 * sans jamais ouvrir de transaction ni toucher au stock.
 *
 * Le comportement transactionnel complet (commit/rollback reel sur
 * MariaDB) releve de tests d'integration avec une vraie base, hors
 * perimetre de ces tests unitaires.
 */
class VenteServiceTest {

    private VenteService venteService;
    private AuthService authServiceSansPermission;

    @BeforeEach
    void setUp() {
        VenteDao venteDaoMock = mock(VenteDao.class);
        StockDao stockDaoMock = mock(StockDao.class);
        ClientDao clientDaoMock = mock(ClientDao.class);
        SessionCaisseDao sessionCaisseDaoMock = mock(SessionCaisseDao.class);
        AuditService auditService = new AuditService();

        // AuthService reel mais sans aucun utilisateur connecte : toute
        // exigerPermission("VENTE_CREER") doit donc echouer.
        authServiceSansPermission = new AuthService();

        venteService = new VenteService(authServiceSansPermission, venteDaoMock, stockDaoMock,
                clientDaoMock, sessionCaisseDaoMock, auditService);
    }

    @Test
    void enregistrerVenteEchoueSiAucunUtilisateurConnecte() {
        List<LigneVente> lignes = List.of(new LigneVente(1, "Produit", BigDecimal.ONE,
                new BigDecimal("10.00"), new BigDecimal("20.00")));
        List<Paiement> paiements = List.of(new Paiement(ModePaiement.ESPECES, new BigDecimal("12.00")));

        // Sans permission VENTE_CREER (personne connecte), l'appel doit
        // etre rejete avant meme de regarder le contenu du panier.
        assertThrows(com.supermarche.exception.AuthentificationException.class,
                () -> venteService.enregistrerVente(1L, 1, null, lignes, paiements));
    }

    @Test
    void enregistrerVenteEchoueSiPanierVide() {
        // On simule un utilisateur connecte avec la permission requise en
        // utilisant un AuthService reel relie a un faux DAO autorisant tout.
        AuthService authServiceAvecPermission = creerAuthServiceAvecPermissionVente();
        VenteService service = new VenteService(authServiceAvecPermission, mock(VenteDao.class),
                mock(StockDao.class), mock(ClientDao.class), mock(SessionCaisseDao.class), new AuditService());

        assertThrows(IllegalArgumentException.class,
                () -> service.enregistrerVente(1L, 1, null, Collections.emptyList(), Collections.emptyList()));
    }

    @Test
    void enregistrerVenteEchoueSiPaiementInsuffisant() {
        AuthService authServiceAvecPermission = creerAuthServiceAvecPermissionVente();
        VenteService service = new VenteService(authServiceAvecPermission, mock(VenteDao.class),
                mock(StockDao.class), mock(ClientDao.class), mock(SessionCaisseDao.class), new AuditService());

        List<LigneVente> lignes = List.of(new LigneVente(1, "Produit", BigDecimal.ONE,
                new BigDecimal("50.00"), new BigDecimal("20.00")));
        // Le client ne paie que 10€ pour un article a 50€ : doit etre rejete avant toute ecriture en base.
        List<Paiement> paiements = List.of(new Paiement(ModePaiement.ESPECES, new BigDecimal("10.00")));

        assertThrows(SupermarcheException.class,
                () -> service.enregistrerVente(1L, 1, null, lignes, paiements));
    }

    /**
     * Construit un AuthService reel dans un etat "connecte avec la
     * permission VENTE_CREER", en s'appuyant sur le mecanisme reel de
     * connexion (et non sur de la reflexion), pour rester fidele au vrai
     * comportement de controle d'acces.
     */
    private AuthService creerAuthServiceAvecPermissionVente() {
        com.supermarche.dao.UtilisateurDao utilisateurDaoMock = mock(com.supermarche.dao.UtilisateurDao.class);
        AuthService authService = new AuthService(utilisateurDaoMock, new AuditService());

        com.supermarche.model.Utilisateur utilisateur = new com.supermarche.model.Utilisateur();
        utilisateur.setId(1);
        utilisateur.setNomUtilisateur("caissier1");
        utilisateur.setMotDePasseHash(com.supermarche.security.PasswordHasher.hacher("MotDePasse123"));
        utilisateur.setActif(true);

        com.supermarche.model.Employe employe = new com.supermarche.model.Employe();
        employe.setId(1);
        employe.setNom("Test");
        employe.setPrenom("Caissier");
        utilisateur.setEmploye(employe);

        com.supermarche.model.Role role = new com.supermarche.model.Role(
                1, com.supermarche.model.CodeRole.CAISSIER, "Caissier", null);
        role.setPermissions(java.util.Set.of("VENTE_CREER"));
        utilisateur.setRole(role);

        org.mockito.Mockito.when(utilisateurDaoMock.trouverParNomUtilisateur("caissier1"))
                .thenReturn(java.util.Optional.of(utilisateur));

        authService.seConnecter("caissier1", "MotDePasse123");
        return authService;
    }
}
