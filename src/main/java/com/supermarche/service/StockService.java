package com.supermarche.service;

import com.supermarche.dao.StockDao;
import com.supermarche.model.MouvementStock;
import com.supermarche.model.Stock;

import java.util.List;

public class StockService {

    private final StockDao stockDao = new StockDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public StockService(AuthService authService) {
        this.authService = authService;
    }

    public void enregistrerEntree(int idProduit, int idDepot, int quantite, String reference) {
        authService.exigerPermission("STOCK_GERER");
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantite d'entree doit etre positive.");
        }
        stockDao.enregistrerEntree(idProduit, idDepot, quantite, idUtilisateurConnecte(), reference);
        auditService.enregistrer(idUtilisateurConnecte(), "STOCK_ENTREE", "produits", (long) idProduit,
                "Quantite : " + quantite + (reference != null ? " (" + reference + ")" : ""));
    }

    /** Ajustement d'inventaire : fixe directement la nouvelle quantite (et non un delta). */
    public void ajusterStock(int idProduit, int idDepot, int nouvelleQuantite, String commentaire) {
        authService.exigerPermission("STOCK_GERER");
        if (nouvelleQuantite < 0) {
            throw new IllegalArgumentException("La quantite en stock ne peut pas etre negative.");
        }
        stockDao.ajusterStock(idProduit, idDepot, nouvelleQuantite, idUtilisateurConnecte(), commentaire);
        auditService.enregistrer(idUtilisateurConnecte(), "STOCK_AJUSTE", "produits", (long) idProduit,
                "Nouvelle quantite : " + nouvelleQuantite + (commentaire != null ? " - " + commentaire : ""));
    }

    public int lireQuantite(int idProduit, int idDepot) {
        return stockDao.lireQuantite(idProduit, idDepot);
    }

    public List<Stock> listerParDepot(int idDepot) {
        return stockDao.listerParDepot(idDepot);
    }

    public List<MouvementStock> historiqueParProduit(int idProduit) {
        return stockDao.historiqueParProduit(idProduit, 200);
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
