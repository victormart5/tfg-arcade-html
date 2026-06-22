package com.retro.main;

import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class VentanaRanking extends JDialog {
    public VentanaRanking(JFrame padre, UsuarioRepository repo) {
        super(padre, "RANKING DE JUGADORES", true);
        setSize(500, 400);
        setLocationRelativeTo(padre);

        String[] columnas = {"NOMBRE", "SNAKE", "PONG", "2048", "TETRIS"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0);
        JTable tabla = new JTable(modelo);

        List<Usuario> lista = repo.findAll(); 
        
        for (Usuario u : lista) {
            Object[] fila = {
                u.getUsername(), 
                
            };
            modelo.addRow(fila);
        }

        add(new JScrollPane(tabla), BorderLayout.CENTER);
        JButton btnCerrar = new JButton("VOLVER");
        btnCerrar.addActionListener(e -> dispose());
        add(btnCerrar, BorderLayout.SOUTH);
    }
}