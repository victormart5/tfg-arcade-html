package com.retro.games.snake;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.JButton; 
import javax.swing.border.LineBorder;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.File;
import javax.swing.ImageIcon; 

import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;

public class SnakeGame extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final int TILE_SIZE = 25;
    private final int WIDTH = 800; 
    private final int HEIGHT = 625; 
    private final int GRID_WIDTH = 32;
    private final int GRID_HEIGHT = 23;

    private double headX, headY;
    private double currentVelX = 4.5; 
    private double currentVelY = 0.0;
    private double targetVelX = 4.5;
    private double targetVelY = 0.0;
    
    private final ArrayList<Point.Double> history = new ArrayList<>();
    private final ArrayList<Point> snake = new ArrayList<>(); 
    private final ArrayList<Point> applesInLevel = new ArrayList<>();
    private final ArrayList<Point> enemies = new ArrayList<>();
    private int[][] maze = new int[GRID_HEIGHT][GRID_WIDTH];
    
    private int currentLevel = 1;
    private boolean running = false;
    private boolean levelCleared = false;
    private boolean gameFinished = false;

    private long startTime;
    private boolean timerStarted = false;
    private int tiempoFinalSegundos = 0;
    private long instantePausa;            
    private long tiempoPausadoAcumulado;   

    private Usuario jugadorActual;
    private UsuarioRepository repo;

    private Clip musicaFondo;
    private boolean musicaActivada = true; 
    private boolean efectosActivados = true; 
    private float volumenMasterBGM = 0.8f; 

    private JButton btnMusica;
    private JButton btnEfectos;

    private boolean juegoPausado = false;
    private JButton btnPausaAdmin;
    private JPanel panelPausaAdmin;
    private JButton btnReanudarAdmin;
    private JButton btnReiniciarNivelAdmin;

    private Timer gameLoopTimer;
    private int currentDirection = KeyEvent.VK_RIGHT;

    private boolean mostrarControles = true;
    private Image imgControles;
    private Timer timerParpadeo;
    private boolean textoVisible = true;

    private boolean partidaAlteradaPorAdmin = false;

    public SnakeGame(Usuario jugador, UsuarioRepository repo) {
        this.jugadorActual = jugador;
        this.repo = repo;
        
        this.setLayout(null);
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(15, 15, 20)); 
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        
        File fileImg = new File("res/snakeControles.png");
        if (!fileImg.exists()) {
            fileImg = new File("res/snakeControles.jpg");
        }
        if (fileImg.exists()) {
            imgControles = new ImageIcon(fileImg.getAbsolutePath()).getImage();
        }

        timerParpadeo = new Timer(500, e -> {
            textoVisible = !textoVisible;
            repaint();
        });
        timerParpadeo.start();

        btnMusica = new JButton("MÚSICA: ON");
        btnMusica.setBounds(530, 585, 110, 30);
        btnMusica.setFont(new Font("Consolas", Font.BOLD, 12));
        btnMusica.setBackground(new Color(30, 30, 45));
        btnMusica.setForeground(Color.CYAN);
        btnMusica.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 1));
        btnMusica.setFocusable(false); 
        btnMusica.setVisible(false); 
        
        btnMusica.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicaActivada = !musicaActivada;
                if (musicaActivada) {
                    btnMusica.setText("MÚSICA: ON");
                    btnMusica.setForeground(Color.CYAN);
                    btnMusica.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 1));
                    playMusicaFondo();
                } else {
                    btnMusica.setText("MÚSICA: OFF");
                    btnMusica.setForeground(Color.LIGHT_GRAY);
                    btnMusica.setBorder(javax.swing.BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                    stopMusicaFondo();
                }
                repaint();
                requestFocusInWindow();
            }
        });
        this.add(btnMusica);

        btnEfectos = new JButton("EFECTOS: ON");
        btnEfectos.setBounds(650, 585, 110, 30);
        btnEfectos.setFont(new Font("Consolas", Font.BOLD, 12));
        btnEfectos.setBackground(new Color(30, 30, 45));
        btnEfectos.setForeground(Color.CYAN);
        btnEfectos.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 1));
        btnEfectos.setFocusable(false); 
        btnEfectos.setVisible(false); 
        
        btnEfectos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                efectosActivados = !efectosActivados;
                if (efectosActivados) {
                    btnEfectos.setText("EFECTOS: ON");
                    btnEfectos.setForeground(Color.CYAN);
                    btnEfectos.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 1));
                } else {
                    btnEfectos.setText("EFECTOS: OFF");
                    btnEfectos.setForeground(Color.LIGHT_GRAY);
                    btnEfectos.setBorder(javax.swing.BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
                }
                repaint();
                requestFocusInWindow();
            }
        });
        this.add(btnEfectos);

        if (esAdministrador()) {
            inicializarSistemaPausaAdmin();
        }

        gameLoopTimer = new Timer(16, this);
        SwingUtilities.invokeLater(() -> repaint());
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

    private boolean esAdministrador() {
        return jugadorActual != null && "admin".equalsIgnoreCase(jugadorActual.getUsername());
    }

    private void inicializarSistemaPausaAdmin() {
        btnPausaAdmin = new JButton("PAUSA");
        btnPausaAdmin.setBounds(410, 585, 110, 30);
        btnPausaAdmin.setFont(new Font("Consolas", Font.BOLD, 12));
        btnPausaAdmin.setBackground(new Color(45, 20, 20));
        btnPausaAdmin.setForeground(Color.YELLOW);
        btnPausaAdmin.setBorder(javax.swing.BorderFactory.createLineBorder(Color.YELLOW, 1));
        btnPausaAdmin.setFocusable(false);
        btnPausaAdmin.setVisible(false);
        btnPausaAdmin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnPausaAdmin.addActionListener(e -> alternarPausaAdmin());
        this.add(btnPausaAdmin);

        panelPausaAdmin = new JPanel(new GridLayout(1, 2, 20, 0));
        panelPausaAdmin.setOpaque(false);
        panelPausaAdmin.setBounds((WIDTH - 440) / 2, ((HEIGHT - 260) / 2) + 110, 440, 40);
        panelPausaAdmin.setVisible(false);

        btnReanudarAdmin = new JButton("REANUDAR");
        estilizarBotonPausa(btnReanudarAdmin, Color.GREEN);
        btnReanudarAdmin.addActionListener(e -> alternarPausaAdmin());

        btnReiniciarNivelAdmin = new JButton("REINICIAR NIVEL");
        estilizarBotonPausa(btnReiniciarNivelAdmin, Color.ORANGE);
        btnReiniciarNivelAdmin.addActionListener(e -> {
            alternarPausaAdmin(); 
            loadLevel(currentLevel); 
        });

        panelPausaAdmin.add(btnReanudarAdmin);
        panelPausaAdmin.add(btnReiniciarNivelAdmin);
        this.add(panelPausaAdmin);
    }

    private void estilizarBotonPausa(JButton b, Color accentColor) {
        b.setBackground(new Color(35, 35, 45));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Consolas", Font.BOLD, 13));
        b.setFocusable(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120), 1));
        
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                b.setBackground(new Color(48, 48, 60)); 
                b.setBorder(new LineBorder(accentColor, 1));
            }
            public void mouseExited(MouseEvent e) { 
                b.setBackground(new Color(35, 35, 45)); 
                b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120), 1));
            }
        });
    }

    private void alternarPausaAdmin() {
        if (!esAdministrador() || !running || levelCleared || gameFinished) return;

        juegoPausado = !juegoPausado;

        if (juegoPausado) {
            gameLoopTimer.stop();
            stopMusicaFondo();
            instantePausa = System.currentTimeMillis(); 
            if (panelPausaAdmin != null) panelPausaAdmin.setVisible(true);
        } else {
            long duracionDeEstaPausa = System.currentTimeMillis() - instantePausa;
            tiempoPausadoAcumulado += duracionDeEstaPausa; 
            
            if (panelPausaAdmin != null) panelPausaAdmin.setVisible(false);
            playMusicaFondo();
            gameLoopTimer.start();
        }
        repaint();
        requestFocusInWindow(); 
    }

    private void forzarCompletarNivelAdmin() {
        if (!esAdministrador() || !running || levelCleared || gameFinished || juegoPausado) return;

        partidaAlteradaPorAdmin = true; 
        stopMusicaFondo(); 
        playSonidoEfecto("victoria.wav"); 
        
        if (currentLevel == 5) {
            gameFinished = true;
            gameLoopTimer.stop();
            long endTime = System.currentTimeMillis();
            
            tiempoFinalSegundos = (int) (((endTime - startTime) - tiempoPausadoAcumulado) / 1000);
            
            if (jugadorActual != null) {
                jugadorActual.setPuntos_snake(tiempoFinalSegundos);
                repo.save(jugadorActual);
            }
        } else {
            levelCleared = true;
            gameLoopTimer.stop();
        }
        repaint();
    }

    private void playMusicaFondo() {
        if (!musicaActivada || mostrarControles || juegoPausado) return; 
        try {
            if (musicaFondo != null) {
                if (!musicaFondo.isRunning()) {
                    aplicarVolumenFiltro();
                    musicaFondo.start();
                }
            } else {
                File musicPath = new File("res/musica_fondo.wav");
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
            System.err.println("Error música: " + e.getMessage());
        }
    }

    private void stopMusicaFondo() {
        if (musicaFondo != null && musicaFondo.isRunning()) {
            musicaFondo.stop();
        }
    }

    public void pararMusica() {
        stopMusicaFondo();
        if (musicaFondo != null) musicaFondo.close();
    }

    private void playSonidoEfecto(String archivo) {
        if (!efectosActivados || mostrarControles || juegoPausado) return; 
        try {
            File soundPath = new File("res/" + archivo);
            if (soundPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Error efecto " + archivo + ": " + e.getMessage());
        }
    }

    private void playSonidoComida() {
        playSonidoEfecto("comida.wav");
    }

    public void loadLevel(int level) {
        this.currentLevel = level;
        this.running = true;
        this.levelCleared = false;
        this.gameFinished = false;
        this.juegoPausado = false;
        this.currentDirection = KeyEvent.VK_RIGHT; 
        
        if (level == 5) {
            this.headX = TILE_SIZE * 1;
            this.headY = TILE_SIZE * 1;
        } else {
            this.headX = TILE_SIZE * 4;
            this.headY = TILE_SIZE * 2;
        }
        
        this.targetVelX = 4.5;
        this.targetVelY = 0.0;
        this.currentVelX = 4.5;
        this.currentVelY = 0.0;

        if (panelPausaAdmin != null) panelPausaAdmin.setVisible(false);

        if (musicaFondo != null) {
            musicaFondo.setFramePosition(0); 
        }
        playMusicaFondo();

        if (level == 1) {
            timerStarted = false;
            tiempoFinalSegundos = 0;
            tiempoPausadoAcumulado = 0; 
            partidaAlteradaPorAdmin = false; 
            
            if (jugadorActual != null) {
                jugadorActual.setPuntos_snake(0);
                repo.save(jugadorActual);
            }
        }

        snake.clear();
        history.clear();
        applesInLevel.clear();
        enemies.clear();
        
        int totalInitialSegments = 4; 
        int stepsPerTile = (int)(TILE_SIZE / 4.5);
        for (int i = 0; i < totalInitialSegments * stepsPerTile; i++) {
            history.add(new Point.Double(headX - (i * 4.5), headY));
        }
        
        snake.add(new Point((int)headX, (int)headY));
        snake.add(new Point((int)headX - TILE_SIZE, (int)headY));
        snake.add(new Point((int)headX - (TILE_SIZE * 2), (int)headY));
        
        generateMapData(level);
        
        gameLoopTimer.start();
        repaint();
    }

    private void setTile(int x, int y, int type) {
        if (x < 0 || x >= GRID_WIDTH || y < 0 || y >= GRID_HEIGHT) return;
        if (type == 1) maze[y][x] = 1; 
        if (type == 2) applesInLevel.add(new Point(x * TILE_SIZE, y * TILE_SIZE)); 
        if (type == 3) enemies.add(new Point(x * TILE_SIZE, y * TILE_SIZE)); 
    }

    private void generateMapData(int level) {
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) maze[y][x] = 0;
        }
        
        for (int x = 0; x < 32; x++) { setTile(x, 0, 1); setTile(x, 22, 1); }
        for (int y = 0; y < 23; y++) { setTile(0, y, 1); setTile(31, y, 1); }

        if (level == 1) {
            for (int y = 4; y < 9; y++) { setTile(8, y, 1); setTile(23, y + 10, 1); }
        } else if (level == 2) {
            for (int y = 3; y < 12; y++) { setTile(8, y, 1); setTile(23, y + 7, 1); }
            for (int y = 10; y < 19; y++) { setTile(14, y, 1); setTile(17, y - 6, 1); }
        } else if (level == 3) {
            for (int x = 4; x < 28; x++) {
                if (x % 4 == 0) {
                    for (int y = 3; y < 14; y++) setTile(x, y, 1);
                } else if (x % 4 == 2) {
                    for (int y = 9; y < 20; y++) setTile(x, y, 1);
                }
            }
        } else if (level == 4) {
            for (int x = 5; x <= 12; x++) { setTile(x, 5, 1); setTile(x, 17, 1); setTile(x + 14, 5, 1); setTile(x + 14, 17, 1); }
            for (int y = 6; y <= 10; y++) { setTile(5, y, 1); setTile(12, y, 1); setTile(19, y, 1); setTile(26, y, 1); }
            for (int y = 12; y <= 16; y++) { setTile(5, y, 1); setTile(12, y, 1); setTile(19, y, 1); setTile(26, y, 1); }
        } else if (level == 5) {
            for (int x = 3; x < 29; x += 3) {
                for (int y = 2; y < 21; y += 3) {
                    setTile(x, y, 1); setTile(x + 1, y, 1);
                }
            }
            for (int x = 12; x < 20; x++) setTile(x, 11, 1);
        }

        int[][] manzanasProvisionales; 
        if (level == 1) {
            manzanasProvisionales = new int[][]{{5,5}, {10,18}, {15,11}, {20,4}, {25,15}, {12,7}, {18,14}, {7,12}, {27,8}, {14,19}};
        } else if (level == 2) {
            manzanasProvisionales = new int[][]{{3,4}, {5,17}, {11,6}, {12,15}, {15,3}, {16,18}, {19,7}, {20,14}, {26,5}, {28,16}, {9,20}, {22,2}};
        } else if (level == 3) {
            manzanasProvisionales = new int[][]{{2,5}, {6,5}, {10,5}, {14,5}, {18,5}, {22,5}, {26,5}, {30,5}, {2,17}, {6,17}, {10,17}, {14,17}, {18,17}, {26,17}};
        } else if (level == 4) {
            manzanasProvisionales = new int[][]{{2,2}, {3,20}, {7,2}, {9,20}, {15,2}, {16,20}, {21,2}, {23,20}, {29,2}, {30,20}, {1,11}, {14,11}, {17,11}, {30,11}, {15,8}, {16,14}};
        } else { 
            manzanasProvisionales = new int[][]{{1,3}, {2,3}, {5,3}, {8,3}, {11,3}, {14,3}, {17,3}, {20,3}, {23,3}, {26,3}, {29,3}, {30,3}, {5,9}, {11,9}, {17,9}, {23,9}, {29,9}, {2,15}, {8,15}, {20,15}};
        }

        for (int[] m : manzanasProvisionales) {
            int mx = m[0];
            int my = m[1];
            while (maze[my][mx] == 1) {
                mx = (mx + 1) % (GRID_WIDTH - 2) + 1; 
            }
            setTile(mx, my, 2);
        }

        if (level == 1) {
            setTile(16, 11, 3);
        } else if (level == 2) {
            setTile(11, 11, 3); setTile(20, 11, 3);
        } else if (level == 3) {
            setTile(2, 11, 3); setTile(15, 6, 3); setTile(29, 11, 3);
        } else if (level == 4) {
            setTile(2, 11, 3); setTile(14, 8, 3); setTile(17, 14, 3); setTile(29, 11, 3);
        } else if (level == 5) {
            setTile(2, 19, 3); 
            setTile(29, 1, 3); setTile(14, 6, 3); setTile(17, 15, 3); setTile(29, 21, 3);
        }
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
                
                int maxW = panelW - 80;
                int maxH = panelH - 140;
                
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
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 24));
                g2d.drawString("GUÍA DE CONTROLES", 50, 150);
            }

            if (textoVisible) {
                g2d.setFont(new Font("Consolas", Font.BOLD, 15));
                g2d.setColor(Color.GREEN);
                FontMetrics fm = g2d.getFontMetrics();
                String msgInicio = "PULSA CUALQUIER TECLA PARA EMPEZAR";
                int xMsg = (panelW - fm.stringWidth(msgInicio)) / 2;
                g2d.drawString(msgInicio, xMsg, panelH - 55);
            }
            
            if (panelPausaAdmin != null && panelPausaAdmin.isVisible()) {
                panelPausaAdmin.setVisible(false);
            }
            if (btnPausaAdmin != null && btnPausaAdmin.isVisible()) {
                btnPausaAdmin.setVisible(false);
            }
            return; 
        }

        g2d.setColor(new Color(25, 25, 40, 140));
        for(int i = 0; i < WIDTH; i += TILE_SIZE) g2d.drawLine(i, 0, i, 575);
        for(int i = 0; i < 575; i += TILE_SIZE) g2d.drawLine(0, i, WIDTH, i);

        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (maze[y][x] == 1) {
                    g2d.setPaint(new GradientPaint((float)(x * TILE_SIZE), (float)(y * TILE_SIZE), new Color(45, 48, 71), 
                                                   (float)((x + 1) * TILE_SIZE), (float)((y + 1) * TILE_SIZE), new Color(20, 22, 33)));
                    g2d.fillRoundRect(x * TILE_SIZE + 1, y * TILE_SIZE + 1, TILE_SIZE - 2, TILE_SIZE - 2, 6, 6);
                    g2d.setColor(new Color(0, 255, 255, 90));
                    g2d.drawRoundRect(x * TILE_SIZE + 1, y * TILE_SIZE + 1, TILE_SIZE - 3, TILE_SIZE - 3, 6, 6);
                }
            }
        }

        for (Point p : applesInLevel) {
            int x = p.x; int y = p.y;
            g2d.setColor(new Color(139, 69, 19)); g2d.fillRect(x + 11, y + 2, 2, 5);
            g2d.setColor(new Color(46, 204, 113)); g2d.fillOval(x + 12, y + 1, 6, 4);
            
            g2d.setPaint(new RadialGradientPaint(new Point(x + 12, y + 13), 11f, new float[]{0f, 1f}, 
                        new Color[]{new Color(255, 0, 51, 110), new Color(0, 0, 0, 0)}));
            g2d.fillOval(x + 1, y + 3, 22, 20);
            
            g2d.setPaint(new GradientPaint((float)(x + 4), (float)(y + 6), new Color(255, 71, 87), (float)(x + 20), (float)(y + 20), new Color(178, 34, 34)));
            g2d.fillOval(x + 3, y + 5, 19, 17);
            g2d.setColor(new Color(255, 255, 255, 180)); g2d.fillOval(x + 6, y + 8, 4, 3);
        }

        for (Point e : enemies) {
            g2d.setPaint(new RadialGradientPaint(new Point(e.x + 12, e.y + 12), 15f, new float[]{0.0f, 1.0f}, 
                        new Color[]{new Color(241, 196, 15, 95), new Color(0, 0, 0, 0)}));
            g2d.fillOval(e.x - 3, e.y - 3, TILE_SIZE + 6, TILE_SIZE + 6);
            
            g2d.setPaint(new GradientPaint((float)e.x, (float)e.y, new Color(241, 196, 15), (float)e.x, (float)(e.y + TILE_SIZE), new Color(212, 140, 10)));
            g2d.fillArc(e.x + 2, e.y + 2, TILE_SIZE - 4, TILE_SIZE - 4, 0, 180);
            g2d.fillRect(e.x + 2, e.y + TILE_SIZE / 2, TILE_SIZE - 4, TILE_SIZE / 3);
            
            g2d.fillOval(e.x + 2, e.y + TILE_SIZE - 6, 6, 5);
            g2d.fillOval(e.x + 9, e.y + TILE_SIZE - 6, 6, 5);
            g2d.fillOval(e.x + 16, e.y + TILE_SIZE - 6, 7, 5);
            
            g2d.setColor(Color.BLACK);
            g2d.fillOval(e.x + 6, e.y + 7, 4, 5); g2d.fillOval(e.x + 14, e.y + 7, 4, 5);
            g2d.setColor(Color.RED);
            g2d.fillOval(e.x + 7, e.y + 8, 2, 2); g2d.fillOval(e.x + 15, e.y + 8, 2, 2);
        }

        int stepsPerTile = (int)(TILE_SIZE / 4.5);
        for (int i = snake.size() - 1; i > 0; i--) {
            int historyIndex = i * stepsPerTile;
            if (historyIndex < history.size()) {
                Point.Double pt = history.get(historyIndex);
                float ratio = (float) i / snake.size();
                int greenVal = Math.max(75, 215 - (int)(ratio * 130));
                int blueVal = Math.min(140, 35 + (int)(ratio * 105));
                
                g2d.setPaint(new GradientPaint((float)pt.x, (float)pt.y, new Color(30, greenVal, blueVal), (float)(pt.x + TILE_SIZE), (float)(pt.y + TILE_SIZE), new Color(10, greenVal - 50, blueVal / 2)));
                g2d.fillRoundRect((int)pt.x + 1, (int)pt.y + 1, TILE_SIZE - 2, TILE_SIZE - 2, 8, 8);
                g2d.setColor(new Color(0, greenVal, blueVal, 110));
                g2d.drawRoundRect((int)pt.x + 1, (int)pt.y + 1, TILE_SIZE - 3, TILE_SIZE - 3, 8, 8);
            }
        }

        g2d.setPaint(new GradientPaint((float)headX, (float)headY, new Color(46, 213, 115), (float)(headX + TILE_SIZE), (float)(headY + TILE_SIZE), new Color(34, 112, 63)));
        g2d.fillRoundRect((int)headX, (int)headY, TILE_SIZE, TILE_SIZE, 11, 11);
        g2d.setColor(Color.GREEN);
        g2d.drawRoundRect((int)headX, (int)headY, TILE_SIZE - 1, TILE_SIZE - 1, 11, 11);
        
        g2d.setColor(Color.WHITE); g2d.fillOval((int)headX + 4, (int)headY + 5, 6, 7); g2d.fillOval((int)headX + 15, (int)headY + 5, 6, 7);
        g2d.setColor(new Color(10, 15, 30)); g2d.fillOval((int)headX + 6, (int)headY + 7, 3, 4); g2d.fillOval((int)headX + 17, (int)headY + 7, 3, 4);

        g2d.setPaint(new GradientPaint(0, 575, new Color(24, 28, 43), 0, HEIGHT, new Color(14, 16, 26)));
        g2d.fillRect(0, 575, WIDTH, 50);
        g2d.setColor(Color.CYAN); g2d.drawRect(0, 575, WIDTH-1, 49);
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.drawString("NIVEL: " + currentLevel, 30, 608);
        g2d.drawString("RESTAN: " + applesInLevel.size(), 180, 608);

        if (esAdministrador() && !gameFinished) {
            drawAdminHUD(g2d);
        }

        if (gameFinished) {
            ocultarBotoneraInteractiva();
            drawFinalOverlay(g2d);
        } else if (juegoPausado) {
            drawOverlayPausaAdmin(g2d, "SESIÓN EN PAUSA", "Panel de Control de Depuración del Admin", Color.YELLOW);
        } else if (levelCleared) {
            ocultarBotoneraInteractiva();
            drawOverlay(g2d, "¡NIVEL " + currentLevel + " COMPLETADO!", "Presiona 'N' para el siguiente nivel", Color.GREEN);
        } else if (!running) {
            ocultarBotoneraInteractiva();
            drawOverlay(g2d, "FIN DEL JUEGO", "Presiona 'R' para reintentar", Color.RED);
        } else {
            btnMusica.setVisible(true); 
            btnEfectos.setVisible(true); 
            if (esAdministrador()) btnPausaAdmin.setVisible(true);
        }
    }

    private void drawAdminHUD(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setFont(new Font("Consolas", Font.BOLD, 12));
        g2d.setColor(Color.YELLOW);
        g2d.drawString("BOTONES 1-5: Mapas", 295, 595);
    }

    private void ocultarBotoneraInteractiva() {
        btnMusica.setVisible(false);
        btnEfectos.setVisible(false);
        if (btnPausaAdmin != null) btnPausaAdmin.setVisible(false);
        if (panelPausaAdmin != null) panelPausaAdmin.setVisible(false);
    }

    private void drawOverlayPausaAdmin(Graphics2D g2d, String title, String subtitle, Color mainColor) {
        g2d.setColor(new Color(10, 10, 15, 230));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        int panelW = 560;
        int panelH = 260;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = (HEIGHT - panelH) / 2 - 20;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(panelX + 8, panelY + 8, panelW, panelH, 20, 20);

        GradientPaint panelGrad = new GradientPaint((float)panelX, (float)panelY, new Color(25, 25, 35), (float)panelX, (float)(panelY + panelH), new Color(15, 15, 20));
        g2d.setPaint(panelGrad);
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 180));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2d.setStroke(new BasicStroke(1f));

        FontMetrics fm;
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 38));
        fm = g2d.getFontMetrics();
        int titleX = panelX + (panelW - fm.stringWidth(title)) / 2;
        
        g2d.setColor(mainColor);
        g2d.drawString(title, titleX, panelY + 60);

        g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 100));
        g2d.drawLine(panelX + 40, panelY + 95, panelX + panelW - 40, panelY + 95);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.PLAIN, 18));
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, panelX + (panelW - fm.stringWidth(subtitle)) / 2, panelY + 215);
    }

    private void drawOverlay(Graphics2D g2d, String title, String subtitle, Color mainColor) {
        g2d.setColor(new Color(10, 10, 15, 230));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        int panelW = 560;
        int panelH = 260;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = (HEIGHT - panelH) / 2 - 20;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(panelX + 8, panelY + 8, panelW, panelH, 20, 20);

        GradientPaint panelGrad = new GradientPaint((float)panelX, (float)panelY, new Color(25, 25, 35), (float)panelX, (float)(panelY + panelH), new Color(15, 15, 20));
        g2d.setPaint(panelGrad);
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        g2d.setColor(new Color(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), 180));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2d.setStroke(new BasicStroke(1f));

        FontMetrics fm;
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 38));
        fm = g2d.getFontMetrics();
        int titleX = panelX + (panelW - fm.stringWidth(title)) / 2;
        
        g2d.setColor(mainColor);
        g2d.drawString(title, titleX, panelY + 90);

        g2d.setColor(new Color(mainColor.getRed(), mainGreen(), mainColor.getBlue(), 100));
        g2d.drawLine(panelX + 40, panelY + 130, panelX + panelW - 40, panelY + 130);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.PLAIN, 20));
        fm = g2d.getFontMetrics();
        g2d.drawString(subtitle, panelX + (panelW - fm.stringWidth(subtitle)) / 2, panelY + 180);
    }

    private int mainGreen() { return 255; }

    private void drawFinalOverlay(Graphics2D g2d) {
        g2d.setColor(new Color(10, 10, 15, 230));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        int panelW = 560;
        int panelH = 360;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = (HEIGHT - panelH) / 2 - 20;

        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(panelX + 8, panelY + 8, panelW, panelH, 20, 20);

        GradientPaint panelGrad = new GradientPaint((float)panelX, (float)panelY, new Color(25, 25, 35), (float)panelX, (float)(panelY + panelH), new Color(15, 15, 20));
        g2d.setPaint(panelGrad);
        g2d.fillRoundRect(panelX, panelY, panelW, panelH, 20, 20);

        g2d.setColor(new Color(255, 215, 0, 180));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(panelX, panelY, panelW, panelH, 20, 20);
        g2d.setStroke(new BasicStroke(1f));

        FontMetrics fm;
        String titleText = "MISIÓN CUMPLIDA";
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 38));
        fm = g2d.getFontMetrics();
        int titleX = panelX + (panelW - fm.stringWidth(titleText)) / 2;
        
        g2d.setColor(new Color(255, 215, 0));
        g2d.drawString(titleText, titleX, panelY + 60);

        g2d.setColor(new Color(255, 215, 0, 100));
        g2d.drawLine(panelX + 40, panelY + 90, panelX + panelW - 40, panelY + 90);

        g2d.setFont(new Font("Monospaced", Font.BOLD, 18));
        fm = g2d.getFontMetrics();
        
        String userStr = jugadorActual != null ? jugadorActual.getUsername().toUpperCase() : "INVITADO";
        g2d.setColor(Color.CYAN);
        g2d.drawString("OPERADOR:", panelX + 60, panelY + 140);
        g2d.setColor(Color.WHITE);
        g2d.drawString(userStr, panelX + panelW - 60 - fm.stringWidth(userStr), panelY + 140);

        String timeStr = partidaAlteradaPorAdmin ? "MODO TEST (0s)" : tiempoFinalSegundos + " SEGUNDOS";
        
        g2d.setColor(new Color(255, 215, 0)); 
        g2d.drawString("TIEMPO RÉCORD:", panelX + 60, panelY + 180);
        g2d.setColor(Color.WHITE);
        g2d.drawString(timeStr, panelX + panelW - 60 - fm.stringWidth(timeStr), panelY + 180);

        String authStr = "JORGE & VICTOR";
        g2d.setColor(Color.CYAN);
        g2d.drawString("DESARROLLADORES:", panelX + 60, panelY + 220);
        g2d.setColor(new Color(15, 15, 20));
        g2d.drawString(authStr, panelX + panelW - 60 - fm.stringWidth(authStr), panelY + 220);

        g2d.setColor(new Color(255, 215, 0, 40));
        g2d.drawLine(panelX + 40, panelY + 250, panelX + panelW - 40, panelY + 250);

        g2d.setFont(new Font("Consolas", Font.BOLD, 14));
        fm = g2d.getFontMetrics();
        
        String prompt1 = "[C] VOLVER AL MENÚ OS";
        String prompt2 = "[R] REINICIAR JUEGO";
        String prompt3 = "[V] REINICIAR A NIVEL 1";
        
        int spacing = panelW / 3;
        g2d.setColor(new Color(255, 80, 80)); 
        g2d.drawString(prompt1, panelX + (spacing - fm.stringWidth(prompt1))/2 + 5, panelY + 295);
        
        g2d.setColor(Color.GREEN); 
        g2d.drawString(prompt2, panelX + spacing + (spacing - fm.stringWidth(prompt2))/2, panelY + 295);
        
        g2d.setColor(Color.YELLOW); 
        g2d.drawString(prompt3, panelX + spacing*2 + (spacing - fm.stringWidth(prompt3))/2 - 5, panelY + 295);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !levelCleared && !gameFinished && !juegoPausado) {
            currentVelX += (targetVelX - currentVelX) * 0.25;
            currentVelY += (targetVelY - currentVelY) * 0.25;
            autoMove(currentVelX, currentVelY);
            repaint(); 
        }
    }

    private void autoMove(double dx, double dy) {
        if (!running || levelCleared || gameFinished || juegoPausado) return;

        if (!timerStarted) {
            startTime = System.currentTimeMillis();
            timerStarted = true;
        }

        headX += dx;
        headY += dy;
        
        if (headX < 0) headX = WIDTH - TILE_SIZE;
        if (headX >= WIDTH) headX = 0;
        if (headY < 0) headY = 550;
        if (headY > 550) headY = 0;

        history.add(0, new Point.Double(headX, headY));
        
        int maxHistoryNeeded = snake.size() * (int)(TILE_SIZE / 4.5) + 10;
        if (history.size() > maxHistoryNeeded) {
            history.remove(history.size() - 1);
        }

        int gridX = (int)((headX + TILE_SIZE / 2) / TILE_SIZE);
        int gridY = (int)((headY + TILE_SIZE / 2) / TILE_SIZE);

        if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
            if (maze[gridY][gridX] == 1 || checkCuerpoFluido()) {
                morir("choque.wav");
                return;
            }
            
            Point headCenter = new Point((int)headX + TILE_SIZE / 2, (int)headY + TILE_SIZE / 2);
            Point eaten = null;
            for (Point p : applesInLevel) {
                Point appleCenter = new Point(p.x + TILE_SIZE / 2, p.y + TILE_SIZE / 2);
                if (headCenter.distance(appleCenter) < TILE_SIZE * 0.85) {
                    eaten = p;
                    break;
                }
            }
            
            if (eaten != null) {
                playSonidoComida(); 
                applesInLevel.remove(eaten);
                snake.add(new Point(-100, -100)); 
                
                if (applesInLevel.isEmpty()) {
                    stopMusicaFondo(); 
                    playSonidoEfecto("victoria.wav"); 
                    
                    if (currentLevel == 5) {
                        gameFinished = true;
                        gameLoopTimer.stop();
                        long endTime = System.currentTimeMillis();
                        tiempoFinalSegundos = (int) (((endTime - startTime) - tiempoPausadoAcumulado) / 1000);
                        
                        if (jugadorActual != null && !partidaAlteradaPorAdmin) {
                            jugadorActual.setPuntos_snake(tiempoFinalSegundos);
                            repo.save(jugadorActual);
                        }
                    } else {
                        levelCleared = true;
                        gameLoopTimer.stop();
                    }
                }
            }
        }

        moveEnemies();

        Point headCenter = new Point((int)headX + TILE_SIZE / 2, (int)headY + TILE_SIZE / 2);
        for (Point e : enemies) {
            Point enemyCenter = new Point(e.x + TILE_SIZE / 2, e.y + TILE_SIZE / 2);
            if (headCenter.distance(enemyCenter) < TILE_SIZE * 0.8) {
                morir("fantasma.wav");
                return;
            }
        }
    }
    
    private boolean checkCuerpoFluido() {
        int stepsPerTile = (int)(TILE_SIZE / 4.5);
        Point headCenter = new Point((int)headX + TILE_SIZE / 2, (int)headY + TILE_SIZE / 2);
        
        for (int i = 3; i < snake.size(); i++) {
            int idx = i * stepsPerTile;
            if (idx < history.size()) {
                Point.Double pt = history.get(idx);
                Point bodyCenter = new Point((int)pt.x + TILE_SIZE / 2, (int)pt.y + TILE_SIZE / 2);
                if (headCenter.distance(bodyCenter) < TILE_SIZE * 0.5) {
                    return true;
                }
            }
        }
        return false;
    }

    public void detenerJuego() {
        if (timerParpadeo != null) timerParpadeo.stop();
        if (gameLoopTimer != null) gameLoopTimer.stop();
        stopMusicaFondo();
    }

    private void morir(String sonidoCausa) {
        running = false;
        gameLoopTimer.stop();
        stopMusicaFondo(); 
        playSonidoEfecto(sonidoCausa);   
        
        if (jugadorActual != null) {
            jugadorActual.setPuntos_snake(0);
            repo.save(jugadorActual);
        }
        repaint();
    }

    private void moveEnemies() {
        for (Point e : enemies) {
            if (Math.random() < 0.65) {
                int nx = e.x, ny = e.y;
                int step = 2; 

                double diffX = headX - e.x;
                double diffY = headY - e.y;

                if (Math.random() < 0.80) {
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        nx += (diffX > 0) ? step : -step;
                    } else {
                        ny += (diffY > 0) ? step : -step;
                    }
                } else {
                    int dir = (int)(Math.random() * 4);
                    if (dir == 0) ny -= step; 
                    else if (dir == 1) ny += step;
                    else if (dir == 2) nx -= step; 
                    else nx += step;
                }

                if (canMoveGhost(nx, ny)) {
                    e.x = nx; e.y = ny;
                } else {
                    nx = e.x; ny = e.y;
                    if (Math.random() < 0.5) {
                        nx += (Math.random() < 0.5) ? step : -step;
                    } else {
                        ny += (Math.random() < 0.5) ? step : -step;
                    }
                    
                    if (canMoveGhost(nx, ny)) {
                        e.x = nx; e.y = ny;
                    }
                }
            }
        }
    }

    private boolean canMoveGhost(int nx, int ny) {
        int size = TILE_SIZE - 2; 
        int[] ptsX = {nx + 1, nx + size, nx + 1, nx + size};
        int[] ptsY = {ny + 1, ny + 1, ny + size, ny + size};
        
        for(int i = 0; i < 4; i++) {
            int gx = ptsX[i] / TILE_SIZE;
            int gy = ptsY[i] / TILE_SIZE;
            if (gx < 0 || gx >= GRID_WIDTH || gy < 0 || gy >= GRID_HEIGHT || maze[gy][gx] == 1) {
                return false;
            }
        }
        return true;
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            
            if (mostrarControles) {
                mostrarControles = false;
                timerParpadeo.stop();
                btnMusica.setVisible(true);
                btnEfectos.setVisible(true);
                if (esAdministrador()) btnPausaAdmin.setVisible(true);
                loadLevel(1); 
                
                requestFocusInWindow();
                return;
            }

            if (esAdministrador()) {
                if (key == KeyEvent.VK_P) {
                    alternarPausaAdmin();
                    return;
                }
                
                if (juegoPausado) return;

                if (key == KeyEvent.VK_PAGE_UP) {
                    forzarCompletarNivelAdmin();
                    return;
                }
                if (key == KeyEvent.VK_1 || key == KeyEvent.VK_NUMPAD1) { partidaAlteradaPorAdmin = true; loadLevel(1); return; }
                if (key == KeyEvent.VK_2 || key == KeyEvent.VK_NUMPAD2) { partidaAlteradaPorAdmin = true; loadLevel(2); return; }
                if (key == KeyEvent.VK_3 || key == KeyEvent.VK_NUMPAD3) { partidaAlteradaPorAdmin = true; loadLevel(3); return; }
                if (key == KeyEvent.VK_4 || key == KeyEvent.VK_NUMPAD4) { partidaAlteradaPorAdmin = true; loadLevel(4); return; }
                if (key == KeyEvent.VK_5 || key == KeyEvent.VK_NUMPAD5) { partidaAlteradaPorAdmin = true; loadLevel(5); return; }
            }

            if (juegoPausado) return;

            if (gameFinished) {
                if (key == KeyEvent.VK_C) { 
                    stopMusicaFondo();
                    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(SnakeGame.this);
                    if(topFrame != null) topFrame.dispose();
                }
                if (key == KeyEvent.VK_R) loadLevel(5);
                if (key == KeyEvent.VK_V) loadLevel(1);
            } else if (levelCleared) {
                if (key == KeyEvent.VK_N && currentLevel < 5) loadLevel(currentLevel + 1);
                if (key == KeyEvent.VK_R) loadLevel(currentLevel);
            } else if (!running) {
                if (key == KeyEvent.VK_R) loadLevel(currentLevel);
            } else {
                if ((key == KeyEvent.VK_UP || key == KeyEvent.VK_W) && currentDirection != KeyEvent.VK_DOWN) {
                    currentDirection = KeyEvent.VK_UP;
                    targetVelX = 0.0; targetVelY = -4.5;
                }
                else if ((key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) && currentDirection != KeyEvent.VK_UP) {
                    currentDirection = KeyEvent.VK_DOWN;
                    targetVelX = 0.0; targetVelY = 4.5;
                }
                else if ((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) && currentDirection != KeyEvent.VK_RIGHT) {
                    currentDirection = KeyEvent.VK_LEFT;
                    targetVelX = -4.5; targetVelY = 0.0;
                }
                else if ((key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) && currentDirection != KeyEvent.VK_LEFT) {
                    currentDirection = KeyEvent.VK_RIGHT;
                    targetVelX = 4.5; targetVelY = 0.0;
                }
            }
        }
    }
}