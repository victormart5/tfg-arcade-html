package com.retro.games.catchgame;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.io.File;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;

public class CatchGame extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final int WIDTH = 800;
    private final int HEIGHT = 625;

    private int basketX = 350;
    private final int basketY = 520;
    private int currentBasketWidth = 100; 
    private final int basketHeight = 30; 
    private final int basketSpeed = 15;

    private class Particle {
        double x, y, vx, vy;
        Color color;
        int vida;
        Particle(double x, double y, double vx, double vy, Color color) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.color = color;
            this.vida = 20; 
        }
    }
    private ArrayList<Particle> particles = new ArrayList<>();

    private class FloatingText {
        String texto;
        int x, y, vida;
        Color color;
        FloatingText(String texto, int x, int y, Color color) {
            this.texto = texto; this.x = x; this.y = y; this.color = color;
            this.vida = 45; 
        }
    }
    private ArrayList<FloatingText> floatingTexts = new ArrayList<>();

    private class Drop {
        double x, y; 
        double speedBase;     
        double velocidadY;    
        int points;
        Color color;
        String tipo;          
        String tipoVisual;    
        double anguloViento;  
        int contadorCamuflaje;
        int duracionExplosion = 25; 
        
        Drop(int x, int y, int speed, int points, Color color, String tipo) {
            this.x = x; this.y = y;
            this.speedBase = speed;
            this.velocidadY = speed; 
            this.points = points;
            this.color = color;
            this.tipo = tipo;
            this.tipoVisual = tipo; 
            this.anguloViento = random.nextDouble() * Math.PI * 2;
            this.contadorCamuflaje = random.nextInt(30);
        }
    }
    private ArrayList<Drop> drops = new ArrayList<>();
    private Random random = new Random();

    private int score = 0;
    private int lives = 3;
    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private int comboActual = 0;
    private int comboMaximo = 0;

    private int maxRecordGlobalCache = 0;

    private Rectangle btnPauseIconRect = new Rectangle(730, 20, 40, 40); 
    private Rectangle btnMuteRect = new Rectangle(680, 20, 40, 40);
    private Rectangle btnResumeRect = new Rectangle(200, 320, 180, 45);     
    private Rectangle btnRestartRect = new Rectangle(420, 320, 180, 45);    
    private Rectangle btnRetryRect = new Rectangle(200, 420, 180, 45);      
    private Rectangle btnExitRect = new Rectangle(420, 420, 180, 45);       

    private boolean hoverPauseIcon = false;
    private boolean hoverMute = false;
    private boolean hoverResume = false;
    private boolean hoverRestart = false;
    private boolean hoverRetry = false;
    private boolean hoverExit = false;

    private boolean silenciado = false;

    private Timer gameTimer;
    private Timer spawnTimer;
    private Timer timerViento; 
    private Timer timerMagnetico; 

    private Usuario jugadorActual;
    private UsuarioRepository repo;

    private boolean mostrarControles = true;
    private Timer timerParpadeo;
    private boolean textoVisible = true;

    private Image imgCorazon;
    private Image imgPlatano;
    private Image imgCereza;
    private Image imgManzana;
    private Image imgBomba;
    private Image imgCesta; 
    private Image imgBombaExplotando; 

    private double fuerzaVientoGlobal = 0.0;
    private String direccionVientoTexto = "WIND: CALM";
    
    private boolean controlesInvertidos = false;
    private boolean parpadeoAlertaMagnetica = false;

    private Clip clipComida;
    private Clip clipExplosion;
    private Clip clipDerrota;

    public CatchGame(Usuario jugador, UsuarioRepository repo) {
        this.jugadorActual = jugador;
        this.repo = repo;

        this.setLayout(null);
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(15, 15, 22));
        this.setFocusable(true);

        if (repo != null) {
            try {
                java.util.List<Usuario> todosLosOperadores = repo.findAll();
                for (Usuario u : todosLosOperadores) {
                    if (u != null && u.getPuntos_catch() > maxRecordGlobalCache) {
                        maxRecordGlobalCache = u.getPuntos_catch();
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error procesando record global en persistencia");
            }
        }

        File fCorazon = new File("res/corazonVida.png");
        if (!fCorazon.exists()) fCorazon = new File("res/corazonVida.jpg");
        if (fCorazon.exists()) {
            imgCorazon = new ImageIcon(fCorazon.getAbsolutePath()).getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
        }

        imgPlatano = cargarYEscalarImagen("Platano", 36, 36);
        imgCereza = cargarYEscalarImagen("Cereza", 36, 36);
        imgManzana = cargarYEscalarImagen("Manzana", 36, 36);
        imgBomba = cargarYEscalarImagen("Bomba", 36, 36);
        imgBombaExplotando = cargarYEscalarImagen("BombaExplotando", 46, 46); 

        File fCesta = new File("res/Cesta.png");
        if (!fCesta.exists()) fCesta = new File("res/Cesta.jpg");
        if (fCesta.exists()) {
            imgCesta = new ImageIcon(fCesta.getAbsolutePath()).getImage();
        }

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                
                if (mostrarControles) {
                    mostrarControles = false;
                    timerParpadeo.stop();
                    iniciarPartida();
                    return;
                }
                
                if (key == KeyEvent.VK_P || key == KeyEvent.VK_ESCAPE) {
                    if (!gameOver && running) {
                        togglePausa();
                    }
                    return;
                }
                
                if (gameOver || paused) return;

                int direccion = controlesInvertidos ? -1 : 1;

                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) {
                    basketX = Math.max(0, basketX - (basketSpeed * direccion));
                    ajustarLimitesCesta();
                }
                if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) {
                    basketX = Math.min(WIDTH - currentBasketWidth, basketX + (basketSpeed * direccion));
                    ajustarLimitesCesta();
                }
                repaint();
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point puntoClic = e.getPoint();
                
                if (running && !gameOver && !paused && btnPauseIconRect.contains(puntoClic)) {
                    togglePausa();
                    return;
                }

                if (running && !gameOver && !paused && btnMuteRect.contains(puntoClic)) {
                    silenciado = !silenciado;
                    if (silenciado) {
                        detenerSonidosEnCurso();
                    }
                    repaint();
                    return;
                }

                if (paused && !gameOver) {
                    if (btnResumeRect.contains(puntoClic)) {
                        togglePausa();
                    }
                    if (btnRestartRect.contains(puntoClic)) {
                        iniciarPartida();
                    }
                    return;
                }

                if (gameOver) {
                    if (btnRetryRect.contains(puntoClic)) {
                        iniciarPartida();
                    }
                    if (btnExitRect.contains(puntoClic)) {
                        detenerJuego();
                        detenerSonidosEnCurso();
                        SwingUtilities.windowForComponent(CatchGame.this).dispose();
                    }
                }
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point puntoRaton = e.getPoint();
                if (running && !gameOver && !paused) {
                    hoverPauseIcon = btnPauseIconRect.contains(puntoRaton);
                    hoverMute = btnMuteRect.contains(puntoRaton);
                }
                if (paused && !gameOver) {
                    hoverResume = btnResumeRect.contains(puntoRaton);
                    hoverRestart = btnRestartRect.contains(puntoRaton);
                }
                if (gameOver) {
                    hoverRetry = btnRetryRect.contains(puntoRaton);
                    hoverExit = btnExitRect.contains(puntoRaton);
                }
                repaint();
            }
        });

        timerParpadeo = new Timer(400, e -> {
            textoVisible = !textoVisible;
            if (controlesInvertidos || paused) {
                parpadeoAlertaMagnetica = !parpadeoAlertaMagnetica;
            }
            repaint();
        });
        timerParpadeo.start();
    }

    private void ajustarLimitesCesta() {
        if (basketX < 0) basketX = 0;
        if (basketX > WIDTH - currentBasketWidth) basketX = WIDTH - currentBasketWidth;
    }

    private Image cargarYEscalarImagen(String nombreArchivo, int w, int h) {
        File f = new File("res/" + nombreArchivo + ".png");
        if (!f.exists()) f = new File("res/" + nombreArchivo + ".jpg");
        
        if (f.exists()) {
            return new ImageIcon(f.getAbsolutePath()).getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        }
        return null; 
    }

    private void detenerSonidosEnCurso() {
        if (clipComida != null && clipComida.isRunning()) clipComida.stop();
        if (clipExplosion != null && clipExplosion.isRunning()) clipExplosion.stop();
        if (clipDerrota != null && clipDerrota.isRunning()) clipDerrota.stop();
    }

    private void reproducirSonidoComida() {
        if (silenciado) return; 
        new Thread(() -> {
            try {
                File archivoAudio = new File("res/comida.wav");
                if (archivoAudio.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(archivoAudio);
                    clipComida = AudioSystem.getClip();
                    clipComida.open(ais);
                    if (!silenciado) clipComida.start();
                }
            } catch (Exception ex) {
                System.err.println("Error al reproducir comida.wav: " + ex.getMessage());
            }
        }).start();
    }

    private void reproducirSonidoExplosion() {
        if (silenciado) return; 
        new Thread(() -> {
            try {
                File archivoAudio = new File("res/Explosion.wav");
                if (!archivoAudio.exists()) archivoAudio = new File("res/explosion.wav");
                
                if (archivoAudio.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(archivoAudio);
                    clipExplosion = AudioSystem.getClip();
                    clipExplosion.open(ais); 
                    if (!silenciado) clipExplosion.start();
                }
            } catch (Exception ex) {
                System.err.println("Error al reproducir el audio de explosión: " + ex.getMessage());
            }
        }).start();
    }
    
    private void reproducirSonidoDerrota() {
        if (silenciado) return; 
        new Thread(() -> {
            try {
                File archivoAudio = new File("res/derrota.wav");
                if (!archivoAudio.exists()) archivoAudio = new File("res/Derrota.wav");
                
                if (archivoAudio.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(archivoAudio);
                    clipDerrota = AudioSystem.getClip();
                    clipDerrota.open(ais);
                    if (!silenciado) clipDerrota.start();
                }
            } catch (Exception ex) {
                System.err.println("Error al reproducir el audio de derrota: " + ex.getMessage());
            }
        }).start();
    }

    private void togglePausa() {
        paused = !paused;
        if (paused) {
            if (spawnTimer != null) spawnTimer.stop();
            if (timerViento != null) timerViento.stop();
            if (timerMagnetico != null) timerMagnetico.stop();
        } else {
            if (spawnTimer != null) spawnTimer.start();
            if (timerViento != null) timerViento.start();
            if (timerMagnetico != null) timerMagnetico.start();
        }
        repaint();
    }

    private void iniciarPartida() {
        running = true;
        gameOver = false;
        paused = false;
        score = 0;
        lives = 3;
        comboActual = 0;
        comboMaximo = 0;
        currentBasketWidth = 100;
        fuerzaVientoGlobal = 0.0;
        direccionVientoTexto = "WIND: CALM";
        controlesInvertidos = false;
        drops.clear();
        particles.clear();
        floatingTexts.clear();
        basketX = 350;

        detenerSonidosEnCurso();

        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(16, this); 
        gameTimer.start();

        if (spawnTimer != null) spawnTimer.stop();
        spawnTimer = new Timer(600, e -> ejecutarSpawnProgresivo());
        spawnTimer.start();

        if (timerViento != null) timerViento.stop();
        timerViento = new Timer(5000, e -> cambiarRumboDelViento());
        timerViento.start();

        planificarSiguienteTormentaMagnetica();
        repaint();
    }

    private void planificarSiguienteTormentaMagnetica() {
        if (!running || gameOver) return;
        
        int tiempoEsperaAleatorio = (random.nextInt(12) + 10) * 1000;
        
        timerMagnetico = new Timer(tiempoEsperaAleatorio, e -> {
            timerMagnetico.stop();
            if (running && !gameOver) {
                controlesInvertidos = true;
                parpadeoAlertaMagnetica = true;
                
                Timer duracionAnomalia = new Timer(6000, ev -> {
                    controlesInvertidos = false;
                    parpadeoAlertaMagnetica = false;
                    planificarSiguienteTormentaMagnetica(); 
                });
                duracionAnomalia.setRepeats(false);
                duracionAnomalia.start();
            }
        });
        timerMagnetico.setRepeats(false);
        timerMagnetico.start();
    }

    private void ejecutarSpawnProgresivo() {
        if (!running || gameOver || paused) return;

        int dx = random.nextInt(WIDTH - 180) + 90; 
        int bonusVelocidad = score / 1500; 
        int velBase = random.nextInt(3) + 3 + bonusVelocidad; 

        int incrementoBombas = Math.min(30, (score / 1000) * 6); 
        int limiteBomba = 55 - incrementoBombas; 

        int RNG = random.nextInt(100);   

        if (RNG < 25) { 
            drops.add(new Drop(dx, 0, velBase, 100, Color.CYAN, "CEREZA"));
        } else if (RNG < 40) { 
            drops.add(new Drop(dx, 0, velBase + 1, 250, Color.YELLOW, "PLATANO"));
        } else if (RNG < limiteBomba) { 
            drops.add(new Drop(dx, 0, velBase - 1, 400, Color.MAGENTA, "MANZANA"));
        } else { 
            String subtipoBomba = (random.nextInt(10) < 4) ? "BOMBA_FALSA" : "BOMBA";
            drops.add(new Drop(dx, 0, velBase, 0, Color.RED, subtipoBomba));
        }
    }

    private void cambiarRumboDelViento() {
        if (!running || gameOver || score < 2000 || paused) return;

        double maxFuerza = Math.min(3.8, ((score - 2000) / 1000) * 0.5 + 1.2);
        fuerzaVientoGlobal = -maxFuerza + (random.nextDouble() * (maxFuerza * 2));

        if (fuerzaVientoGlobal < -0.4) {
            direccionVientoTexto = "WIND: << LEFT";
        } else if (fuerzaVientoGlobal > 0.4) {
            direccionVientoTexto = "WIND: RIGHT >>";
        } else {
            fuerzaVientoGlobal = 0.0; 
            direccionVientoTexto = "WIND: CALM";
        }
    }

    private void actualizarModificadoresDeDificultad() {
        if (spawnTimer == null) return;
        
        int nuevoDelay = Math.max(180, 600 - ((score / 1000) * 50));
        spawnTimer.setDelay(nuevoDelay);

        currentBasketWidth = Math.max(45, 100 - ((score / 1200) * 6));

        if (score < 2000) {
            fuerzaVientoGlobal = 0.0;
            direccionVientoTexto = "WIND: CALM";
        }
    }
    
    private void crearEstallidoParticulas(double x, double y, Color colorBase) {
        for (int i = 0; i < 8; i++) {
            double vx = -2.0 + (random.nextDouble() * 4.0);
            double vy = -4.0 - (random.nextDouble() * 3.0); 
            particles.add(new Particle(x, y, vx, vy, colorBase));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running || gameOver || paused) return; 

        double valorGravedadPorFotograma = 0.12 + (score / 4000) * 0.03;

        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.y -= 1; 
            ft.vida--;
            if (ft.vida <= 0) floatingTexts.remove(i);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.vy += 0.2; 
            p.x += p.vx;
            p.y += p.vy;
            p.vida--;
            if (p.vida <= 0) particles.remove(i);
        }

        for (int i = drops.size() - 1; i >= 0; i--) {
            Drop d = drops.get(i);
            
            if ("EXPLOSION".equals(d.tipo)) {
                d.duracionExplosion--;
                if (d.duracionExplosion <= 0) drops.remove(i);
                continue;
            }

            d.velocidadY += valorGravedadPorFotograma;
            d.y += d.velocidadY;
            
            d.anguloViento += 0.05;
            d.x += fuerzaVientoGlobal + (Math.sin(d.anguloViento) * 0.4);

            if (d.x < 5) d.x = 5;
            if (d.x > WIDTH - 45) d.x = WIDTH - 45; 

            if ("BOMBA_FALSA".equals(d.tipo)) {
                d.contadorCamuflaje++;
                if (d.y < 360) {
                    if (d.contadorCamuflaje % 15 == 0) {
                        int rFruta = random.nextInt(3);
                        d.tipoVisual = (rFruta == 0) ? "CEREZA" : (rFruta == 1) ? "PLATANO" : "MANZANA";
                    }
                } else {
                    d.tipoVisual = "BOMBA"; 
                }
            }

            if (d.y + 36 >= basketY && d.y <= basketY + basketHeight &&
                d.x + 36 >= basketX && d.x <= (basketX + currentBasketWidth)) {
                
                if ("BOMBA".equals(d.tipo) || "BOMBA_FALSA".equals(d.tipo)) {
                    lives--; 
                    
                    if (comboActual >= 2) {
                        floatingTexts.add(new FloatingText("¡COMBO ROTO!", basketX, basketY - 20, Color.RED));
                    }
                    comboActual = 0; 
                    
                    reproducirSonidoExplosion(); 
                    
                    d.tipo = "EXPLOSION";        
                    d.x = d.x - 5;               
                    if (lives <= 0) finalizarJuego();
                } else {
                    comboActual++; 
                    if (comboActual > comboMaximo) comboMaximo = comboActual;
                    
                    if (comboActual >= 2) {
                        floatingTexts.add(new FloatingText("COMBO x" + comboActual + "!", (int)d.x, (int)d.y - 15, Color.GREEN));
                    }
                    
                    int bonificacionCombo = (comboActual > 1) ? (d.points * (comboActual - 1) / 10) : 0;
                    score += d.points + bonificacionCombo; 
                    
                    crearEstallidoParticulas(d.x + 18, d.y + 18, d.color);
                    reproducirSonidoComida(); 
                    actualizarModificadoresDeDificultad(); 
                    drops.remove(i);
                }
                continue;
            }

            if (d.y > HEIGHT - 90) {
                drops.remove(i);
                
                if (!"BOMBA".equals(d.tipo) && !"BOMBA_FALSA".equals(d.tipo)) {
                    if (comboActual >= 2) {
                        floatingTexts.add(new FloatingText("¡COMBO ROTO!", (int)d.x, HEIGHT - 110, Color.RED));
                    }
                    comboActual = 0; 
                    
                    int penalizacion = d.points / 2;
                    score = Math.max(0, score - penalizacion);
                    actualizarModificadoresDeDificultad(); 
                }
            }
        }
        running = lives > 0;
        repaint();
    }

    private void finalizarJuego() {
        gameOver = true;
        running = false;
        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        if (timerViento != null) timerViento.stop(); 
        if (timerMagnetico != null) timerMagnetico.stop(); 

        reproducirSonidoDerrota();
        
        if (jugadorActual != null) {
            try {
                if (score > jugadorActual.getPuntos_catch()) {
                    jugadorActual.setPuntos_catch(score);
                    repo.save(jugadorActual);
                }
            } catch (Exception ex) {
                System.err.println("Error de persistencia del repositorio: " + ex.getMessage());
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (score > maxRecordGlobalCache) {
            maxRecordGlobalCache = score;
        }

        if (mostrarControles) {
            g2d.setColor(new Color(25, 25, 40, 80));
            for (int x = 0; x < WIDTH; x += 40) g2d.drawLine(x, 0, x, HEIGHT);
            for (int y = 0; y < HEIGHT; y += 40) g2d.drawLine(0, y, WIDTH, y);

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
            g2d.setColor(new Color(0, 100, 150));
            g2d.drawString("CATCH OR DROP", 253, 93); 
            g2d.setColor(Color.CYAN);
            g2d.drawString("CATCH OR DROP", 250, 90); 

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 16));
            String strHighScore = "RÉCORD GLOBAL DEL SISTEMA: " + maxRecordGlobalCache + " PTS";
            g2d.drawString(strHighScore, (WIDTH - g2d.getFontMetrics().stringWidth(strHighScore)) / 2, 120);

            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            g2d.setColor(Color.WHITE);
            String ctrl = "CONTROLES: MOVER CESTA CON FLECHAS [<-] / [->] O TECLAS [A] / [D]";
            g2d.drawString(ctrl, (WIDTH - g2d.getFontMetrics().stringWidth(ctrl)) / 2, 155);

            g2d.setColor(new Color(40, 45, 60));
            g2d.fillRoundRect(100, 185, 600, 115, 10, 10); 
            g2d.setColor(Color.CYAN);
            g2d.drawRoundRect(100, 185, 600, 115, 10, 10);

            g2d.setFont(new Font("Consolas", Font.BOLD, 13));
            int startX_Labels = 140;
            int startX_Points = 300;
            int startX_Drop = 480;

            g2d.setColor(Color.WHITE); g2d.drawString("> CEREZA", startX_Labels, 215);
            g2d.setColor(Color.GREEN); g2d.drawString(":: +100 PTS", startX_Points, 215);
            g2d.setColor(Color.RED);   g2d.drawString(":: DEJA CAER: -50 PTS", startX_Drop, 215);

            g2d.setColor(Color.WHITE); g2d.drawString("> PLATANO", startX_Labels, 245);
            g2d.setColor(Color.GREEN); g2d.drawString(":: +250 PTS", startX_Points, 245);
            g2d.setColor(Color.RED);   g2d.drawString(":: DEJA CAER: -125 PTS", startX_Drop, 245);

            g2d.setColor(Color.WHITE); g2d.drawString("> MANZANA", startX_Labels, 275);
            g2d.setColor(Color.GREEN); g2d.drawString(":: +400 PTS", startX_Points, 275);
            g2d.setColor(Color.RED);   g2d.drawString(":: DEJA CAER: -200 PTS", startX_Drop, 275);

            int mechY = 335;
            int colEstadoX = 160;  
            int colTextoX = 260;   
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            
            String[][] mods = {
                {"[MOD]", "CESTA ADAPTATIVA       :: Se encoge conforme sube tu puntuación"},
                {"[MOD]", "TORMENTA DINÁMICA      :: El viento cambiará de rumbo a ambos lados"},
                {"[MOD]", "EFECTO GRAVEDAD        :: Los objetos aceleran rápido al caer"},
                {"[ALERTA]", "INCOHERENCIA MAGNÉTICA :: ¡Los controles se invertirán al azar!"},
                {"[BONUS]", "RACHAS DE COMBO        :: ¡Captura frutas seguidas para ganar bonus!"},
                {"[PELIGRO]", "BOMBAS MIMÉTICAS       :: Camufladas en el aire. Pierdes 1 vida"},
                {"[SISTEMA]", "SISTEMA DE PAUSA       :: Pulsa la tecla [P] o [ESC] en la partida"}
            };
            Color[] colors = {Color.ORANGE, Color.ORANGE, Color.ORANGE, Color.MAGENTA, Color.GREEN, Color.RED, Color.YELLOW};
            
            for(int i = 0; i < mods.length; i++) {
                g2d.setColor(colors[i]); g2d.drawString(mods[i][0], colEstadoX, mechY);
                g2d.setColor(Color.WHITE); g2d.drawString(mods[i][1], colTextoX, mechY);
                mechY += 28;
            }

            if (textoVisible) {
                g2d.setColor(Color.GREEN);
                g2d.setFont(new Font("Consolas", Font.BOLD, 14));
                String init = "--- PULSA CUALQUIER TECLA PARA INICIAR EL SISTEMA ---";
                g2d.drawString(init, (WIDTH - g2d.getFontMetrics().stringWidth(init)) / 2, 560);
            }
            
            g2d.setColor(new Color(0, 0, 0, 20));
            for (int yLim = 0; yLim < HEIGHT; yLim += 3) g2d.fillRect(0, yLim, WIDTH, 1);
            return;
        }

        g2d.setColor(new Color(255, 0, 0, 40));
        g2d.fillRect(0, HEIGHT - 90, WIDTH, 5);

        if (imgCesta != null) {
            Composite originalComposite = g2d.getComposite();
            if (controlesInvertidos) {
                g2d.setColor(new Color(255, 0, 255, parpadeoAlertaMagnetica ? 100 : 40));
                g2d.fillRoundRect(basketX, basketY, currentBasketWidth, basketHeight, 8, 8);
            }
            g2d.drawImage(imgCesta, basketX, basketY, currentBasketWidth, basketHeight, this);
            g2d.setComposite(originalComposite);
        } else {
            Color colorBordeCesta = controlesInvertidos ? new Color(255, 0, 255) : Color.WHITE;
            g2d.setPaint(new GradientPaint(basketX, basketY, Color.CYAN, basketX, basketY + basketHeight, 
                    controlesInvertidos ? new Color(130, 0, 130) : new Color(10, 50, 100)));
            g2d.fillRoundRect(basketX, basketY, currentBasketWidth, basketHeight, 10, 10);
            g2d.setColor(colorBordeCesta);
            g2d.drawRoundRect(basketX, basketY, currentBasketWidth - 1, basketHeight - 1, 10, 10);
        }

        for (Drop d : drops) {
            if ("EXPLOSION".equals(d.tipo)) {
                if (imgBombaExplotando != null) {
                    g2d.drawImage(imgBombaExplotando, (int)d.x, (int)d.y, this);
                } else {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillOval((int)d.x - 5, (int)d.y - 5, 46, 46);
                }
                continue;
            }

            Image imgActual = null;
            if ("BOMBA".equals(d.tipoVisual)) imgActual = imgBomba;
            else if ("PLATANO".equals(d.tipoVisual)) imgActual = imgPlatano;
            else if ("MANZANA".equals(d.tipoVisual)) imgActual = imgManzana;
            else if ("CEREZA".equals(d.tipoVisual)) imgActual = imgCereza;

            if (imgActual != null) {
                g2d.drawImage(imgActual, (int)d.x, (int)d.y, this);
            } else {
                if ("BOMBA".equals(d.tipoVisual)) {
                    g2d.setColor(Color.RED); g2d.fillOval((int)d.x, (int)d.y, 32, 32);
                } else {
                    g2d.setColor(d.color); g2d.fillOval((int)d.x, (int)d.y, 32, 32);
                    g2d.setColor(Color.WHITE); g2d.drawOval((int)d.x, (int)d.y, 31, 31);
                }
            }
        }

        for (Particle p : particles) {
            g2d.setColor(p.color);
            g2d.fillRect((int)p.x, (int)p.y, 5, 5);
        }

        g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        for (FloatingText ft : floatingTexts) {
            g2d.setColor(ft.color);
            g2d.drawString(ft.texto, ft.x, ft.y);
        }

        if (!gameOver && !paused) {
            g2d.setColor(hoverPauseIcon ? new Color(80, 80, 110, 200) : new Color(40, 45, 60, 150));
            g2d.fillRoundRect(btnPauseIconRect.x, btnPauseIconRect.y, btnPauseIconRect.width, btnPauseIconRect.height, 8, 8);
            g2d.setColor(Color.CYAN);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(btnPauseIconRect.x, btnPauseIconRect.y, btnPauseIconRect.width, btnPauseIconRect.height, 8, 8);
            
            g2d.setColor(Color.WHITE);
            g2d.fillRect(743, 31, 4, 18);
            g2d.fillRect(753, 31, 4, 18);

            g2d.setColor(hoverMute ? new Color(80, 80, 110, 200) : new Color(40, 45, 60, 150));
            g2d.fillRoundRect(btnMuteRect.x, btnMuteRect.y, btnMuteRect.width, btnMuteRect.height, 8, 8);
            g2d.setColor(Color.CYAN);
            g2d.drawRoundRect(btnMuteRect.x, btnMuteRect.y, btnMuteRect.width, btnMuteRect.height, 8, 8);

            g2d.setColor(Color.WHITE);
            if (!silenciado) {
                g2d.fillRect(690, 34, 6, 12); 
                int[] xPoints = {696, 704, 704, 696};
                int[] yPoints = {34, 28, 52, 46};
                g2d.fillPolygon(xPoints, yPoints, 4);
                g2d.drawArc(700, 30, 12, 20, -45, 90);
            } else {
                g2d.fillRect(690, 34, 6, 12); 
                int[] xPoints = {696, 704, 704, 696};
                int[] yPoints = {34, 28, 52, 46};
                g2d.fillPolygon(xPoints, yPoints, 4);
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(705, 34, 713, 46);
                g2d.drawLine(713, 34, 705, 46);
            }
        }

        g2d.setPaint(new GradientPaint(0, 550, new Color(24, 28, 43), 0, HEIGHT, new Color(14, 16, 26)));
        g2d.fillRect(0, 550, WIDTH, 75);
        g2d.setColor(Color.CYAN);
        g2d.drawRect(0, 550, WIDTH - 1, 74);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.drawString("SCORE: " + score, 40, 592);
        
        if (comboActual > 1) {
            g2d.setColor(Color.GREEN);
            g2d.drawString("COMBO: x" + comboActual, 180, 592);
        }
        
        if (controlesInvertidos) {
            if (parpadeoAlertaMagnetica) {
                g2d.setFont(new Font("Consolas", Font.BOLD, 16));
                g2d.setColor(new Color(255, 0, 255));
                String alertaTexto = "CONTROLES INVERTIDOS";
                int anchoAlerta = g2d.getFontMetrics().stringWidth(alertaTexto);
                g2d.drawString(alertaTexto, (WIDTH - anchoAlerta) / 2, 592); 
            }
        } else {
            g2d.setFont(new Font("Consolas", Font.BOLD, 16));
            g2d.setColor(fuerzaVientoGlobal < -0.4 ? Color.ORANGE : fuerzaVientoGlobal > 0.4 ? Color.YELLOW : Color.GRAY);
            int anchoViento = g2d.getFontMetrics().stringWidth(direccionVientoTexto);
            g2d.drawString(direccionVientoTexto, (WIDTH - anchoViento) / 2, 592); 
        }
        
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 18));
        g2d.drawString("VIDAS: ", WIDTH - 230, 592);
        int inicioCorazonesX = WIDTH - 160; 
        int posYCorazon = 574;             
        
        if (lives > 0) {
            for (int i = 0; i < lives; i++) {
                if (imgCorazon != null) {
                    g2d.drawImage(imgCorazon, inicioCorazonesX + (i * 26), posYCorazon, this);
                } else {
                    g2d.setColor(Color.RED); g2d.fillRect(inicioCorazonesX + (i * 26), posYCorazon + 2, 16, 16);
                }
            }
        } else {
            g2d.setColor(Color.RED); g2d.setFont(new Font("Consolas", Font.BOLD, 16));
            g2d.drawString("CRÍTICO", inicioCorazonesX, 592);
        }

        g2d.setColor(new Color(0, 0, 0, 25)); 
        for (int yLines = 0; yLines < HEIGHT; yLines += 3) {
            g2d.fillRect(0, yLines, WIDTH, 1); 
        }
        int vSize = 60;
        g2d.setPaint(new GradientPaint(0, 0, new Color(0,0,0,130), vSize, vSize, new Color(0,0,0,0)));
        g2d.fillRect(0, 0, vSize, vSize); 
        g2d.setPaint(new GradientPaint(WIDTH, 0, new Color(0,0,0,130), WIDTH - vSize, vSize, new Color(0,0,0,0)));
        g2d.fillRect(WIDTH - vSize, 0, vSize, vSize); 

        if (paused && !gameOver) {
            g2d.setColor(new Color(10, 10, 20, 200)); 
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            
            g2d.setColor(new Color(30, 35, 50));
            g2d.fillRoundRect(150, 140, 500, 280, 20, 20);
            g2d.setColor(Color.YELLOW);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(150, 140, 500, 280, 20, 20);

            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 38));
            String strTituloPausa = "SISTEMA EN PAUSA";
            int anchoTitulo = g2d.getFontMetrics().stringWidth(strTituloPausa);
            g2d.drawString(strTituloPausa, (WIDTH - anchoTitulo) / 2, 215); 
            
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.PLAIN, 15));
            String strSubtituloPausa = "EL FLUJO DEL ENTORNO SE ENCUENTRA CONGELADO";
            int anchoSubtitulo = g2d.getFontMetrics().stringWidth(strSubtituloPausa);
            g2d.drawString(strSubtituloPausa, (WIDTH - anchoSubtitulo) / 2, 265); 

            dibujarBotonSimetrico(g2d, btnResumeRect, "REANUDAR", hoverResume ? new Color(0, 180, 255) : new Color(20, 80, 150));
            dibujarBotonSimetrico(g2d, btnRestartRect, "REINICIAR", hoverRestart ? new Color(220, 140, 0) : new Color(130, 80, 10));
            return; 
        }

        if (gameOver) {
            g2d.setColor(new Color(10, 10, 20, 235));
            g2d.fillRect(0, 0, WIDTH, HEIGHT);

            g2d.setColor(new Color(30, 35, 50));
            g2d.fillRoundRect(150, 100, 500, 400, 20, 20);
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRoundRect(150, 100, 500, 400, 20, 20);

            g2d.setFont(new Font("Segoe UI", Font.BOLD, 38));
            String strTituloGameOver = "PARTIDA FINALIZADA";
            g2d.drawString(strTituloGameOver, (WIDTH - g2d.getFontMetrics().stringWidth(strTituloGameOver)) / 2, 165);

            g2d.setFont(new Font("Consolas", Font.BOLD, 20));
            g2d.setColor(Color.WHITE);
            
            String nombreUsuario = "INVITADO";
            if (jugadorActual != null) {
                try {
                    nombreUsuario = jugadorActual.getUsername().toUpperCase(); 
                } catch (Throwable t1) {
                    try {
                        nombreUsuario = jugadorActual.getUsername().toUpperCase();
                    } catch (Throwable t2) {
                        try {
                            java.lang.reflect.Method m = jugadorActual.getClass().getMethod("getNombre_usuario");
                            nombreUsuario = ((String) m.invoke(jugadorActual)).toUpperCase();
                        } catch (Throwable t3) {
                            nombreUsuario = "JUGADOR 1";
                        }
                    }
                }
            }
            g2d.drawString("USUARIO: " + nombreUsuario, 200, 230);
            
            g2d.setColor(Color.CYAN);
            g2d.drawString("PUNTOS CONSEGUIDOS: " + score + " PTS", 200, 275);
            
            g2d.setColor(Color.GREEN);
            g2d.drawString("MAXIMA RACHA COMBO: " + comboMaximo + " EN CADENA", 200, 320);

            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.drawLine(200, 365, 600, 365);

            dibujarBotonSimetrico(g2d, btnRetryRect, "REINTENTAR", hoverRetry ? new Color(50, 205, 50) : new Color(40, 110, 40));
            dibujarBotonSimetrico(g2d, btnExitRect, "SALIR AL MENÚ", hoverExit ? new Color(220, 50, 50) : new Color(115, 40, 40));
            
            g2d.setColor(new Color(0, 0, 0, 30));
            for (int i = 0; i < HEIGHT; i += 4) g2d.drawLine(0, i, WIDTH, i);
        }
    }

    private void dibujarBotonSimetrico(Graphics2D g2d, Rectangle r, String txt, Color c) {
        g2d.setColor(c); g2d.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12);
        g2d.setColor(Color.WHITE); g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        int tw = g2d.getFontMetrics().stringWidth(txt);
        g2d.drawString(txt, r.x + (r.width - tw) / 2, r.y + 28);
    }

    public void detenerJuego() {
        if (gameTimer != null) gameTimer.stop();
        if (spawnTimer != null) spawnTimer.stop();
        if (timerParpadeo != null) timerParpadeo.stop();
        if (timerViento != null) timerViento.stop(); 
        if (timerMagnetico != null) timerMagnetico.stop();
        detenerSonidosEnCurso();
    }
}