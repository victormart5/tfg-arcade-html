package com.retro.main;

public class MainLauncher {
    public static void main(String[] args) {
        // En lugar de lanzar el juego, lanzamos el menú
        MenuPrincipal menu = new MenuPrincipal();
        menu.setVisible(true);
    }
}