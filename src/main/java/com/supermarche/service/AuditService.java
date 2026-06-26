package com.supermarche.service;

import com.supermarche.dao.JournalAuditDao;
import com.supermarche.model.EntreeJournalAudit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Centralise l'ecriture du journal d'audit (tracabilite des actions
 * sensibles : connexions, ventes, modifications de stock/produits...).
 *
 * Choix de conception : l'audit est "best effort". Si l'ecriture du
 * journal echoue (probleme reseau transitoire avec la base), on logue
 * l'erreur localement mais on NE bloque PAS l'operation metier en cours
 * (une vente ne doit pas echouer parce que le journal n'a pas pu etre
 * ecrit). Le compromis inverse - bloquer toute l'appli si l'audit echoue -
 * serait pire pour la continuite de service d'un commerce.
 */
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final JournalAuditDao journalAuditDao = new JournalAuditDao();
    private AuthService authService;

    public AuditService() {
    }

    /** Permet d'associer un AuthService apres construction (evite une dependance circulaire au demarrage). */
    public void lierAuthService(AuthService authService) {
        this.authService = authService;
    }

    public void enregistrer(Integer idUtilisateur, String action, String entite, Long idEntite, String details) {
        try {
            EntreeJournalAudit entree = new EntreeJournalAudit(idUtilisateur, action, entite, idEntite, details);
            journalAuditDao.enregistrer(entree);
        } catch (Exception e) {
            log.error("Impossible d'ecrire dans le journal d'audit (action={}, entite={}, idEntite={})",
                    action, entite, idEntite, e);
        }
    }

    public void enregistrer(Integer idUtilisateur, String action) {
        enregistrer(idUtilisateur, action, null, null, null);
    }

    /** Consultation du journal, reservee aux titulaires de la permission JOURNAL_VOIR. */
    public List<EntreeJournalAudit> rechercher(LocalDateTime depuis, LocalDateTime jusqua, String action) {
        if (authService != null) {
            authService.exigerPermission("JOURNAL_VOIR");
        }
        return journalAuditDao.rechercher(depuis, jusqua, null, action, 500);
    }
}
