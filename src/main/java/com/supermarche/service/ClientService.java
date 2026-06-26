package com.supermarche.service;

import com.supermarche.dao.ClientDao;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.Client;

import java.util.List;
import java.util.Optional;

public class ClientService {

    private final ClientDao clientDao = new ClientDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public ClientService(AuthService authService) {
        this.authService = authService;
    }

    public Client creerClient(Client client) {
        authService.exigerPermission("CLIENT_GERER");
        validerClient(client);
        Client cree = clientDao.creer(client);
        auditService.enregistrer(idUtilisateurConnecte(), "CLIENT_CREE", "clients", cree.getId().longValue(), null);
        return cree;
    }

    public void modifierClient(Client client) {
        authService.exigerPermission("CLIENT_GERER");
        validerClient(client);
        clientDao.modifier(client);
        auditService.enregistrer(idUtilisateurConnecte(), "CLIENT_MODIFIE", "clients", client.getId().longValue(), null);
    }

    public Optional<Client> trouverParCarteFidelite(String carte) {
        return clientDao.trouverParCarteFidelite(carte);
    }

    public List<Client> rechercher(String motCle) {
        return clientDao.rechercherParNom(motCle);
    }

    public List<Client> listerTous() {
        return clientDao.listerTous();
    }

    private void validerClient(Client c) {
        if (c.getNom() == null || c.getNom().isBlank()) {
            throw new IllegalArgumentException("Le nom du client est obligatoire.");
        }
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
