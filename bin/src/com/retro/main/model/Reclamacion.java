package com.retro.main.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamaciones")
public class Reclamacion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    
    @Column(columnDefinition = "TEXT")
    private String mensaje;
    
    private LocalDateTime fecha = LocalDateTime.now();

    public Reclamacion() {}

    public Reclamacion(String username, String mensaje) {
        this.username = username;
        this.mensaje = mensaje;
    }

    public Long getId() { 
        return id; 
    }
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getUsername() { 
        return username; 
    }
    public void setUsername(String username) { 
        this.username = username; 
    }

    public String getMensaje() { 
        return mensaje; 
    }
    public void setMensaje(String mensaje) { 
        this.mensaje = mensaje; 
    }

    public LocalDateTime getFecha() { 
        return fecha; 
    }
    public void setFecha(LocalDateTime fecha) { 
        this.fecha = fecha; 
    }
}