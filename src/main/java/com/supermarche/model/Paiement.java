package com.supermarche.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Paiement {

    private Long id;
    private Long idVente;
    private ModePaiement mode;
    private BigDecimal montant;
    private LocalDateTime horodatage;

    public Paiement() {
    }

    public Paiement(ModePaiement mode, BigDecimal montant) {
        this.mode = mode;
        this.montant = montant;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdVente() {
        return idVente;
    }

    public void setIdVente(Long idVente) {
        this.idVente = idVente;
    }

    public ModePaiement getMode() {
        return mode;
    }

    public void setMode(ModePaiement mode) {
        this.mode = mode;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public LocalDateTime getHorodatage() {
        return horodatage;
    }

    public void setHorodatage(LocalDateTime horodatage) {
        this.horodatage = horodatage;
    }
}
