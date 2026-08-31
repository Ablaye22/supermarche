package com.supermarche.model;

import java.util.HashSet;
import java.util.Set;

public class Role {

    private Integer id;
    private CodeRole code;
    private String libelle;
    private String description;
    private Set<String> permissions = new HashSet<>();

    public Role() {
    }

    public Role(Integer id, CodeRole code, String libelle, String description) {
        this.id = id;
        this.code = code;
        this.libelle = libelle;
        this.description = description;
    }

    public boolean possedePermission(String codePermission) {
        return permissions.contains(codePermission);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public CodeRole getCode() {
        return code;
    }

    public void setCode(CodeRole code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
