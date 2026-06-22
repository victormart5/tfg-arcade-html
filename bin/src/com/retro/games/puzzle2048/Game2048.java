package com.retro.games.puzzle2048;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;

public class Game2048 extends JPanel {
    private static final long serialVersionUID = 1L;
    private int[][] board = new int[4][4];
    private int score = 0;
    private boolean gameOver = false;
    private boolean win = false;

    private Usuario jugadorActual;
    private UsuarioRepository repo;

    private JPanel panelBotonesFinal;
    private JButton btnReiniciar;
    private JButton btnSalir;

    private JButton btnSonido;
    private boolean sonidoActivado = true; 

    private boolean mostrarControles = true;
    private Image imgControles;
    private Timer timerParpadeo;
    private boolean textoVisible = true;

    public Game2048(Usuario jugador, UsuarioRepository repo) {
        this.jugadorActual = jugador;
        this.repo = repo;
        setPreferredSize(new Dimension(400, 500));
        setBackground(new Color(187, 173, 160));
        setFocusable(true);
        this.setLayout(null);
        
        File fileImg = new File("res/2048Controles.png");
        if (fileImg.exists()) {
            imgControles = new ImageIcon(fileImg.getAbsolutePath()).getImage();
        }

        timerParpadeo = new Timer(500, e -> {
            textoVisible = !textoVisible;
            repaint();
        });
        timerParpadeo.start();
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (mostrarControles) {
                    mostrarControles = false;
                    timerParpadeo.stop();
                    btnSonido.setVisible(true);
                    reiniciarJuego();
                    return;
                }

                if (gameOver || win) return;
                
                boolean moved = false;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:    moved = moveUp(); break;
                    case KeyEvent.VK_DOWN:  moved = moveDown(); break;
                    case KeyEvent.VK_LEFT:  moved = moveLeft(); break;
                    case KeyEvent.VK_RIGHT: moved = moveRight(); break;
                }
                
