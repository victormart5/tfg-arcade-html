package com.retro.main.model;

import jakarta.persistence.*;

@Entity
@Table(name = "logs_sistema")
public class LogRegistro {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String contenido;

    public LogRegistro() {}

    public LogRegistro(String contenido) {
        this.contenido = contenido;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
}