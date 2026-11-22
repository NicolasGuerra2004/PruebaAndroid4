package com.guerr.barriored.model;

import java.io.Serializable;

public class Alert implements Serializable {
    public static final String CAT_ROBO = "ROBO";
    public static final String CAT_MEDICA = "MEDICA";
    public static final String CAT_SOSPECHOSA = "SOSPECHOSA";
    public static final String CAT_INCENDIO = "INCENDIO";
    public static final String CAT_MASCOTA = "MASCOTA";

    public static final String STATE_ACTIVA = "activa";
    public static final String STATE_ATENDIDA = "atendida";
    public static final String STATE_CERRADA = "cerrada";

    private String id;
    private String categoria;
    private String descripcion;
    private String autorNombre;
    private String autorVivienda; // ej: 311A
    private String autorIdentidad; // ej: "Juan Pérez · #311A"
    private String autorUid;
    private String estado; // "activa", "atendida", "cerrada"
    private long timestamp;

    // Required empty constructor for Firebase Realtime Database
    public Alert() {
    }

    public Alert(String id, String categoria, String descripcion, String autorNombre, String autorVivienda, String autorUid, long timestamp) {
        this.id = id;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.autorNombre = autorNombre;
        this.autorVivienda = autorVivienda != null ? autorVivienda.replace("#", "").trim() : "";
        this.autorIdentidad = ((autorNombre != null ? autorNombre : "Vecino") + " · #" + (this.autorVivienda.isEmpty() ? "S/N" : this.autorVivienda));
        this.autorUid = autorUid;
        this.estado = STATE_ACTIVA;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public String getAutorVivienda() {
        return autorVivienda;
    }

    public void setAutorVivienda(String autorVivienda) {
        this.autorVivienda = autorVivienda != null ? autorVivienda.replace("#", "").trim() : "";
    }

    // Backwards compatibility
    public String getAutorDireccion() {
        return autorVivienda;
    }

    public void setAutorDireccion(String autorDireccion) {
        this.autorVivienda = autorDireccion != null ? autorDireccion.replace("#", "").trim() : "";
    }

    public String getAutorIdentidad() {
        if (autorIdentidad != null && !autorIdentidad.isEmpty()) {
            return autorIdentidad;
        }
        String n = autorNombre != null && !autorNombre.isEmpty() ? autorNombre : "Vecino";
        String v = autorVivienda != null && !autorVivienda.isEmpty() ? autorVivienda : "S/N";
        return n + " · #" + v;
    }

    public void setAutorIdentidad(String autorIdentidad) {
        this.autorIdentidad = autorIdentidad;
    }

    public String getAutorUid() {
        return autorUid;
    }

    public void setAutorUid(String autorUid) {
        this.autorUid = autorUid;
    }

    public String getEstado() {
        return estado != null ? estado : STATE_ACTIVA;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategoriaDisplay() {
        if (categoria == null) return "Alerta General";
        switch (categoria.toUpperCase()) {
            case CAT_ROBO:
                return "Robo / Seguridad";
            case CAT_MEDICA:
                return "Emergencia Médica";
            case CAT_SOSPECHOSA:
                return "Actividad Sospechosa";
            case CAT_INCENDIO:
                return "Incendio / Siniestro";
            case CAT_MASCOTA:
                return "Mascota / Comunidad";
            default:
                return categoria;
        }
    }
}