                if (moved) {
                    spawnRandom();
                    repaint();
                    checkGameState(); 
                }
            }
        });
        
        inicializarBotoneraFinal();
        inicializarBotonSonido();
    }

    private void inicializarBotonSonido() {
        btnSonido = new JButton("SOUND: ON");
        btnSonido.setBounds(265, 16, 115, 32);
        btnSonido.setFont(new Font("Arial", Font.BOLD, 13));
        
        btnSonido.setBackground(new Color(143, 122, 102));
        btnSonido.setForeground(Color.WHITE);
        btnSonido.setBorder(null); 
        btnSonido.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSonido.setFocusable(false); 
        btnSonido.setVisible(false); 

        btnSonido.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) { 
                btnSonido.setBackground(new Color(119, 110, 101)); 
            }
            @Override
            public void mouseExited(MouseEvent e) { 
                btnSonido.setBackground(new Color(143, 122, 102)); 
            }
        });

        btnSonido.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sonidoActivado = !sonidoActivado;
                if (sonidoActivado) {
                    btnSonido.setText("SOUND: ON");
                    btnSonido.setBackground(new Color(143, 122, 102));
                } else {
                    btnSonido.setText("SOUND: OFF");
                    btnSonido.setBackground(new Color(175, 160, 145)); 
                }
                repaint();
                requestFocusInWindow(); 
            }
        });

        this.add(btnSonido);
    }

    private void inicializarBotoneraFinal() {
        panelBotonesFinal = new JPanel(new GridLayout(1, 2, 15, 0));
        panelBotonesFinal.setOpaque(false);
        
        int panelW = 300;
        int panelH = 38;
        int panelX = (400 - panelW) / 2;
        int panelY = 295; 
        panelBotonesFinal.setBounds(panelX, panelY, panelW, panelH);

        btnReiniciar = new JButton("REINTENTAR");
        estilizarBotonInterface(btnReiniciar, Color.GREEN);
        btnReiniciar.addActionListener(e -> {
            panelBotonesFinal.setVisible(false);
            reiniciarJuego();
            this.requestFocusInWindow();
        });

        btnSalir = new JButton("VOLVER AL MENÚ");
        estilizarBotonInterface(btnSalir, new Color(255, 80, 80));
        btnSalir.addActionListener(e -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
        });

        panelBotonesFinal.add(btnReiniciar);
        panelBotonesFinal.add(btnSalir);
        panelBotonesFinal.setVisible(false);
        this.add(panelBotonesFinal);
    }

    private void estilizarBotonInterface(JButton b, Color accentColor) {
        b.setBackground(new Color(45, 40, 40));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100), 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                b.setBackground(new Color(60, 55, 55)); 
                b.setBorder(new LineBorder(accentColor, 1));
            }
            public void mouseExited(MouseEvent e) { 
                b.setBackground(new Color(45, 40, 40)); 
                b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100), 1));
            }
        });
    }

    private void playEfecto(String archivo) {
        if (!sonidoActivado || mostrarControles) return; 
        try {
            File soundPath = new File("res/" + archivo);
            if (soundPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Error al reproducir: " + archivo);
        }
    }

    private void reiniciarJuego() {
        board = new int[4][4];
        score = 0;
        gameOver = false;
        win = false;
        spawnRandom();
        spawnRandom();
        repaint();
    }

    private void spawnRandom() {
        ArrayList<Integer> emptySpaces = new ArrayList<>();
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (board[r][c] == 0) emptySpaces.add(r * 4 + c);
            }
        }
        if (!emptySpaces.isEmpty()) {
            int pos = emptySpaces.get(new Random().nextInt(emptySpaces.size()));
            board[pos / 4][pos % 4] = (Math.random() < 0.9) ? 2 : 4;
        }
    }

    private boolean moveLeft() {
        boolean moved = false;
        for (int r = 0; r < 4; r++) {
            int[] row = board[r];
            int[] newRow = new int[4];
            int pos = 0;
            for (int c = 0; c < 4; c++) {
                if (row[c] != 0) newRow[pos++] = row[c];
            }
            for (int c = 0; c < 3; c++) {
                if (newRow[c] != 0 && newRow[c] == newRow[c+1]) {
                    newRow[c] *= 2;
                    score += newRow[c];
                    newRow[c+1] = 0;
                }
            }
            int[] finalRow = new int[4];
            pos = 0;
            for (int c = 0; c < 4; c++) {
                if (newRow[c] != 0) finalRow[pos++] = newRow[c];
            }
            if (!java.util.Arrays.equals(board[r], finalRow)) moved = true;
            finalRow = finalRow.clone();
            board[r] = finalRow;
        }
        return moved;
    }

    private boolean moveRight() { reverseBoard(); boolean m = moveLeft(); reverseBoard(); return m; }
    private boolean moveUp() { transpose(); boolean m = moveLeft(); transpose(); return m; }
    private boolean moveDown() { transpose(); boolean m = moveRight(); transpose(); return m; }

    private void reverseBoard() {
        for(int r=0; r<4; r++) {
            for(int c=0; c<2; c++) {
                int temp = board[r][c];
                board[r][c] = board[r][3-c];
                board[r][3-c] = temp;
            }
        }
    }

    private void transpose() {
        for(int r=0; r<4; r++) {
            for(int c=r; c<4; c++) {
                int temp = board[r][c];
                board[r][c] = board[c][r];
                board[c][r] = temp;
            }
        }
    }

    private void checkGameState() {
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (board[r][c] == 2048) {
                    win = true;
                    guardarPuntosBaseDatos();
                    playEfecto("victoria.wav"); 
                    panelBotonesFinal.setVisible(true);
                    return;
                }
            }
        }

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                if (board[r][c] == 0) return;
                if (c < 3 && board[r][c] == board[r][c+1]) return;
                if (r < 3 && board[r][c] == board[r+1][c]) return;
            }
        }

        gameOver = true;
        guardarPuntosBaseDatos();
        playEfecto("derrota.wav"); 
        panelBotonesFinal.setVisible(true);
    }

    private void guardarPuntosBaseDatos() {
        if (jugadorActual != null && score > jugadorActual.getPuntos_2048()) {
            jugadorActual.setPuntos_2048(score);
            repo.save(jugadorActual);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (mostrarControles) {
            int panelW = getWidth();
            int panelH = getHeight();
            
            g2.setColor(new Color(15, 15, 18));
            g2.fillRect(0, 0, panelW, panelH);

            if (imgControles != null) {
                int imgW = imgControles.getWidth(this);
                int imgH = imgControles.getHeight(this);
                
                int maxW = panelW - 30;
                int maxH = panelH - 120;
                
                double scaleX = (double) maxW / imgW;
                double scaleY = (double) maxH / imgH;
                double scale = Math.min(scaleX, scaleY);
                
                int targetW = (int) (imgW * scale);
                int targetH = (int) (imgH * scale);
                
                int renderX = (panelW - targetW) / 2;
                int renderY = (panelH - targetH) / 2 - 20;
                
                g2.drawImage(imgControles, renderX, renderY, targetW, targetH, this);
                
                g2.setColor(new Color(0, 255, 255, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(renderX - 2, renderY - 2, targetW + 4, targetH + 4);
                g2.setStroke(new BasicStroke(1f));
            } else {
                g2.setColor(Color.CYAN);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 20));
                g2.drawString("GUÍA DE CONTROLES", 50, 150);
            }

            if (textoVisible) {
                g2.setFont(new Font("Consolas", Font.BOLD, 13));
                g2.setColor(Color.GREEN);
                FontMetrics fm = g2.getFontMetrics();
                String msgInicio = "PULSA CUALQUIER TECLA PARA EMPEZAR";
                int xMsg = (panelW - fm.stringWidth(msgInicio)) / 2;
                g2.drawString(msgInicio, xMsg, panelH - 45);
            }
            return; 
        }

        g.setColor(new Color(119, 110, 101));
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("Puntos: " + score, 20, 40);

        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                drawTile(g2, board[r][c], 20 + c * 90, 70 + r * 90);
            }
        }

        if (gameOver || win) {
            btnSonido.setVisible(false);

            g2.setColor(new Color(15, 15, 20, 220));
            g2.fillRect(0, 0, getWidth(), getHeight());

            int cardW = 340;
            int cardH = 310;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (getHeight() - cardH) / 2;

            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(cardX + 5, cardY + 5, cardW, cardH, 15, 15);

            GradientPaint cardGrad = new GradientPaint(cardX, cardY, new Color(35, 30, 30), cardX, cardY + cardH, new Color(20, 18, 18));
            g2.setPaint(cardGrad);
            g2.fillRoundRect(cardX, cardY, cardW, cardH, 15, 15);

            String headerText;
            Color accentColor;
            if (win) {
                headerText = "¡OBJETIVO ALCANZADO!";
                accentColor = Color.GREEN;
            } else {
                headerText = "FIN DE LA PARTIDA";
                accentColor = new Color(255, 75, 75);
            }

            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 160));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(cardX, cardY, cardW, cardH, 15, 15);
            g2.setStroke(new BasicStroke(1f)); 

            FontMetrics fm;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
            fm = g2.getFontMetrics();
            int titleX = cardX + (cardW - fm.stringWidth(headerText)) / 2;
            
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
            g2.drawString(headerText, titleX - 1, cardY + 51);
            g2.drawString(headerText, titleX + 1, cardY + 51);
            
            g2.setColor(accentColor);
            g2.drawString(headerText, titleX, cardY + 50);

            g2.setColor(new Color(0, 255, 255, 60));
            g2.drawLine(cardX + 30, cardY + 80, cardX + cardW - 30, cardY + 80);

            g2.setFont(new Font("Monospaced", Font.BOLD, 15));
            fm = g2.getFontMetrics();

            g2.setColor(Color.CYAN);
            g2.drawString("PUNTOS:", cardX + 40, cardY + 125);
            g2.setColor(Color.WHITE);
            String scoreStr = score + " PTS";
            g2.drawString(scoreStr, cardX + cardW - 40 - fm.stringWidth(scoreStr), cardY + 125);

            g2.setColor(Color.CYAN);
            g2.drawString("USUARIO:", cardX + 40, cardY + 260);
            g2.setColor(Color.WHITE);
            String opName = (jugadorActual != null) ? jugadorActual.getUsername().toUpperCase() : "INVITADO";
            g2.drawString(opName, cardX + cardW - 40 - fm.stringWidth(opName), cardY + 260);
        } else {
            if (!mostrarControles) btnSonido.setVisible(true); 
        }
    }

    private void drawTile(Graphics2D g, int value, int x, int y) {
        g.setColor(getTileColor(value));
        g.fillRoundRect(x, y, 80, 80, 15, 15);
        
        if (value != 0) {
            g.setColor(value < 8 ? new Color(119, 110, 101) : Color.WHITE);
            String s = String.valueOf(value);
            g.setFont(new Font("Arial", Font.BOLD, value < 100 ? 35 : value < 1000 ? 30 : 25));
            FontMetrics fm = g.getFontMetrics();
            int tx = x + (80 - fm.stringWidth(s)) / 2;
            int ty = y + (80 - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(s, tx, ty);
        }
    }

    private Color getTileColor(int value) {
        switch (value) {
            case 2:    return new Color(238, 228, 218);
            case 4:    return new Color(237, 224, 200);
            case 8:    return new Color(242, 177, 121);
            case 16:   return new Color(245, 149, 99);
            case 32:   return new Color(246, 124, 95);
            case 64:   return new Color(246, 94, 59);
            case 128:  return new Color(237, 207, 114);
            case 256:  return new Color(237, 204, 97);
            case 512:  return new Color(237, 200, 80);
            case 1024: return new Color(237, 197, 63);
            case 2048: return new Color(237, 194, 46);
            default:   return new Color(205, 193, 180);
        }
    }
}