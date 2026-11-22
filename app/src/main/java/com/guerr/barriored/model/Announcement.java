package com.guerr.barriored.model;

import java.io.Serializable;

public class Announcement implements Serializable {
    private String id;
    private String titulo;
    private String descripcion;
    private long fecha;
    private String autorNombre;
    private String autorUid;
    private boolean importante;

    // Required empty constructor for Firebase Realtime Database
    public Announcement() {
    }

    public Announcement(String id, String titulo, String descripcion, long fecha, String autorNombre, String autorUid, boolean importante) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.autorNombre = autorNombre;
        this.autorUid = autorUid;
        this.importante = importante;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public long getFecha() {
        return fecha;
    }

    public void setFecha(long fecha) {
        this.fecha = fecha;
    }

    public String getAutorNombre() {
        return autorNombre;
    }

    public void setAutorNombre(String autorNombre) {
        this.autorNombre = autorNombre;
    }

    public String getAutorUid() {
        return autorUid;
    }

    public void setAutorUid(String autorUid) {
        this.autorUid = autorUid;
    }

    public boolean isImportante() {
        return importante;
    }

    public void setImportante(boolean importante) {
        this.importante = importante;
    }
}
