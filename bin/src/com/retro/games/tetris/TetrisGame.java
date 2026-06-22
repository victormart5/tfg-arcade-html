package com.retro.games.tetris;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;

import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;

public class TetrisGame extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    
    private final int BOARD_WIDTH = 10;
    private final int BOARD_HEIGHT = 20;
    private final int TILE_SIZE = 30;

    private Timer timer;
    private boolean isStarted = false;
    private int curX = 0;
    private int curY = 0;
    private int score = 0;
    
    private int[][] board;
    private Tetromino curPiece;

    private Clip musicaFondo;
    private boolean musicaActivada = true; 
    private boolean efectosActivados = true; 
    private float volumenMasterBGM = 0.8f;

    private JPanel panelBotonesFinal;
    private JButton btnReiniciar;
    private JButton btnSalir;
    private boolean mostraMenuFinGrafico = false;

    private JButton btnMusica;
    private JButton btnEfectos;

    private boolean mostrarControles = true;
    private Image imgControles;
    private Timer timerParpadeo;
    private boolean textoVisible = true;

    public TetrisGame(Usuario jugador, UsuarioRepository repo) {
        this.jugadorActual = jugador;
        this.repo = repo;
        
        setPreferredSize(new Dimension(BOARD_WIDTH * TILE_SIZE, BOARD_HEIGHT * TILE_SIZE + 50));
        setBackground(new Color(20, 20, 20));
        setFocusable(true);
        this.setLayout(null);
        
        File fileImg = new File("res/tetrisControles.png");
        if (fileImg.exists()) {
            imgControles = new ImageIcon(fileImg.getAbsolutePath()).getImage();
        }

        board = new int[BOARD_HEIGHT][BOARD_WIDTH];
        curPiece = new Tetromino();
        timer = new Timer(400, this); 
        
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
                    btnMusica.setVisible(true);
                    btnEfectos.setVisible(true);
                    playMusicaFondo();
                    start();
                    return;
                }

                if (!isStarted) return;

                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) tryMove(curPiece, curX - 1, curY);
                if (key == KeyEvent.VK_RIGHT) tryMove(curPiece, curX + 1, curY);
                if (key == KeyEvent.VK_DOWN) dropOneLine();
                if (key == KeyEvent.VK_UP) tryMove(curPiece.rotate(), curX, curY);
                if (key == KeyEvent.VK_SPACE) dropToBottom();
            }
        });

        btnMusica = new JButton("MÚSICA: ON");
        btnMusica.setBounds(110, BOARD_HEIGHT * TILE_SIZE + 12, 85, 26);
        btnMusica.setFont(new Font("Consolas", Font.BOLD, 10));
        btnMusica.setBackground(new Color(30, 30, 45));
        btnMusica.setForeground(Color.CYAN);
        btnMusica.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        btnMusica.setFocusable(false);
        btnMusica.setVisible(false); 
        
        btnMusica.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicaActivada = !musicaActivada;
                if (musicaActivada) {
                    btnMusica.setText("MÚSICA: ON");
                    btnMusica.setForeground(Color.CYAN);
                    btnMusica.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
                    playMusicaFondo();
                } else {
                    btnMusica.setText("MÚSICA: OFF");
                    btnMusica.setForeground(Color.LIGHT_GRAY);
                    btnMusica.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                    stopMusica();
                }
                repaint();
                requestFocusInWindow();
            }
        });
        this.add(btnMusica);

        btnEfectos = new JButton("FX: ON");
        btnEfectos.setBounds(205, BOARD_HEIGHT * TILE_SIZE + 12, 85, 26);
        btnEfectos.setFont(new Font("Consolas", Font.BOLD, 10));
        btnEfectos.setBackground(new Color(30, 30, 45));
        btnEfectos.setForeground(Color.CYAN);
        btnEfectos.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        btnEfectos.setFocusable(false);
        btnEfectos.setVisible(false); 
        
        btnEfectos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                efectosActivados = !efectosActivados;
                if (efectosActivados) {
                    btnEfectos.setText("FX: ON");
                    btnEfectos.setForeground(Color.CYAN);
                    btnEfectos.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
                } else {
                    btnEfectos.setText("FX: OFF");
                    btnEfectos.setForeground(Color.LIGHT_GRAY);
                    btnEfectos.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                }
                repaint();
                requestFocusInWindow();
            }
        });
        this.add(btnEfectos);

        inicializarBotoneraFinal();
    }

    public void setVolumenMasterBGM(float vol) {
        this.volumenMasterBGM = vol;
        aplicarVolumenFiltro();
    }

    private void aplicarVolumenFiltro() {
        if (musicaFondo != null && musicaFondo.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) musicaFondo.getControl(FloatControl.Type.MASTER_GAIN);
            float db = (float) (Math.log(volumenMasterBGM <= 0.0f ? 0.0001f : volumenMasterBGM) / Math.log(10.0) * 20.0);
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        }
    }

    private Usuario jugadorActual; 
    private UsuarioRepository repo;

    private void inicializarBotoneraFinal() {
        panelBotonesFinal = new JPanel(new GridLayout(1, 2, 12, 0));
        panelBotonesFinal.setOpaque(false);
        
        int panelW = 240;
        int panelH = 36;
        int panelX = (300 - panelW) / 2;
        int panelY = 400; 
        panelBotonesFinal.setBounds(panelX, panelY, panelW, panelH);

        btnReiniciar = new JButton("REINTENTAR");
        estilizarBotonInterface(btnReiniciar, Color.GREEN);
        btnReiniciar.addActionListener(e -> {
            panelBotonesFinal.setVisible(false);
            mostraMenuFinGrafico = false;
            playMusicaFondo();
            start();
            this.requestFocusInWindow();
        });

        btnSalir = new JButton("VOLVER AL MENÚ");
        estilizarBotonInterface(btnSalir, new Color(255, 80, 80));
        btnSalir.addActionListener(e -> {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            if (topFrame != null) topFrame.dispose();
        });

        panelBotonesFinal.add(btnReiniciar);
        panelBotonesFinal.add(btnSalir);
        panelBotonesFinal.setVisible(false);
        this.add(panelBotonesFinal);
    }

    private void estilizarBotonInterface(JButton b, Color accentColor) {
        b.setBackground(new Color(40, 40, 45));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 11));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100), 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                b.setBackground(new Color(55, 55, 60)); 
                b.setBorder(new LineBorder(accentColor, 1));
            }
            public void mouseExited(MouseEvent e) { 
                b.setBackground(new Color(40, 40, 45)); 
                b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 100), 1));
            }
        });
    }

    private void playMusicaFondo() {
        if (!musicaActivada || mostrarControles) return;
        try {
            if (musicaFondo != null) {
                if (!musicaFondo.isRunning()) {
                    aplicarVolumenFiltro();
                    musicaFondo.start();
                }
            } else {
                File musicPath = new File("res/musica_tetris.wav");
                if (musicPath.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(musicPath);
                    musicaFondo = AudioSystem.getClip();
                    musicaFondo.open(audioInput);
                    aplicarVolumenFiltro();
                    musicaFondo.loop(Clip.LOOP_CONTINUOUSLY);
                    musicaFondo.start();
                }
            }
        } catch (Exception e) {
            System.err.println("Error música Tetris: " + e.getMessage());
        }
    }

    private void playEfecto(String archivo) {
        if (!efectosActivados || mostrarControles) return;
        try {
            File soundPath = new File("res/" + archivo);
            if (soundPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Error efecto: " + archivo);
        }
    }

    private void stopMusica() {
        if (musicaFondo != null && musicaFondo.isRunning()) {
            musicaFondo.stop();
        }
    }

    public void pararMusica() {
        stopMusica();
        if (musicaFondo != null) musicaFondo.close();
    }

    public void start() {
        clearBoard();
        score = 0;
        isStarted = true;
        mostraMenuFinGrafico = false;
        newPiece();
        timer.start();
    }

    private void clearBoard() {
        for (int i = 0; i < BOARD_HEIGHT; i++)
            for (int j = 0; j < BOARD_WIDTH; j++) board[i][j] = 0;
    }

    private void newPiece() {
        curPiece.setRandomShape();
        curX = BOARD_WIDTH / 2;
        curY = 1;

        if (!tryMove(curPiece, curX, curY)) {
            isStarted = false;
            timer.stop();
            stopMusica(); 
            playEfecto("derrota.wav"); 

            if (jugadorActual != null) {
                jugadorActual.setPuntos_tetris(score); 
                repo.save(jugadorActual);
            }
            mostraMenuFinGrafico = true;
            panelBotonesFinal.setVisible(true);
            repaint();
        }
    }

    private boolean tryMove(Tetromino piece, int newX, int newY) {
        for (int i = 0; i < 4; i++) {
            int x = newX + piece.x(i);
            int y = newY + piece.y(i);
            
            if (x < 0 || x >= BOARD_WIDTH || y < 0 || y >= BOARD_HEIGHT) return false;
            if (board[y][x] != 0) return false;
        }
        
        curPiece = piece;
        curX = newX;
        curY = newY;
        repaint();
        return true;
    }

    private void dropOneLine() {
        if (!tryMove(curPiece, curX, curY + 1)) pieceDropped();
    }

    private void pieceDropped() {
        for (int i = 0; i < 4; i++) {
            int x = curX + curPiece.x(i);
            int y = curY + curPiece.y(i);
            board[y][x] = curPiece.getShape();
        }
        removeFullLines();
        newPiece();
    }

    private void removeFullLines() {
        int linesFilled = 0;
        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            boolean full = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] == 0) {
                    full = false;
                    break;
                }
            }
            if (full) {
                linesFilled++;
                for (int k = i; k > 0; k--)
                    System.arraycopy(board[k - 1], 0, board[k], 0, BOARD_WIDTH);
                i++;
            }
        }
        if (linesFilled > 0) {
            score += linesFilled * 100;
            playEfecto("puntos.wav"); 
            repaint();
        }
    }

    private void dropToBottom() {
        int newY = curY;
        while (newY < BOARD_HEIGHT - 1) {
            if (!tryMove(curPiece, curX, newY + 1)) break;
            newY++;
        }
        pieceDropped();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dropOneLine();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (mostrarControles) {
            int panelW = getWidth();
            int panelH = getHeight();
            
            g2d.setColor(new Color(15, 15, 18));
            g2d.fillRect(0, 0, panelW, panelH);

            if (imgControles != null) {
                int imgW = imgControles.getWidth(this);
                int imgH = imgControles.getHeight(this);
                
                int maxW = panelW - 30;
                int maxH = panelH - 120;
                
                double scale = Math.min((double) maxW / imgW, (double) maxH / imgH);
                
                int targetW = (int) (imgW * scale);
                int targetH = (int) (imgH * scale);
                
                int renderX = (panelW - targetW) / 2;
                int renderY = (panelH - targetH) / 2 - 20;
                
                g2d.drawImage(imgControles, renderX, renderY, targetW, targetH, this);
                
                g2d.setColor(new Color(0, 255, 255, 80));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRect(renderX - 2, renderY - 2, targetW + 4, targetH + 4);
                g2d.setStroke(new BasicStroke(1f));
            } else {
                g2d.setColor(Color.CYAN);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 20));
                g2d.drawString("GUÍA DE CONTROLES", 50, 150);
            }

            if (textoVisible) {
                g2d.setFont(new Font("Consolas", Font.BOLD, 13));
                g2d.setColor(Color.GREEN);
                FontMetrics fm = g2d.getFontMetrics();
                String msgInicio = "PULSA CUALQUIER TECLA PARA EMPEZAR";
                int xMsg = (panelW - fm.stringWidth(msgInicio)) / 2;
                g2d.drawString(msgInicio, xMsg, panelH - 45);
            }
            return; 
        }

        g2d.setColor(new Color(25, 25, 35));
        for (int i = 0; i <= BOARD_WIDTH * TILE_SIZE; i += TILE_SIZE) {
            g2d.drawLine(i, 0, i, BOARD_HEIGHT * TILE_SIZE);
        }
        for (int i = 0; i <= BOARD_HEIGHT * TILE_SIZE; i += TILE_SIZE) {
            g2d.drawLine(0, i, BOARD_WIDTH * TILE_SIZE, i);
        }

        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (board[i][j] != 0) drawSquare(g2d, j * TILE_SIZE, i * TILE_SIZE, board[i][j]);
            }
        }

        if (isStarted && curPiece.getShape() != 0) {
            for (int i = 0; i < 4; i++) {
                drawSquare(g2d, (curX + curPiece.x(i)) * TILE_SIZE, (curY + curPiece.y(i)) * TILE_SIZE, curPiece.getShape());
            }
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2d.drawString("PUNTOS: " + score, 10, BOARD_HEIGHT * TILE_SIZE + 28);

        if (mostraMenuFinGrafico) {
            btnMusica.setVisible(false);
            btnEfectos.setVisible(false);

            g2d.setColor(new Color(15, 15, 20, 220));
            g2d.fillRect(0, 0, getWidth(), getHeight());

            int cardW = 270;
            int cardH = 330;
            int cardX = (getWidth() - cardW) / 2;
            int cardY = (BOARD_HEIGHT * TILE_SIZE - cardH) / 2;

            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRoundRect(cardX + 6, cardY + 6, cardW, cardH, 16, 16);

            GradientPaint cardGrad = new GradientPaint(cardX, cardY, new Color(35, 30, 30), cardX, cardY + cardH, new Color(20, 18, 18));
            g2d.setPaint(cardGrad);
            g2d.fillRoundRect(cardX, cardY, cardW, cardH, 16, 16);

            Color accentColor = new Color(255, 75, 75);
            String headerText = "FIN DE LA PARTIDA";

            g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 160));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(cardX, cardY, cardW, cardH, 16, 16);
            g2d.setStroke(new BasicStroke(1f));

            FontMetrics fm;
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 22));
            fm = g2d.getFontMetrics();
            int titleX = cardX + (cardW - fm.stringWidth(headerText)) / 2;
            
            g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
            g2d.drawString(headerText, titleX - 1, cardY + 51);
            g2d.drawString(headerText, titleX + 1, cardY + 51);
            
            g2d.setColor(accentColor);
            g2d.drawString(headerText, titleX, cardY + 50);

            g2d.setColor(new Color(0, 255, 255, 60));
            g2d.drawLine(cardX + 25, cardY + 80, cardX + cardW - 25, cardY + 80);

            g2d.setFont(new Font("Monospaced", Font.BOLD, 14));
            fm = g2d.getFontMetrics();

            g2d.setColor(Color.CYAN);
            g2d.drawString("PUNTOS:", cardX + 30, cardY + 135);
            g2d.setColor(Color.WHITE);
            String scoreStr = score + " PTS";
            g2d.drawString(scoreStr, cardX + cardW - 30 - fm.stringWidth(scoreStr), cardY + 135);

            g2d.setColor(Color.CYAN);
            g2d.drawString("USUARIO:", cardX + 30, cardY + 195);
            g2d.setColor(Color.WHITE);
            String opName = (jugadorActual != null) ? jugadorActual.getUsername().toUpperCase() : "INVITADO";
            g2d.drawString(opName, cardX + cardW - 30 - fm.stringWidth(opName), cardY + 195);
        } else {
            if (!mostrarControles) {
                btnMusica.setVisible(true);
                btnEfectos.setVisible(true);
            }
        }
    }

    private void drawSquare(Graphics g, int x, int y, int shape) {
        Color colors[] = { new Color(0, 0, 0), new Color(204, 102, 102), 
            new Color(102, 204, 102), new Color(102, 102, 204), 
            new Color(204, 204, 102), new Color(204, 102, 204), 
            new Color(102, 204, 204), new Color(218, 170, 0) };
        Color color = colors[shape];
        g.setColor(color);
        g.fillRect(x + 1, y + 1, TILE_SIZE - 2, TILE_SIZE - 2);
        
        g.setColor(color.brighter());
        g.drawLine(x, y + TILE_SIZE - 1, x, y);
        g.drawLine(x, y, x + TILE_SIZE - 1, y);
        g.setColor(color.darker());
        g.drawLine(x + 1, y + TILE_SIZE - 1, x + TILE_SIZE - 1, y + TILE_SIZE - 1);
        g.drawLine(x + TILE_SIZE - 1, y + TILE_SIZE - 1, x + TILE_SIZE - 1, y + 1);
    }

    public void detenerJuego() {
        if (timerParpadeo != null) timerParpadeo.stop();
        if (timer != null) {
            timer.stop();
            stopMusica(); 
        }
    }

    class Tetromino {
        private int[][] coords;
        private int shape;
        private final int[][][] table = {
            {{0,0},{0,0},{0,0},{0,0}},
            {{-1,0},{0,0},{1,0},{0,1}}, 
            {{0,0},{1,0},{0,1},{1,1}}, 
            {{-1,-1},{0,-1},{0,0},{0,1}}, 
            {{1,-1},{0,-1},{0,0},{0,1}}, 
            {{0,-1},{0,0},{0,1},{0,2}}, 
            {{-1,0},{0,0},{0,1},{1,1}}, 
            {{-1,1},{0,1},{0,0},{1,0}}  
        };

        public Tetromino() { coords = new int[4][2]; }
        public void setShape(int s) {
            for(int i=0; i<4; i++) System.arraycopy(table[s][i], 0, coords[i], 0, 2);
            shape = s;
        }
        public void setRandomShape() { setShape(new Random().nextInt(7) + 1); }
        public int x(int i) { return coords[i][0]; }
        public int y(int i) { return coords[i][1]; }
        public int getShape() { return shape; }
        public Tetromino rotate() {
            if (shape == 2) return this;
            Tetromino res = new Tetromino();
            res.shape = shape;
            for(int i=0; i<4; i++) {
                res.coords[i][0] = -coords[i][1];
                res.coords[i][1] = coords[i][0];
            }
            return res;
        }
    }
}