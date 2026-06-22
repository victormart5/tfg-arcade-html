package com.retro.games.pong;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

/**
 * Clase principal del minijuego Pong Retro Multimodo.
 * Implementa una arquitectura basada en estados, Inteligencia Artificial adaptativa,
 * sistema de aceleración progresiva por impacto y un menú de pausa universal.
 * * @author JORGE & VICTOR
 * @version 2.5 - TFG EDITION
 */
public class PongGame extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final int WIDTH = 800, HEIGHT = 500;
    private final int PADDLE_WIDTH = 15, PADDLE_HEIGHT = 80;
    private final int BALL_SIZE = 15;

    private int p1Y = 210, p2Y = 210;
    private int ballX = 400, ballY = 250;
    
    private int ballXSpeed = -4, ballYSpeed = 4;
    private final int VELOCIDAD_INICIAL_X = 4;
    private final int VELOCIDAD_INICIAL_Y = 4;
    private final int MAX_VELOCIDAD = 12;

    private int score1 = 0, score2 = 0;
    
    private int timeLeft = 60; 
    private int frameCounter = 0; 
    private boolean gameFinished = false;

    private int waitFrames = 0; 
    private boolean waiting = false; 

    private boolean wPressed, sPressed, upPressed, downPressed;
    private Timer timer;

    private JPanel panelBotonesFinal;
    private JButton btnReiniciar;
    private JButton btnSalir;

    private boolean juegoPausado = false;
    private JPanel panelBotonesPausa;
    private JButton btnReanudarPausa;
    private JButton btnReiniciarPausa;
    private JButton btnSalirPausa;
    
    private JButton btnPausaInterfaz;

    private JButton btnSonido;
    private boolean sonidoActivado = true; 

    private int estadoMenu = 0; 
    private boolean contraMaquina = false;
    private int nivelDificultad = 2; 

    private Image imgControles;
    private Timer timerParpadeo;
    private boolean textoVisible = true;

    
    public PongGame() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.setLayout(null);

        File fileImgPng = new File("res/pongControles.png");
        File fileImgJpg = new File("res/pongControles.jpg");
        
        if (fileImgPng.exists()) {
            imgControles = new ImageIcon(fileImgPng.getAbsolutePath()).getImage();
        } else if (fileImgJpg.exists()) {
            imgControles = new ImageIcon(fileImgJpg.getAbsolutePath()).getImage();
        }

        timerParpadeo = new Timer(500, e -> {
            textoVisible = !textoVisible;
            repaint();
        });
        timerParpadeo.start();

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();

                if (estadoMenu == 0) { 
                    if (key == KeyEvent.VK_1) { contraMaquina = false; estadoMenu = 2; } 
                    else if (key == KeyEvent.VK_2) { contraMaquina = true; estadoMenu = 1; }
                    repaint();
                    return;
                }

                if (estadoMenu == 1) { 
                    if (key == KeyEvent.VK_1) { nivelDificultad = 1; comenzarPartidaActiva(); }
                    if (key == KeyEvent.VK_2) { nivelDificultad = 2; comenzarPartidaActiva(); }
                    if (key == KeyEvent.VK_3) { nivelDificultad = 3; comenzarPartidaActiva(); }
                    return;
                }

                if (estadoMenu == 2) { 
                    comenzarPartidaActiva();
                    return;
                }

                if (key == KeyEvent.VK_P && !gameFinished) {
                    alternarPausaJuego();
                    return;
                }

                if (juegoPausado) return;

                if (key == KeyEvent.VK_W) wPressed = true;
                if (key == KeyEvent.VK_S) sPressed = true;
                if (!contraMaquina) { 
                    if (key == KeyEvent.VK_UP) upPressed = true;
                    if (key == KeyEvent.VK_DOWN) downPressed = true;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (juegoPausado) return;
                
                if (e.getKeyCode() == KeyEvent.VK_W) wPressed = false;
                if (e.getKeyCode() == KeyEvent.VK_S) sPressed = false;
                if (!contraMaquina) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) upPressed = false;
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) downPressed = false;
                }
            }
        });

        inicializarBotoneraPausa();
        inicializarBotoneraFinal();
        inicializarBotónSonido();
        inicializarBotónPausaInterfaz(); 

        timer = new Timer(10, this); 
        SwingUtilities.invokeLater(() -> repaint());
    }

    
    private void comenzarPartidaActiva() {
        estadoMenu = 3; 
        juegoPausado = false;
        panelBotonesPausa.setVisible(false);
        if (timerParpadeo != null) timerParpadeo.stop();
        
        btnSonido.setVisible(true);
        btnPausaInterfaz.setVisible(true); 
        
        ballXSpeed = (Math.random() > 0.5) ? VELOCIDAD_INICIAL_X : -VELOCIDAD_INICIAL_X;
        ballYSpeed = (Math.random() > 0.5) ? VELOCIDAD_INICIAL_Y : -VELOCIDAD_INICIAL_Y;

        ballX = WIDTH / 2 - BALL_SIZE / 2;
        ballY = HEIGHT / 2 - BALL_SIZE / 2;
        waiting = true;
        waitFrames = 0;
        
        timer.start(); 
        repaint();
    }

    
    private void alternarPausaJuego() {
        juegoPausado = !juegoPausado;
        if (juegoPausado) {
            timer.stop(); 
            btnSonido.setVisible(false);
            btnPausaInterfaz.setVisible(false); 
            panelBotonesPausa.setVisible(true); 
            wPressed = false; sPressed = false; upPressed = false; downPressed = false;
        } else {
            panelBotonesPausa.setVisible(false);
            btnSonido.setVisible(true);
            btnPausaInterfaz.setVisible(true); 
            timer.start(); 
        }
        repaint();
        this.requestFocusInWindow();
    }

    private void inicializarBotónSonido() {
        btnSonido = new JButton("AUDIO: ACTIVADO");
        btnSonido.setBounds((WIDTH / 2) - 210, HEIGHT - 55, 200, 40);
        btnSonido.setFont(new Font("Consolas", Font.BOLD, 14)); 
        btnSonido.setBackground(new Color(20, 20, 30));
        btnSonido.setForeground(Color.CYAN);
        btnSonido.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 2)); 
        btnSonido.setFocusable(false); 
        btnSonido.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSonido.setVisible(false); 

        btnSonido.addActionListener(e -> {
            sonidoActivado = !sonidoActivado;
            if (sonidoActivado) {
                btnSonido.setText("AUDIO: ACTIVADO");
                btnSonido.setForeground(Color.CYAN);
                btnSonido.setBorder(javax.swing.BorderFactory.createLineBorder(Color.CYAN, 2));
            } else {
                btnSonido.setText("AUDIO: DESACTIVADO");
                btnSonido.setForeground(Color.LIGHT_GRAY);
                btnSonido.setBorder(javax.swing.BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
            }
            repaint();
            requestFocusInWindow(); 
        });

        this.add(btnSonido);
    }

    private void inicializarBotónPausaInterfaz() {
        btnPausaInterfaz = new JButton("PAUSAR PARTIDA");
        btnPausaInterfaz.setBounds((WIDTH / 2) + 10, HEIGHT - 55, 200, 40);
        btnPausaInterfaz.setFont(new Font("Consolas", Font.BOLD, 14));
        btnPausaInterfaz.setBackground(new Color(20, 20, 30));
        btnPausaInterfaz.setForeground(Color.YELLOW); 
        btnPausaInterfaz.setBorder(javax.swing.BorderFactory.createLineBorder(Color.YELLOW, 2)); 
        btnPausaInterfaz.setFocusable(false);
        btnPausaInterfaz.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnPausaInterfaz.setVisible(false);

        btnPausaInterfaz.addActionListener(e -> alternarPausaJuego());

        this.add(btnPausaInterfaz);
    }

    private void inicializarBotoneraPausa() {
        panelBotonesPausa = new JPanel(new GridLayout(1, 3, 15, 0));
        panelBotonesPausa.setOpaque(false);
        
        int panelW = 560; int panelH = 40;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = 270; 
        panelBotonesPausa.setBounds(panelX, panelY, panelW, panelH);

        btnReanudarPausa = new JButton("REANUDAR PARTIDA");
        estilizarBotonInterface(btnReanudarPausa, Color.GREEN);
        btnReanudarPausa.addActionListener(e -> alternarPausaJuego());

        btnReiniciarPausa = new JButton("REINICIAR PARTIDA");
        estilizarBotonInterface(btnReiniciarPausa, Color.YELLOW);
        btnReiniciarPausa.addActionListener(e -> {
            panelBotonesPausa.setVisible(false);
            juegoPausado = false;
            reiniciarPartidaCompleta();
        });

        btnSalirPausa = new JButton("SALIR AL MENÚ");
        estilizarBotonInterface(btnSalirPausa, new Color(255, 80, 80));
        btnSalirPausa.addActionListener(e -> salirAlMenuPrincipal());

        panelBotonesPausa.add(btnReanudarPausa);
        panelBotonesPausa.add(btnReiniciarPausa);
        panelBotonesPausa.add(btnSalirPausa);
        panelBotonesPausa.setVisible(false);
        this.add(panelBotonesPausa);
    }

    private void inicializarBotoneraFinal() {
        panelBotonesFinal = new JPanel(new GridLayout(1, 3, 15, 0)); 
        panelBotonesFinal.setOpaque(false);
        
        int panelW = 580; int panelH = 42;
        int panelX = (WIDTH - panelW) / 2;
        int panelY = 380; 
        panelBotonesFinal.setBounds(panelX, panelY, panelW, panelH);

        btnReiniciar = new JButton("REINTENTAR MODO");
        estilizarBotonInterface(btnReiniciar, Color.GREEN);
        btnReiniciar.addActionListener(e -> {
            if (contraMaquina) {
                estadoMenu = 1; 
                panelBotonesFinal.setVisible(false);
                gameFinished = false;
                if (timerParpadeo != null) timerParpadeo.start();
                repaint();
            } else {
                reiniciarPartidaCompleta(); 
            }
        });

        JButton btnCambiarModo = new JButton("CAMBIAR DE MODO");
        estilizarBotonInterface(btnCambiarModo, Color.CYAN);
        btnCambiarModo.addActionListener(e -> {
            estadoMenu = 0; 
            panelBotonesFinal.setVisible(false);
            gameFinished = false;
            if (timerParpadeo != null) timerParpadeo.start();
            repaint();
        });

        btnSalir = new JButton("VOLVER AL MENU");
        estilizarBotonInterface(btnSalir, new Color(255, 80, 80));
        btnSalir.addActionListener(e -> salirAlMenuPrincipal());

        panelBotonesFinal.add(btnReiniciar);
        panelBotonesFinal.add(btnCambiarModo);
        panelBotonesFinal.add(btnSalir);
        panelBotonesFinal.setVisible(false); 
        this.add(panelBotonesFinal);
    }

    private void estilizarBotonInterface(JButton b, Color accentColor) {
        b.setBackground(new Color(40, 40, 50));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Consolas", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120), 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                b.setBackground(new Color(55, 55, 68)); 
                b.setBorder(new LineBorder(accentColor, 1));
            }
            public void mouseExited(MouseEvent e) { 
                b.setBackground(new Color(40, 40, 50)); 
                b.setBorder(new LineBorder(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 120), 1));
            }
        });
    }

    private void reiniciarPartidaCompleta() {
        p1Y = 210; p2Y = 210;
        score1 = 0; score2 = 0;
        timeLeft = 60;
        frameCounter = 0;
        waiting = false;
        waitFrames = 0;
        gameFinished = false;
        panelBotonesFinal.setVisible(false);
        panelBotonesPausa.setVisible(false);
        resetBall();
        timer.start();
        this.requestFocusInWindow();
    }

    private void salirAlMenuPrincipal() {
        detenerJuego();
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) {
            topFrame.dispose();
        }
    }

    public void detenerJuego() {
        if (timerParpadeo != null) timerParpadeo.stop();
        if (timer != null) timer.stop();
    }

    private void playSound(String fileName) {
        if (!sonidoActivado || estadoMenu < 3 || juegoPausado) return; 
        try {
            File soundPath = new File("res/" + fileName);
            if (soundPath.exists()) {
                AudioInputStream audioInput = AudioSystem.getAudioInputStream(soundPath);
                Clip clip = AudioSystem.getClip();
                clip.open(audioInput);
                clip.start();
            }
        } catch (Exception e) {
            System.err.println("Error sonido: " + fileName);
        }
    }

    /**
     * Aplica el multiplicador de velocidad dinámico sobre los vectores de movimiento.
     */
    private void acelerarPelota() {
        if (Math.abs(ballXSpeed) < MAX_VELOCIDAD) {
            ballXSpeed = (ballXSpeed > 0) ? ballXSpeed + 1 : ballXSpeed - 1;
        }
        if (Math.abs(ballYSpeed) < MAX_VELOCIDAD) {
            ballYSpeed = (ballYSpeed > 0) ? ballYSpeed + 1 : ballYSpeed - 1;
        }
    }

    /**
     * Ejecuta el cálculo secuencial de la física, entradas e Inteligencia Artificial.
     */
    private void update() {
        if (estadoMenu < 3 || juegoPausado || gameFinished) return; 

        if (wPressed && p1Y > 0) p1Y -= 5;
        if (sPressed && p1Y < HEIGHT - PADDLE_HEIGHT) p1Y += 5;

        if (contraMaquina) {
            int centroPalaIA = p2Y + (PADDLE_HEIGHT / 2);
            int velocidadIA = 0; int margenError = 0;
            int velocidadActualBola = Math.abs(ballXSpeed);

            if (nivelDificultad == 1) { 
                velocidadIA = 3; margenError = 45; 
            } 
            else if (nivelDificultad == 2) { 
                velocidadIA = Math.min(velocidadActualBola - 1, 5); 
                margenError = 20; 
            } 
            else if (nivelDificultad == 3) { 
                velocidadIA = velocidadActualBola; 
                margenError = 8;  
            }

            if (ballXSpeed > 0 && ballX > WIDTH / 2) { 
                if (ballY < centroPalaIA - margenError && p2Y > 0) p2Y -= velocidadIA;
                else if (ballY > centroPalaIA + margenError && p2Y < HEIGHT - PADDLE_HEIGHT) p2Y += velocidadIA;
            }
        } else {
            if (upPressed && p2Y > 0) p2Y -= 5;
            if (downPressed && p2Y < HEIGHT - PADDLE_HEIGHT) p2Y += 5;
        }

        if (waiting) {
            waitFrames++;
            if (waitFrames >= 200) { waiting = false; waitFrames = 0; }
            return; 
        }

        frameCounter++;
        if (frameCounter >= 100) { 
            timeLeft--; frameCounter = 0;
            if (timeLeft <= 0) {
                gameFinished = true; timer.stop();
                panelBotonesFinal.setVisible(true);
            }
        }

        ballX += ballXSpeed; ballY += ballYSpeed;

        if (ballY <= 0 || ballY >= HEIGHT - BALL_SIZE) {
            ballYSpeed *= -1; acelerarPelota(); playSound("choque.wav"); 
        }

        if (ballX >= 20 && ballX <= 35 && ballY + BALL_SIZE >= p1Y && ballY <= p1Y + PADDLE_HEIGHT && ballXSpeed < 0) {
            ballXSpeed = Math.abs(ballXSpeed); ballX = 36; 
            acelerarPelota(); playSound("choque.wav"); 
        }

        if (ballX >= WIDTH - 50 && ballX <= WIDTH - 35 && ballY + BALL_SIZE >= p2Y && ballY <= p2Y + PADDLE_HEIGHT && ballXSpeed > 0) {
            ballXSpeed = -Math.abs(ballXSpeed); ballX = WIDTH - 51; 
            acelerarPelota(); playSound("choque.wav"); 
        }

        if (ballX < -BALL_SIZE) { score2++; playSound("gol.wav"); resetBall(); }
        else if (ballX > WIDTH) { score1++; playSound("gol.wav"); resetBall(); }
    }

    private void resetBall() {
        if (estadoMenu < 3) return;
        ballX = WIDTH / 2 - BALL_SIZE / 2;
        ballY = HEIGHT / 2 - BALL_SIZE / 2;
        
        ballXSpeed = (ballXSpeed > 0) ? -VELOCIDAD_INICIAL_X : VELOCIDAD_INICIAL_X;
        ballYSpeed = (ballYSpeed > 0) ? VELOCIDAD_INICIAL_Y : -VELOCIDAD_INICIAL_Y;

        waiting = true; waitFrames = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelW = getWidth(); int panelH = getHeight();

        if (estadoMenu == 0) {
            g2d.setPaint(new GradientPaint(0, 0, new Color(10, 10, 20), 0, panelH, new Color(20, 20, 40)));
            g2d.fillRect(0, 0, panelW, panelH);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 36)); g2d.setColor(Color.CYAN);
            g2d.drawString("PONG RETRO MULTIMODO", 190, 110);
            g2d.setColor(new Color(0, 255, 255, 100)); g2d.drawLine(100, 150, panelW - 100, 150);
            g2d.setFont(new Font("Consolas", Font.BOLD, 18));
            g2d.setColor(new Color(30, 30, 45)); g2d.fillRoundRect(150, 200, 500, 50, 12, 12);
            g2d.setColor(new Color(0, 255, 255, 120)); g2d.drawRoundRect(150, 200, 500, 50, 12, 12);
            g2d.setColor(Color.WHITE); g2d.drawString("PULSA [1] -> MODO 2 JUGADORES (LOCAL)", 210, 232);
            g2d.setColor(new Color(30, 30, 45)); g2d.fillRoundRect(150, 280, 500, 50, 12, 12);
            g2d.setColor(new Color(255, 0, 255, 120)); g2d.drawRoundRect(150, 280, 500, 50, 12, 12);
            g2d.setColor(Color.WHITE); g2d.drawString("PULSA [2] -> VS LA MÁQUINA (IA)", 240, 312);
            if (textoVisible) {
                g2d.setFont(new Font("Consolas", Font.PLAIN, 14)); g2d.setColor(Color.YELLOW);
                String instruct = "POR FAVOR, SELECCIONA UN MODO EN EL TECLADO";
                g2d.drawString(instruct, (panelW - g2d.getFontMetrics().stringWidth(instruct)) / 2, 410);
            }
            return;
        }

        if (estadoMenu == 1) {
            g2d.setPaint(new GradientPaint(0, 0, new Color(5, 15, 10), 0, panelH, new Color(15, 35, 20)));
            g2d.fillRect(0, 0, panelW, panelH);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 32)); g2d.setColor(Color.GREEN);
            g2d.drawString("CONFIGURAR DIFICULTAD IA", 190, 110);
            g2d.setColor(new Color(0, 255, 0, 100)); g2d.drawLine(120, 150, panelW - 120, 150);
            g2d.setFont(new Font("Consolas", Font.BOLD, 16));
            g2d.setColor(new Color(25, 35, 25)); g2d.fillRoundRect(200, 190, 400, 42, 10, 10);
            g2d.setColor(Color.GREEN); g2d.drawRoundRect(200, 190, 400, 42, 10, 10);
            g2d.setColor(Color.WHITE); g2d.drawString("[1] MODO FÁCIL (ENTRENAMIENTO)", 245, 216);
            g2d.setColor(new Color(25, 35, 25)); g2d.fillRoundRect(200, 250, 400, 42, 10, 10);
            g2d.setColor(Color.ORANGE); g2d.drawRoundRect(200, 250, 400, 42, 10, 10);
            g2d.setColor(Color.WHITE); g2d.drawString("[2] MODO MEDIO (RETO ARCADE)", 255, 276);
            g2d.setColor(new Color(25, 35, 25)); g2d.fillRoundRect(200, 310, 400, 42, 10, 10);
            g2d.setColor(Color.RED); g2d.drawRoundRect(200, 310, 400, 42, 10, 10);
            g2d.setColor(Color.WHITE); g2d.drawString("[3] MODO EXPERTO (IMBATIBLE)", 255, 336);
            if (textoVisible) {
                g2d.setFont(new Font("Consolas", Font.PLAIN, 14)); g2d.setColor(Color.GREEN);
                String msg = "PULSA [1], [2] O [3] PARA INYECTAR LA INTELIGENCIA";
                g2d.drawString(msg, (panelW - g2d.getFontMetrics().stringWidth(msg)) / 2, 420);
            }
            return;
        }

        if (estadoMenu == 2) {
            g2d.setColor(new Color(15, 15, 18)); g2d.fillRect(0, 0, panelW, panelH);
            if (imgControles != null) {
                int imgW = imgControles.getWidth(this); int imgH = imgControles.getHeight(this);
                double scale = Math.min((double) (panelW - 80) / imgW, (double) (panelH - 120) / imgH);
                int targetW = (int) (imgW * scale); int targetH = (int) (imgH * scale);
                int renderX = (panelW - targetW) / 2; int renderY = (panelH - targetH) / 2 - 20;
                g2d.drawImage(imgControles, renderX, renderY, targetW, targetH, this);
                g2d.setColor(new Color(0, 255, 255, 80)); g2d.drawRect(renderX - 2, renderY - 2, targetW + 4, targetH + 4);
            } else {
                g2d.setColor(Color.CYAN); g2d.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2d.drawString("GUÍA DE CONTROLES - MODO 2 JUGADORES", 50, 150);
            }
            if (textoVisible) {
                g2d.setFont(new Font("Consolas", Font.BOLD, 14)); g2d.setColor(Color.GREEN);
                FontMetrics fm = g2d.getFontMetrics(); String msgInicio = "PULSA CUALQUIER TECLA PARA EMPEZAR LA BATALLA";
                g2d.drawString(msgInicio, (panelW - fm.stringWidth(msgInicio)) / 2, panelH - 50);
            }
            return; 
        }

        g2d.setColor(Color.DARK_GRAY);
        for(int i=0; i<HEIGHT; i+=20) g2d.drawLine(WIDTH/2, i, WIDTH/2, i+10);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(20, p1Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        g2d.fillRect(WIDTH - 35, p2Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        
        if (!gameFinished) {
            if (Math.abs(ballXSpeed) > 8) g2d.setColor(Color.RED);
            else if (Math.abs(ballXSpeed) > 5) g2d.setColor(Color.ORANGE);
            else g2d.setColor(Color.WHITE);
            g2d.fillOval(ballX, ballY, BALL_SIZE, BALL_SIZE);
        }

        g2d.setColor(Color.WHITE); g2d.setFont(new Font("Consolas", Font.BOLD, 40));
        g2d.drawString(score1 + " - " + score2, WIDTH/2 - 60, 50);
        
        g2d.setFont(new Font("Consolas", Font.PLAIN, 16)); g2d.setColor(Color.GRAY);
        g2d.drawString("Tiempo: " + timeLeft + "s", WIDTH/2 - 50, 80);
        
        g2d.setFont(new Font("Consolas", Font.PLAIN, 12)); g2d.setColor(new Color(0, 255, 255, 130));
        g2d.drawString("TURBO: x" + (Math.abs(ballXSpeed) - VELOCIDAD_INICIAL_X + 1), WIDTH/2 - 35, 105);

        g2d.setFont(new Font("Consolas", Font.PLAIN, 11)); g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("[P] PAUSA", 15, 20);

        if (waiting && !gameFinished) {
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 18)); g2d.setColor(Color.CYAN);
            FontMetrics fm = g2d.getFontMetrics(); String textGol = " PREPARANDO SAQUE...";
            g2d.drawString(textGol, (WIDTH - fm.stringWidth(textGol)) / 2, HEIGHT - 80);
        }

        if (juegoPausado) {
            g2d.setColor(new Color(10, 10, 15, 220)); 
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            int boxW = 600; int boxH = 200;
            int boxX = (WIDTH - boxW) / 2; int boxY = (HEIGHT - boxH) / 2 - 30;

            g2d.setColor(new Color(20, 20, 30));
            g2d.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(boxX, boxY, boxW, boxH, 15, 15);
            g2d.setStroke(new BasicStroke(1f));

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
            FontMetrics fm = g2d.getFontMetrics();
            String titlePausa = "PARTIDA EN PAUSA";
            g2d.drawString(titlePausa, boxX + (boxW - fm.stringWidth(titlePausa)) / 2, boxY + 55);

            g2d.setFont(new Font("Consolas", Font.PLAIN, 13));
            g2d.setColor(Color.LIGHT_GRAY);
            String subPausa = "El cronómetro y los motores lógicos se han congelado temporalmente.";
            g2d.drawString(subPausa, boxX + (boxW - g2d.getFontMetrics().stringWidth(subPausa)) / 2, boxY + 90);
        }

        if (gameFinished) {
            btnSonido.setVisible(false);
            btnPausaInterfaz.setVisible(false); 
            g2d.setColor(new Color(10, 10, 15, 230)); g2d.fillRect(0, 0, WIDTH, HEIGHT);

            int cardW = 620; int cardH = 340;
            int cardX = (WIDTH - cardW) / 2; int cardY = (HEIGHT - cardH) / 2 - 20;

            g2d.setColor(new Color(0, 0, 0, 160)); g2d.fillRoundRect(cardX + 6, cardY + 6, cardW, cardH, 18, 18);
            g2d.setPaint(new GradientPaint(cardX, cardY, new Color(25, 25, 35), cardX, cardY + cardH, new Color(15, 15, 20)));
            g2d.fillRoundRect(cardX, cardY, cardW, cardH, 18, 18);

            String mensaje; Color accentColor;
            if (contraMaquina) {
                if (score1 > score2) { mensaje = "¡VICTORIA ABSOLUTA SOBRE LA IA!"; accentColor = Color.GREEN; } 
                else if (score2 > score1) { mensaje = "¡DERROTA! LA MÁQUINA HA GANADO"; accentColor = Color.RED; } 
                else { mensaje = "¡EMPATE TÉCNICO CON EL CIRCUITO!"; accentColor = Color.YELLOW; }
            } else {
                if (score1 > score2) { mensaje = "¡VICTORIA JUGADOR 1!"; accentColor = Color.GREEN; } 
                else if (score2 > score1) { mensaje = "¡VICTORIA JUGADOR 2!"; accentColor = Color.GREEN; } 
                else { mensaje = "¡EMPATE TÉCNICO!"; accentColor = Color.YELLOW; }
            }

            g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 180));
            g2d.setStroke(new BasicStroke(2f)); g2d.drawRoundRect(cardX, cardY, cardW, cardH, 18, 18);
            g2d.setStroke(new BasicStroke(1f));

            FontMetrics fm; g2d.setFont(new Font("Segoe UI", Font.BOLD, 30));
            fm = g2d.getFontMetrics(); int msgX = cardX + (cardW - fm.stringWidth(mensaje)) / 2;
            g2d.setColor(accentColor); g2d.drawString(mensaje, msgX, cardY + 55);

            g2d.setColor(new Color(0, 255, 255, 80)); g2d.drawLine(cardX + 40, cardY + 85, cardX + cardW - 40, cardY + 85);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 16)); fm = g2d.getFontMetrics();

            g2d.setColor(Color.CYAN); g2d.drawString("PUNTUACIÓN JUGADOR 1 (TÚ):", cardX + 50, cardY + 135);
            g2d.setColor(Color.WHITE); String pts1 = score1 + " GOLES"; g2d.drawString(pts1, cardX + cardW - 50 - fm.stringWidth(pts1), cardY + 135);

            g2d.setColor(Color.CYAN); String txtRival = contraMaquina ? "PUNTUACIÓN MÁQUINA (IA):" : "PUNTUACIÓN JUGADOR 2:";
            g2d.drawString(txtRival, cardX + 50, cardY + 180);
            g2d.setColor(Color.WHITE); String pts2 = score2 + " GOLES"; g2d.drawString(pts2, cardX + cardW - 50 - fm.stringWidth(pts2), cardY + 180);

            g2d.setColor(new Color(255, 255, 255, 30)); g2d.drawLine(cardX + 40, cardY + 215, cardX + cardW - 40, cardY + 215);
            g2d.setFont(new Font("Consolas", Font.BOLD, 13)); g2d.setColor(Color.YELLOW);
            String pregunta = "¿QUÉ OPERACIÓN DESEAS REALIZAR AHORA?";
            g2d.drawString(pregunta, cardX + (cardW - g2d.getFontMetrics().stringWidth(pregunta)) / 2, cardY + 245);
        } else {
            if (estadoMenu == 3 && !juegoPausado) {
                btnSonido.setVisible(true); 
                btnPausaInterfaz.setVisible(true); 
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        update();
        repaint();
    }
}