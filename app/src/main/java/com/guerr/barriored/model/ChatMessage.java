package com.guerr.barriored.model;

import java.io.Serializable;

public class ChatMessage implements Serializable {
    private String id;
    private String senderUid;
    private String senderNombre; // Format: Nombre · #311A
    private String texto;
    private long timestamp;

    // Required empty constructor for Firebase Realtime Database
    public ChatMessage() {
    }

    public ChatMessage(String id, String senderUid, String senderNombre, String texto, long timestamp) {
        this.id = id;
        this.senderUid = senderUid;
        this.senderNombre = senderNombre;
        this.texto = texto;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderNombre() {
        return senderNombre;
    }

    public void setSenderNombre(String senderNombre) {
        this.senderNombre = senderNombre;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
