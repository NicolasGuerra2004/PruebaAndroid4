package com.guerr.barriored.model;

import java.io.Serializable;
import java.util.regex.Pattern;

public class User implements Serializable {
    private String uid;
    private String nombre;
    private String email;
    private String vivienda; // Formato alfanumérico ej: 311A, 102B
    private String rol; // "vecino" o "admin"
    private String fotoUrl;

    // Pattern for alphanumeric house/apartment number like 311A, 102B, 405
    private static final Pattern VIVIENDA_PATTERN = Pattern.compile("^[a-zA-Z0-9]{2,10}$");

    // Required empty constructor for Firebase Realtime Database
    public User() {
    }

    public User(String uid, String nombre, String email, String vivienda, String rol, String fotoUrl) {
        this.uid = uid;
        this.nombre = nombre;
        this.email = email;
        this.vivienda = vivienda != null ? vivienda.replace("#", "").trim() : "";
        this.rol = rol != null ? rol : "vecino";
        this.fotoUrl = fotoUrl != null ? fotoUrl : "";
    }

    public static boolean isValidVivienda(String vivienda) {
        if (vivienda == null) return false;
        String cleaned = vivienda.replace("#", "").trim();
        return VIVIENDA_PATTERN.matcher(cleaned).matches();
    }

    public String getIdentidad() {
        String nameStr = (nombre != null && !nombre.trim().isEmpty()) ? nombre.trim() : "Vecino";
        String vivStr = (vivienda != null && !vivienda.trim().isEmpty()) ? vivienda.replace("#", "").trim() : "S/N";
        return nameStr + " · #" + vivStr;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getVivienda() {
        return vivienda;
    }

    public void setVivienda(String vivienda) {
        this.vivienda = vivienda != null ? vivienda.replace("#", "").trim() : "";
    }

    // Backwards compatibility for existing records
    public String getDireccion() {
        return vivienda;
    }

    public void setDireccion(String direccion) {
        this.vivienda = direccion != null ? direccion.replace("#", "").trim() : "";
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public boolean isAdmin() {
        return "admin".equalsIgnoreCase(rol);
    }
}
