package com.supermarche.config;

import com.supermarche.service.AuditService;
import com.supermarche.service.AuthService;
import com.supermarche.service.CaisseService;
import com.supermarche.service.ClientService;
import com.supermarche.service.FournisseurService;
import com.supermarche.service.ProduitService;
import com.supermarche.service.RapportService;
import com.supermarche.service.StockService;
import com.supermarche.service.UtilisateurService;
import com.supermarche.service.VenteService;

/**
 * Conteneur simple d'instances de services, partage entre tous les
 * controleurs JavaFX de l'application. Un vrai framework d'injection de
 * dependances (Spring, etc.) serait disproportionne pour une application
 * desktop de cette taille ; cette classe joue le meme role de maniere
 * minimaliste et explicite.
 */
public final class ContexteApplication {

    private static final ContexteApplication INSTANCE = new ContexteApplication();

    private final AuthService authService = new AuthService();
    private final AuditService auditService = new AuditService();
    private final ProduitService produitService = new ProduitService(authService);
    private final StockService stockService = new StockService(authService);
    private final VenteService venteService = new VenteService(authService);
    private final CaisseService caisseService = new CaisseService(authService);
    private final ClientService clientService = new ClientService(authService);
    private final FournisseurService fournisseurService = new FournisseurService(authService);
    private final UtilisateurService utilisateurService = new UtilisateurService(authService);
    private final RapportService rapportService = new RapportService(authService);

    /** Caisse et depot choisis a la connexion pour ce poste de travail (persistent pendant la session JavaFX). */
    private Integer idCaisseCourante;
    private Integer idDepotCourant;
    private Long idSessionCaisseCourante;

    private ContexteApplication() {
        auditService.lierAuthService(authService);
    }

    public static ContexteApplication getInstance() {
        return INSTANCE;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public ProduitService getProduitService() {
        return produitService;
    }

    public StockService getStockService() {
        return stockService;
    }

    public VenteService getVenteService() {
        return venteService;
    }

    public CaisseService getCaisseService() {
        return caisseService;
    }

    public ClientService getClientService() {
        return clientService;
    }

    public FournisseurService getFournisseurService() {
        return fournisseurService;
    }

    public UtilisateurService getUtilisateurService() {
        return utilisateurService;
    }

    public RapportService getRapportService() {
        return rapportService;
    }

    public Integer getIdCaisseCourante() {
        return idCaisseCourante;
    }

    public void setIdCaisseCourante(Integer idCaisseCourante) {
        this.idCaisseCourante = idCaisseCourante;
    }

    public Integer getIdDepotCourant() {
        return idDepotCourant;
    }

    public void setIdDepotCourant(Integer idDepotCourant) {
        this.idDepotCourant = idDepotCourant;
    }

    public Long getIdSessionCaisseCourante() {
        return idSessionCaisseCourante;
    }

    public void setIdSessionCaisseCourante(Long idSessionCaisseCourante) {
        this.idSessionCaisseCourante = idSessionCaisseCourante;
    }

    /** Reinitialise l'etat de session courante (a la deconnexion). */
    public void reinitialiserSession() {
        this.idCaisseCourante = null;
        this.idDepotCourant = null;
        this.idSessionCaisseCourante = null;
    }
}
