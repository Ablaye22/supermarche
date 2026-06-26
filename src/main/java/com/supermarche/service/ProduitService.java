package com.supermarche.service;

import com.supermarche.dao.ProduitDao;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.Produit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class ProduitService {

    private final ProduitDao produitDao = new ProduitDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public ProduitService(AuthService authService) {
        this.authService = authService;
    }

    public Produit creerProduit(Produit produit) {
        authService.exigerPermission("PRODUIT_GERER");
        validerProduit(produit);

        if (produitDao.trouverParCodeBarre(produit.getCodeBarre()).isPresent()) {
            throw new SupermarcheException("Un produit avec ce code-barre existe deja.");
        }

        Produit cree = produitDao.creer(produit);
        auditService.enregistrer(idUtilisateurConnecte(), "PRODUIT_CREE", "produits",
                cree.getId().longValue(), cree.getDesignation());
        return cree;
    }

    public void modifierProduit(Produit produit) {
        authService.exigerPermission("PRODUIT_GERER");
        validerProduit(produit);
        produitDao.modifier(produit);
        auditService.enregistrer(idUtilisateurConnecte(), "PRODUIT_MODIFIE", "produits",
                produit.getId().longValue(), produit.getDesignation());
    }

    public void desactiverProduit(int idProduit) {
        authService.exigerPermission("PRODUIT_GERER");
        Produit produit = trouverParIdObligatoire(idProduit);
        produit.setActif(false);
        produitDao.modifier(produit);
        auditService.enregistrer(idUtilisateurConnecte(), "PRODUIT_DESACTIVE", "produits", (long) idProduit, null);
    }

    public Produit trouverParIdObligatoire(int id) {
        return produitDao.trouverParId(id)
                .orElseThrow(() -> new SupermarcheException("Produit introuvable (id=" + id + ")"));
    }

    public Optional<Produit> trouverParCodeBarre(String codeBarre) {
        return produitDao.trouverParCodeBarre(codeBarre);
    }

    public List<Produit> rechercher(String motCle) {
        return produitDao.rechercherParDesignation(motCle, false);
    }

    public List<Produit> listerTous() {
        return produitDao.listerTous(false);
    }

    public List<Produit> listerEnAlerteStock(int idDepot) {
        return produitDao.listerEnAlerteStock(idDepot);
    }

    private void validerProduit(Produit p) {
        if (p.getDesignation() == null || p.getDesignation().isBlank()) {
            throw new IllegalArgumentException("La designation du produit est obligatoire.");
        }
        if (p.getCodeBarre() == null || p.getCodeBarre().isBlank()) {
            throw new IllegalArgumentException("Le code-barre du produit est obligatoire.");
        }
        if (p.getPrixVenteTtc() == null || p.getPrixVenteTtc().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix de vente doit etre positif ou nul.");
        }
        if (p.getPrixAchatHt() == null || p.getPrixAchatHt().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix d'achat doit etre positif ou nul.");
        }
        if (p.getSeuilAlerteStock() < 0) {
            throw new IllegalArgumentException("Le seuil d'alerte de stock ne peut pas etre negatif.");
        }
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
