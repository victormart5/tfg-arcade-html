package com.retro.main.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private int puntos_snake = 0;
    private int puntos_2048 = 0;
    private int puntos_tetris = 0;
    
    private int puntos_break = 0;
    private int puntos_catch = 0;
    
    @Column(name = "dark_mode")
    private boolean darkMode = false;

    @Column(name = "baneado")
    private Boolean baneado = false; 

    @Column(name = "avisar_desbaneo")
    private Boolean avisarDesbaneo = false;

    public Long getId() { return id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public int getPuntos_snake() { return puntos_snake; }
    public void setPuntos_snake(int puntos_snake) { this.puntos_snake = puntos_snake; }
    
    public int getPuntos_2048() { return puntos_2048; }
    public void setPuntos_2048(int puntos_2048) { this.puntos_2048 = puntos_2048; }
    
    public int getPuntos_tetris() { return puntos_tetris; }
    public void setPuntos_tetris(int puntos_tetris) { this.puntos_tetris = puntos_tetris; }

    public int getPuntos_break() { return puntos_break; }
    public void setPuntos_break(int puntos_break) { this.puntos_break = puntos_break; }

    public int getPuntos_catch() { return puntos_catch; }
    public void setPuntos_catch(int puntos_catch) { this.puntos_catch = puntos_catch; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    public boolean isBaneado() { 
        return baneado != null && baneado; 
    }
    
    public void setBaneado(Boolean baneado) { 
        this.baneado = baneado; 
    }

    public boolean isAvisarDesbaneo() {
        return avisarDesbaneo != null && avisarDesbaneo;
    }

    public void setAvisarDesbaneo(Boolean avisarDesbaneo) {
        this.avisarDesbaneo = avisarDesbaneo;
    }
}