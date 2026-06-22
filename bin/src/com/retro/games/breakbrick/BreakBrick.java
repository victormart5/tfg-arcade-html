package com.retro.games.breakbrick;

import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import com.retro.main.model.Usuario;
import com.retro.main.repository.UsuarioRepository;

 
import com.retro.main.MenuPrincipal; 

public class BreakBrick extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private final int WIDTH = 800;
    private final int HEIGHT = 625;
    private final int WALL_THICKNESS = 24;

    private Usuario jugadorActual;
    private UsuarioRepository repo;
    private MenuPrincipal ventanaMenu; 

    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false; 
    private boolean victory = false; 
    
    private boolean soundMuted = false;

    private int faseActual = 1;
    private int timerTransicionFase = 0; 
    private boolean bossFinalActivado = false;
    private double bossX = 320;
    private final double bossY = 50;
    private final int bossWidth = 160;
    private final int bossHeight = 45;
    private final double bossDirX = 5.5; 
    private int bossHits = 30; 
    private final int bossMaxHits = 30;
    private int timerAtaqueBoss = 0;
    private int timerEscudoBoss = 0;
    private boolean modoAlertaBoss = false;
    private int timerAlertaBoss = 0;
    
    private boolean threshold75 = false;
    private boolean threshold50 = false;
    private boolean threshold25 = false;
    private boolean threshold5 = false;
    private boolean bossEnraged = false;
    
    private int timerAtaqueLaser = 300; 
    private boolean laserAvisando = false;
    private int timerAvisoLaser = 0;
    private double laserCaidaY = 0;
    private int laserActualX = 0;
    private boolean laserCayendo = false;
    
    
    private boolean nieblaActiva = false;
    private int timerNiebla = 0;          
    private int timerAvisoNiebla = 0;     
    
    private int score = 0;
    private int lives = 3;
    private Timer gameTimer;
    private Random random = new Random();

    private int maxRecordGlobalCache = 0;

    private JButton btnPausaFlotante;
    private JButton btnReanudar;
    private JButton btnReiniciar;
    private JButton btnSalir;
    private JButton btnMuteAudio; 

    private Rectangle btnPauseIconRect = new Rectangle(730, 5, 40, 25);
    private Rectangle btnResumeRect = new Rectangle(200, 320, 180, 45);     
    private Rectangle btnRestartRect = new Rectangle(420, 320, 180, 45);    
    private Rectangle btnRetryRect = new Rectangle(200, 420, 180, 45);      
    private Rectangle btnExitRect = new Rectangle(420, 420, 180, 45);       

    private boolean hoverPauseIcon = false;
    private boolean hoverResume = false;
    private boolean hoverRestart = false;
    private boolean hoverRetry = false;
    private boolean hoverExit = false;

    private int comboActual = 0;

    private class FloatingText {
        String texto;
        int x, y, vida;
        Color color;
        FloatingText(String texto, int x, int y, Color color) {
            this.texto = texto; this.x = x; this.y = y; this.color = color;
            this.vida = 35; 
        }
    }
    private ArrayList<FloatingText> floatingTexts = new ArrayList<>();

    private int paddleX = 340;
    private final int paddleY = 540;
    private int currentPaddleWidth = 120;
    private final int paddleHeight = 15;
    private final int paddleSpeed = 12;
    private boolean moveLeft = false, moveRight = false;

    private class Ball {
        double x, y, dx, dy;
        boolean stuck = true; 
        double stuckXOffset = 60.0;

        Ball(double x, double y, double dx, double dy) {
            this.x = x; this.y = y; this.dx = dx; this.dy = dy;
        }

        void acelerarSutil() {
            double factorAceleracion;
            double maxVelocidad;
            
            if (faseActual == 1) {
                factorAceleracion = 1.035; 
                maxVelocidad = 14.5;
            } else if (faseActual == 2) {
                factorAceleracion = 1.055; 
                maxVelocidad = 17.5;
            } else {
                factorAceleracion = 1.020; 
                maxVelocidad = 15.0;
            }
            
            double velocidadActual = Math.sqrt(dx * dx + dy * dy);
            if (velocidadActual < maxVelocidad) {
                dx *= factorAceleracion;
                dy *= factorAceleracion;
            }
        }
    }
    private ArrayList<Ball> balls = new ArrayList<>();
    private final int ballDim = 14;

    private enum BrickType { NORMAL, TNT, MOVING, INVISIBLE, METAL, BOSS, GRAVITY }
    private class Brick {
        int x, y, w, h, hits, maxHits, row;
        BrickType type;
        Color colorBase;
        boolean destroyed = false;
        double angle = 0; 
        int originalX;
        
        boolean isRegen;
        int regenTimer = 0;
        final int MAX_REGEN_TIME = 300; 

        Brick(int x, int y, int w, int h, Color c, int hits, BrickType type, int row, boolean isRegen) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.colorBase = c; this.maxHits = hits; this.hits = hits; this.type = type;
            this.row = row;
            this.originalX = x;
            this.isRegen = isRegen;
        }
    }
    private ArrayList<Brick> brickGrid = new ArrayList<>();

    private class Bomb {
        double x, y, dx, dy;
        boolean reflected = false; 
        
        Bomb(double x, double y, double dx, double dy) { 
            this.x = x; this.y = y; this.dx = dx; this.dy = dy; 
        }
    }
    private ArrayList<Bomb> activeBombs = new ArrayList<>();

    private int visualExplosionX = 0;
    private int visualExplosionY = 0;
    private int visualExplosionTimer = 0;

    private enum PowerType { 
        MULTIBOLA("MULTIBOLA"), 
        PALA_EXTENDIDA("PALA EXTENDIDA"), 
        BOLA_DE_FUEGO("BOLA DE FUEGO"), 
        PEGAJOSA("PEGAJOSA"),
        TRAMPA_INVERSA("CAOS DE CONTROL"),
        TRAMPA_LENTA("MOTOR DAÑADO");

        private final String nombreEs;
        PowerType(String nombreEs) { this.nombreEs = nombreEs; }
        public String getNombreEs() { return nombreEs; }
    }

    private class PowerUp {
        double x, y;
        PowerType type;
        PowerType fakeIdentity; 
        double seed; 

        PowerUp(double x, double y, PowerType t) {
            this.x = x; this.y = y; this.type = t;
            this.seed = random.nextDouble() * 100;
            
            if (t == PowerType.TRAMPA_INVERSA || t == PowerType.TRAMPA_LENTA) {
                
                this.fakeIdentity = PowerType.values()[random.nextInt(4)];
            } else {
                this.fakeIdentity = t;
            }
        }
    }
    private ArrayList<PowerUp> activeDrops = new ArrayList<>();
    private PowerType currentSkill = null;
    private int skillTimer = 0;
    
    
    private PowerType activeDebuff = null;
    private int debuffTimer = 0;

    
    private class Particle {
        double x, y, dx, dy;
        int life;
        Color color;
        Particle(double x, double y, Color c) {
            this.x = x; this.y = y; this.color = c;
            this.dx = (random.nextDouble() - 0.5) * 7;
            this.dy = -2.0 - random.nextDouble() * 4.0;
            this.life = 25 + random.nextInt(20);
        }
    }
    private ArrayList<Particle> particles = new ArrayList<>();
    
    private Image imgVida;
    private Image imgBolaFuego;
    private Image imgPalaExtendida;
    private Image imgPegajosa;
    private Image imgMultiBolas;
    private Image imgBomba; 
    private Image imgBombaExplotando;

    private Clip clipChoque; 
    private Clip clipRomper;
    private Clip clipExplosion;
    private Clip clipVictoria; 
    private Clip clipDerrota;

    public BreakBrick(Usuario jugador, UsuarioRepository repo) {
        this.jugadorActual = jugador;
        this.repo = repo;
        this.setLayout(null); 
        this.setFocusable(true);
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        this.setBackground(new Color(10, 10, 15));

        if (repo != null) {
            try {
                java.util.List<Usuario> todosLosOperadores = repo.findAll();
                for (Usuario u : todosLosOperadores) {
                    if (u != null && u.getPuntos_break() > maxRecordGlobalCache) {
                        maxRecordGlobalCache = u.getPuntos_break();
                    }
                }
            } catch (Exception ex) {
                System.err.println("Error procesando record global en persistencia");
            }
        }

        btnPausaFlotante = new JButton("||");
        btnPausaFlotante.setBounds(WIDTH - WALL_THICKNESS - 45, 5, 40, 25);
        btnPausaFlotante.setFont(new Font("Arial", Font.BOLD, 10));
        btnPausaFlotante.setBackground(new Color(25, 30, 45));
        btnPausaFlotante.setForeground(Color.CYAN);
        btnPausaFlotante.setBorder(BorderFactory.createLineBorder(Color.CYAN, 1));
        btnPausaFlotante.setFocusable(false); 
        btnPausaFlotante.setVisible(false);  
        btnPausaFlotante.addActionListener(e -> alternarPausa());
        this.add(btnPausaFlotante);

        btnMuteAudio = new JButton("SONIDO: ON");
        btnMuteAudio.setBounds(WIDTH - 150, HEIGHT - 38, 110, 25);
        btnMuteAudio.setFont(new Font("Consolas", Font.BOLD, 12));
        btnMuteAudio.setBackground(new Color(20, 25, 40));
        btnMuteAudio.setForeground(Color.GREEN);
        btnMuteAudio.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
        btnMuteAudio.setFocusable(false);
        btnMuteAudio.setVisible(false); 
        btnMuteAudio.addActionListener(e -> {
            soundMuted = !soundMuted;
            if (soundMuted) {
                btnMuteAudio.setText("SONIDO: OFF");
                btnMuteAudio.setForeground(Color.RED);
                btnMuteAudio.setBorder(BorderFactory.createLineBorder(Color.RED, 1));
            } else {
                btnMuteAudio.setText("SONIDO: ON");
                btnMuteAudio.setForeground(Color.GREEN);
                btnMuteAudio.setBorder(BorderFactory.createLineBorder(Color.GREEN, 1));
            }
        });
        this.add(btnMuteAudio);

        btnReanudar = new JButton("REANUDAR PARTIDA");
        btnReiniciar = new JButton("REINICIAR PARTIDA");
        btnSalir = new JButton("VOLVER AL MENÚ");

        configurarBotonMenu(btnReanudar, 280, 230);
        configurarBotonMenu(btnReiniciar, 280, 285);
        configurarBotonMenu(btnSalir, 280, 340);

        btnReanudar.addActionListener(e -> alternarPausa());
        btnReiniciar.addActionListener(e -> { alternarPausa(); iniciarJuego(); });
        
        btnSalir.addActionListener(e -> {
            detenerJuego();
            Window topFrame = SwingUtilities.getWindowAncestor(BreakBrick.this);
            if (topFrame != null) {
                topFrame.dispose(); 
            }
        }); 

        try {
            imgVida = new ImageIcon("res/corazonVida.png").getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
            imgBolaFuego = new ImageIcon("res/poderFuego.png").getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
            imgPalaExtendida = new ImageIcon("res/poderAumentoTamañoTabla.png").getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
            imgPegajosa = new ImageIcon("res/poderSlime.png").getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
            imgMultiBolas = new ImageIcon("res/poderMultiplicadorDeBolas.png").getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH);
            imgBomba = new ImageIcon("res/Bomba.png").getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
            imgBombaExplotando = new ImageIcon("res/BombaExplotando.png").getImage().getScaledInstance(45, 45, Image.SCALE_SMOOTH);
        } catch (Exception e) {}

        try {
            File fChoque = new File("res/choque.wav"); 
            if(fChoque.exists()) { clipChoque = AudioSystem.getClip(); clipChoque.open(AudioSystem.getAudioInputStream(fChoque)); }
            File fRomper = new File("res/romperLadrillo.wav");
            if(fRomper.exists()) { clipRomper = AudioSystem.getClip(); clipRomper.open(AudioSystem.getAudioInputStream(fRomper)); }
            File fExplosion = new File("res/explosion.wav");
            if(fExplosion.exists()) { clipExplosion = AudioSystem.getClip(); clipExplosion.open(AudioSystem.getAudioInputStream(fExplosion)); }
            File fVictoria = new File("res/victoria.wav"); 
            if(fVictoria.exists()) { clipVictoria = AudioSystem.getClip(); clipVictoria.open(AudioSystem.getAudioInputStream(fVictoria)); }
            File fDerrota = new File("res/derrota.wav"); 
            if(fDerrota.exists()) { clipDerrota = AudioSystem.getClip(); clipDerrota.open(AudioSystem.getAudioInputStream(fDerrota)); }
        } catch (Exception e) {}

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                
                if (jugadorActual != null && "admin".equalsIgnoreCase(jugadorActual.getUsername()) && running && !gameOver && !victory && !paused) {
                    if (key == KeyEvent.VK_1) {
                        faseActual = 1; timerTransicionFase = 0; bossFinalActivado = false; modoAlertaBoss = false;
                        generarNivel(); 
                        return;
                    }
                    if (key == KeyEvent.VK_2) {
                        faseActual = 2; timerTransicionFase = 0; bossFinalActivado = false; modoAlertaBoss = false;
                        generarNivel(); 
                        return;
                    }
                    if (key == KeyEvent.VK_3) {
                        faseActual = 3; timerTransicionFase = 0; bossFinalActivado = false; 
                        modoAlertaBoss = true; timerAlertaBoss = 120;
                        brickGrid.clear(); activeDrops.clear(); activeBombs.clear(); balls.clear(); 
                        resetThresholds();
                        return;
                    }
                }

                if (key == KeyEvent.VK_P && running && !gameOver && !victory && timerTransicionFase <= 0 && !modoAlertaBoss) {
                    alternarPausa();
                    return;
                }

                if (paused || timerTransicionFase > 0 || modoAlertaBoss) return; 

                if (!running && !gameOver && !victory) { iniciarJuego(); return; }
                if (gameOver || victory) { return; } 
                
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) moveLeft = true;
                if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) moveRight = true;
                
                if (key == KeyEvent.VK_SPACE) {
                    for (Ball b : balls) {
                        if (b.stuck) {
                            b.stuck = false;
                            b.dx = (random.nextDouble() - 0.5) * 3; 
                            b.dy = (faseActual >= 2) ? -7.2 : -5.5; 
                        }
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) moveLeft = false;
                if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) moveRight = false;
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point puntoClic = e.getPoint();
                if (running && !gameOver && !paused && !victory && timerTransicionFase <= 0 && !modoAlertaBoss && btnPauseIconRect.contains(puntoClic)) { alternarPausa(); return; }
                if (paused && !gameOver) {
                    if (btnResumeRect.contains(puntoClic)) alternarPausa();
                    if (btnRestartRect.contains(puntoClic)) iniciarJuego();
                    return;
                }
                if (gameOver || victory) {
                    if (btnRetryRect.contains(puntoClic)) iniciarJuego();
                    if (btnExitRect.contains(puntoClic)) {
                        detenerJuego();
                        Window topFrame = SwingUtilities.getWindowAncestor(BreakBrick.this);
                        if (topFrame != null) topFrame.dispose(); 
                    }
                }
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point puntoRaton = e.getPoint();
                if (running && !gameOver && !paused && !victory && timerTransicionFase <= 0) hoverPauseIcon = btnPauseIconRect.contains(puntoRaton);
                if (paused && !gameOver) { hoverResume = btnResumeRect.contains(puntoRaton); hoverRestart = btnRestartRect.contains(puntoRaton); }
                if (gameOver || victory) { hoverRetry = btnRetryRect.contains(puntoRaton); hoverExit = btnExitRect.contains(puntoRaton); }
                repaint();
            }
        });
        
        generarNivel();
    }

    private void configurarBotonMenu(JButton btn, int x, int y) {
        btn.setBounds(x, y, 240, 40);
        btn.setFont(new Font("Consolas", Font.BOLD, 14));
        btn.setBackground(new Color(20, 20, 35));
        btn.setForeground(Color.WHITE);
        btn.setBorder(BorderFactory.createLineBorder(Color.CYAN, 2));
        btn.setFocusable(false);
        btn.setVisible(false);
        this.add(btn);
    }

    private void alternarPausa() {
        if (!running || gameOver || victory || timerTransicionFase > 0 || modoAlertaBoss) return;
        paused = !paused;
        
        if (paused) {
            gameTimer.stop();
            btnReanudar.setVisible(true);
            btnReiniciar.setVisible(true);
            btnSalir.setVisible(true);
            btnMuteAudio.setVisible(false);
        } else {
            btnReanudar.setVisible(false);
            btnReiniciar.setVisible(false);
            btnSalir.setVisible(false);
            btnMuteAudio.setVisible(true);
            gameTimer.start();
        }
        repaint();
    }

    private void resetThresholds() {
        threshold75 = false;
        threshold50 = false;
        threshold25 = false;
        threshold5 = false;
        bossEnraged = false;
        timerAtaqueLaser = 300;
        laserAvisando = false;
        laserCayendo = false;
        activeDebuff = null;
        debuffTimer = 0;
    }

    private void generarNivel() {
        brickGrid.clear();
        balls.clear();
        activeBombs.clear();
        balls.add(new Ball((WIDTH / 2.0) - 7.0, paddleY - ballDim, 0.0, 0.0)); 
        paddleX = 340; 
        bossFinalActivado = false;
        modoAlertaBoss = false;
        resetThresholds();
        
        Color[] colors = {
            new Color(255, 45, 85),   
            new Color(255, 149, 0),  
            new Color(255, 214, 10),  
            new Color(52, 199, 89),   
            new Color(0, 199, 190)    
        };
        
        int usableWidth = WIDTH - (WALL_THICKNESS * 2);
        int bw = usableWidth / 10;
        int bh = 24;

        for (int r = 0; r < 6; r++) { 
            for (int c = 0; c < 10; c++) {
                int xCoord = WALL_THICKNESS + (c * bw);
                int yCoord = 35 + r * bh;

                BrickType type = BrickType.NORMAL;
                boolean isRegen = false;
                int rand = random.nextInt(100);
                
                int hits = (faseActual >= 2) ? (random.nextInt(3) + 1) : ((r <= 1) ? 3 : (r <= 3) ? 2 : 1);
                Color colorAleatorio = (faseActual >= 2) ? colors[random.nextInt(colors.length)] : colors[Math.min(r, colors.length - 1)];

                if (faseActual == 2) {
                    if (r == 2 && (c == 2 || c == 7)) { 
                        type = BrickType.GRAVITY; 
                        hits = 1;
                    } else if (rand < 25 && hits == 3) { 
                        isRegen = true; 
                    } else if (rand < 35) { type = BrickType.TNT; }
                    else if (rand < 50) { type = BrickType.MOVING; }
                    else if (rand < 60) { type = BrickType.INVISIBLE; }
                } else {
                    if (rand < 10) type = BrickType.TNT;
                    else if (rand < 20) type = BrickType.MOVING;
                    else if (rand < 28) type = BrickType.INVISIBLE;
                }

                brickGrid.add(new Brick(xCoord, yCoord, bw, bh, colorAleatorio, hits, type, r, isRegen));
            }
        }
    }

    private void iniciarJuego() {
        running = true; gameOver = false; paused = false; victory = false; 
        faseActual = 1; timerTransicionFase = 0; score = 0; lives = 3;
        nieblaActiva = false; timerNiebla = 0; timerAvisoNiebla = 0; 
        currentSkill = null; skillTimer = 0; comboActual = 0;
        currentPaddleWidth = 120;
        bossFinalActivado = false;
        modoAlertaBoss = false;
        visualExplosionTimer = 0;
        floatingTexts.clear();
        activeDrops.clear();
        particles.clear();
        activeBombs.clear();
        resetThresholds();
        
        btnReanudar.setVisible(false);
        btnReiniciar.setVisible(false);
        btnSalir.setVisible(false);
        btnPausaFlotante.setVisible(true); 
        btnMuteAudio.setVisible(true); 

        generarNivel();
        if (gameTimer != null) gameTimer.stop();
        gameTimer = new Timer(16, this);
        gameTimer.start();
    }

    private void invocarEsbirros() {
        floatingTexts.add(new FloatingText("¡ESBIRROS INVOCADOS!", (int)bossX, (int)bossY + 60, Color.RED));
        int bw = (WIDTH - WALL_THICKNESS * 2) / 10;
        for(int c = 0; c < 10; c++) {
            if (c == 4 || c == 5) continue; 
            BrickType t = (c % 2 == 0) ? BrickType.METAL : BrickType.MOVING;
            brickGrid.add(new Brick(WALL_THICKNESS + c * bw, (int)bossY + 120, bw, 24, Color.GRAY, 2, t, 0, false));
        }
    }

    private void aplicarDanoBoss(int damage) {
        bossHits -= damage;
        if (bossHits <= 0) {
            bossHits = 0;
            ejecutarVictoria();
            return;
        }

        if (bossHits <= 22 && !threshold75) {
            threshold75 = true;
            lanzarDefensaYSuministros();
        }
        if (bossHits <= 15 && !threshold50) {
            threshold50 = true;
            bossEnraged = true; 
            floatingTexts.add(new FloatingText("¡SISTEMA NÉMESIS ENFURECIDO!", (int)bossX - 20, (int)bossY - 10, Color.RED));
            lanzarDefensaYSuministros();
        }
        if (bossHits <= 7 && !threshold25) {
            threshold25 = true;
            invocarEsbirros(); 
            lanzarDefensaYSuministros();
        }
        if (bossHits <= 2 && !threshold5) {
            threshold5 = true;
            lanzarDefensaYSuministros();
        }
    }

    private void lanzarDefensaYSuministros() {
        if (!nieblaActiva) {
            timerAvisoNiebla = 90;
            floatingTexts.add(new FloatingText("¡DEFENSA TÁCTICA: NIEBLA!", (WIDTH / 2) - 120, HEIGHT - 80, Color.ORANGE));
        }
        int cantidad = 2 + random.nextInt(2);
        for(int i = 0; i < cantidad; i++) {
            double rx = bossX + random.nextInt(bossWidth);
            activeDrops.add(new PowerUp(rx, bossY + bossHeight, PowerType.values()[random.nextInt(4)]));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running || gameOver || paused || victory) return;

        if (modoAlertaBoss) {
            timerAlertaBoss--;
            if (timerAlertaBoss <= 0) {
                modoAlertaBoss = false;
                bossFinalActivado = true;
                bossX = 320;
                bossHits = bossMaxHits;
                timerAtaqueBoss = 60;
                balls.clear();
                balls.add(new Ball(paddleX + 60.0, paddleY - ballDim, 0.0, 0.0));
                balls.get(0).stuck = true;
            }
            repaint();
            return;
        }

        if (timerTransicionFase > 0) {
            timerTransicionFase--;
            if (timerTransicionFase == 0) {
                faseActual++;
                generarNivel(); 
            }
            repaint();
            return;
        }

        if (faseActual >= 2) {
            if (!bossFinalActivado && !nieblaActiva && timerAvisoNiebla <= 0 && random.nextInt(1800) == 0) {
                timerAvisoNiebla = 90;
                floatingTexts.add(new FloatingText("¡ANOMALÍA CLIMÁTICA DETECTADA!", WIDTH / 2 - 120, HEIGHT - 80, Color.ORANGE));
            }
            if (timerAvisoNiebla > 0) {
                timerAvisoNiebla--;
                if (timerAvisoNiebla == 0) { nieblaActiva = true; timerNiebla = 420; }
            }
            if (nieblaActiva) {
                timerNiebla--;
                if (timerNiebla <= 0) nieblaActiva = false;
            }
        }

        if (skillTimer > 0) {
            skillTimer--;
            if (skillTimer == 0) { currentSkill = null; currentPaddleWidth = 120; }
        }

        if (debuffTimer > 0) {
            debuffTimer--;
            if (debuffTimer <= 0) { activeDebuff = null; }
        }

        if (visualExplosionTimer > 0) visualExplosionTimer--;

        boolean dirLeft = moveLeft;
        boolean dirRight = moveRight;
        
        if (activeDebuff == PowerType.TRAMPA_INVERSA) {
            dirLeft = moveRight;
            dirRight = moveLeft;
        }
        
        int efectPaddleSpeed = (activeDebuff == PowerType.TRAMPA_LENTA) ? paddleSpeed / 3 : paddleSpeed;

        if (dirLeft) paddleX = Math.max(WALL_THICKNESS, paddleX - efectPaddleSpeed);
        if (dirRight) paddleX = Math.min(WIDTH - WALL_THICKNESS - currentPaddleWidth, paddleX + efectPaddleSpeed);

        if (bossFinalActivado) {
            if (!balls.isEmpty()) {
                Ball objetivo = balls.get(0);
                double centroBoss = bossX + (bossWidth / 2.0);
                double velocidadBossActual = bossEnraged ? bossDirX * 1.6 : bossDirX; 
                
                if (objetivo.dy < 0 && objetivo.y < bossY + bossHeight + 250) {
                    if (objetivo.x + (ballDim / 2.0) < centroBoss) bossX += velocidadBossActual; 
                    else bossX -= velocidadBossActual; 
                }
            }
            
            if (bossX < WALL_THICKNESS) bossX = WALL_THICKNESS; 
            if (bossX > WIDTH - WALL_THICKNESS - bossWidth) bossX = WIDTH - WALL_THICKNESS - bossWidth; 

            timerAtaqueBoss--;
            if (timerAtaqueBoss <= 0) {
                double bOrigX = bossX + (bossWidth / 2.0) - 11;
                double bOrigY = bossY + bossHeight;
                double destinoTargetX = paddleX + (currentPaddleWidth / 2.0);
                double deltaX = destinoTargetX - bOrigX;
                double deltaY = paddleY - bOrigY;
                double distanciaPala = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
                
                double bombaVelY = bossEnraged ? 7.5 : 5.0; 
                double bombaVelX = (deltaX / distanciaPala) * bombaVelY; 
                
                activeBombs.add(new Bomb(bOrigX, bOrigY, bombaVelX, bombaVelY));
                
                if (bossEnraged) timerAtaqueBoss = 35 + random.nextInt(30); 
                else timerAtaqueBoss = 70 + random.nextInt(50); 
            }

            if (!laserAvisando && !laserCayendo) {
                timerAtaqueLaser--;
                if (timerAtaqueLaser <= 0) {
                    laserAvisando = true;
                    timerAvisoLaser = 60; 
                    laserActualX = paddleX + (currentPaddleWidth / 2) - 15;
                }
            } else if (laserAvisando) {
                timerAvisoLaser--;
                if (timerAvisoLaser <= 0) {
                    laserAvisando = false;
                    laserCayendo = true;
                    laserCaidaY = bossY + bossHeight;
                    ejecutarClipPrecalculado(clipExplosion); 
                }
            } else if (laserCayendo) {
                laserCaidaY += 28.0; 
                int beamStart = (int)bossY + bossHeight;
                int beamHeightLaser = (int)laserCaidaY - beamStart;
                
                Rectangle rectLaser = new Rectangle(laserActualX - 5, beamStart, 40, beamHeightLaser);
                Rectangle rectPala = new Rectangle(paddleX, paddleY, currentPaddleWidth, paddleHeight);
                
                if (rectLaser.intersects(rectPala)) {
                    laserCayendo = false;
                    timerAtaqueLaser = 300 + random.nextInt(200);
                    
                    visualExplosionX = laserActualX;
                    visualExplosionY = paddleY - 20;
                    visualExplosionTimer = 20; 

                    lives--;
                    comboActual = 0;
                    ejecutarClipPrecalculado(clipExplosion);
                    floatingTexts.add(new FloatingText("¡IMPACTO LÁSER!", paddleX, paddleY - 20, Color.RED));
                    
                    if (lives <= 0) {
                        finalizarJuego();
                        return;
                    } else {
                        balls.clear();
                        balls.add(new Ball(paddleX + 60.0, paddleY - ballDim, 0.0, 0.0));
                        balls.get(0).stuck = true;
                    }
                } else if (laserCaidaY > HEIGHT) {
                    laserCayendo = false;
                    timerAtaqueLaser = 300 + random.nextInt(200);
                }
            }

            if (timerEscudoBoss > 0) timerEscudoBoss--;
            if (timerEscudoBoss <= 0 && !balls.isEmpty()) {
                Ball bola = balls.get(0);
                if (bola.dy < 0 && bola.y < bossY + bossHeight + 100 && bola.x > bossX - 20 && bola.x < bossX + bossWidth + 20) {
                    int bw = (WIDTH - WALL_THICKNESS * 2) / 10;
                    BrickType tEscudo = BrickType.values()[random.nextInt(4)]; 
                    brickGrid.add(new Brick((int)bossX + bossWidth/2 - bw/2, (int)bossY + bossHeight + 30, bw, 24, Color.CYAN, 1, tEscudo, 0, false));
                    timerEscudoBoss = 180; 
                    floatingTexts.add(new FloatingText("¡ESCUDO INVOCADO!", (int)bossX + 20, (int)bossY + bossHeight + 10, Color.CYAN));
                }
            }
        }

        for (Brick b : brickGrid) {
            if (!b.destroyed) {
                if (b.type == BrickType.MOVING) {
                    b.angle += 0.05;
                    b.x = b.originalX + (int)(Math.sin(b.angle) * 15);
                }
                
                if (b.isRegen && b.hits > 0 && b.hits < b.maxHits) {
                    b.regenTimer--;
                    if (b.regenTimer <= 0) {
                        b.hits = b.maxHits; 
                        floatingTexts.add(new FloatingText("REGEN", b.x, b.y, Color.GREEN));
                    }
                }
            }
        }

        for (int i = floatingTexts.size() - 1; i >= 0; i--) {
            FloatingText ft = floatingTexts.get(i);
            ft.y -= 1;
            ft.vida--;
            if (ft.vida <= 0) floatingTexts.remove(i);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.dy += 0.2; 
            p.x += p.dx; 
            p.y += p.dy; 
            p.life--;
            if (p.life <= 0) particles.remove(i);
        }

        Iterator<Bomb> bombIter = activeBombs.iterator();
        while (bombIter.hasNext()) {
            Bomb b = bombIter.next(); 
            b.x += b.dx; 
            b.y += b.dy; 
            
            Rectangle rectBomba = new Rectangle((int)b.x, (int)b.y, 22, 22);
            Rectangle rectPala = new Rectangle(paddleX, paddleY, currentPaddleWidth, paddleHeight);
            
            if (!b.reflected && rectBomba.intersects(rectPala)) {
                if (currentSkill == PowerType.BOLA_DE_FUEGO || currentSkill == PowerType.PEGAJOSA) {
                    b.reflected = true;
                    b.dy = -9.0; 
                    b.dx = ((b.x + 11) - (paddleX + currentPaddleWidth / 2.0)) / 5.0; 
                    floatingTexts.add(new FloatingText("¡BLOQUEO PARRY!", (int)b.x - 10, (int)b.y - 10, Color.CYAN));
                    ejecutarClipPrecalculado(clipChoque);
                } else {
                    visualExplosionX = (int)b.x - 11;
                    visualExplosionY = (int)b.y - 11;
                    visualExplosionTimer = 20; 

                    bombIter.remove();
                    lives--;
                    comboActual = 0;
                    ejecutarClipPrecalculado(clipExplosion);
                    
                    if (lives <= 0) {
                        finalizarJuego();
                        return;
                    } else {
                        balls.clear();
                        balls.add(new Ball(paddleX + 60.0, paddleY - ballDim, 0.0, 0.0));
                        balls.get(0).stuck = true;
                    }
                }
            } 
            else if (b.reflected && bossFinalActivado && rectBomba.intersects(new Rectangle((int)bossX, (int)bossY, bossWidth, bossHeight))) {
                bombIter.remove();
                score += 500;
                floatingTexts.add(new FloatingText("¡DAÑO DEVUELTO!", (int)b.x - 20, (int)b.y, Color.MAGENTA));
                crearExplosion((int)b.x, (int)b.y, Color.RED);
                ejecutarClipPrecalculado(clipExplosion);
                aplicarDanoBoss(5); 
                
            } else if (b.y > HEIGHT || b.x < 0 || b.x > WIDTH || b.y < 0) {
                bombIter.remove();
            }
        }

        Iterator<PowerUp> pIter = activeDrops.iterator();
        while (pIter.hasNext()) {
            PowerUp p = pIter.next();
            p.y += 3;
            
            if (p.type == PowerType.TRAMPA_INVERSA || p.type == PowerType.TRAMPA_LENTA) {
                p.y += 0.5; 
                p.x += Math.sin((p.y + p.seed) / 15.0) * 3.0; 
                
                if (p.x < WALL_THICKNESS) p.x = WALL_THICKNESS;
                if (p.x > WIDTH - WALL_THICKNESS - 25) p.x = WIDTH - WALL_THICKNESS - 25;
            }

            if (new Rectangle((int)p.x, (int)p.y, 25, 25).intersects(new Rectangle(paddleX, paddleY, currentPaddleWidth, paddleHeight))) {
                aplicarPowerUp(p.type);
                pIter.remove();
            } else if (p.y > HEIGHT) pIter.remove();
        }

        Iterator<Ball> bIter = balls.iterator();
        while (bIter.hasNext()) {
            Ball b = bIter.next();
            if (b.stuck) {
                b.x = paddleX + b.stuckXOffset;
                b.y = paddleY - ballDim;
            } else {
                b.x += b.dx; b.y += b.dy;
                
                if (faseActual == 2) {
                    for (Brick br : brickGrid) {
                        if (br.type == BrickType.GRAVITY && !br.destroyed) {
                            double cx = br.x + (br.w / 2.0);
                            double cy = br.y + (br.h / 2.0);
                            double bcx = b.x + (ballDim / 2.0);
                            double bcy = b.y + (ballDim / 2.0);
                            double dist = Math.hypot(cx - bcx, cy - bcy);
                            
                            if (dist < 180 && dist > 15) {
                                double fuerzaAtraccion = 0.6 / (dist / 40.0); 
                                fuerzaAtraccion = Math.min(fuerzaAtraccion, 1.5);
                                b.dx += ((cx - bcx) / dist) * fuerzaAtraccion;
                                b.dy += ((cy - bcy) / dist) * fuerzaAtraccion;
                            }
                        }
                    }
                }
                
                if (b.x <= WALL_THICKNESS) {
                    b.x = WALL_THICKNESS; b.dx *= -1; b.acelerarSutil();
                    ejecutarClipPrecalculado(clipChoque);
                }
                if (b.x >= WIDTH - WALL_THICKNESS - ballDim) {
                    b.x = WIDTH - WALL_THICKNESS - ballDim; b.dx *= -1; b.acelerarSutil();
                    ejecutarClipPrecalculado(clipChoque);
                }
                if (b.y <= WALL_THICKNESS) {
                    b.y = WALL_THICKNESS; b.dy *= -1; b.acelerarSutil();
                    ejecutarClipPrecalculado(clipChoque);
                }

                if (new Rectangle((int)b.x, (int)b.y, ballDim, ballDim).intersects(new Rectangle(paddleX, paddleY, currentPaddleWidth, paddleHeight))) {
                    comboActual = 0; 
                    ejecutarClipPrecalculado(clipChoque);
                    
                    if (currentSkill == PowerType.PEGAJOSA) {
                        b.stuck = true;
                        b.stuckXOffset = b.x - paddleX;
                    } else {
                        b.dy = -Math.abs(b.dy);
                        b.dx = ((b.x + ballDim/2) - (paddleX + currentPaddleWidth/2)) / 10;
                        b.acelerarSutil();
                    }
                }

                if (bossFinalActivado) {
                    Rectangle rectBola = new Rectangle((int)b.x, (int)b.y, ballDim, ballDim);
                    Rectangle rectBoss = new Rectangle((int)bossX, (int)bossY, bossWidth, bossHeight);
                    Rectangle rectCore = new Rectangle((int)bossX + (bossWidth / 2) - 15, (int)bossY, 30, bossHeight);

                    if (rectBola.intersects(rectBoss)) {
                        if (rectBola.intersects(rectCore)) {
                            score += 1000;
                            comboActual++;
                            floatingTexts.add(new FloatingText("¡GOLPE CRÍTICO!", (int)b.x - 20, (int)b.y - 10, Color.YELLOW));
                            aplicarDanoBoss(4); 
                        } else {
                            score += 250;
                            comboActual++;
                            aplicarDanoBoss(1); 
                        }
                        
                        ejecutarClipPrecalculado(clipChoque);
                        if (currentSkill != PowerType.BOLA_DE_FUEGO) b.dy = Math.abs(b.dy); 
                        continue; 
                    }
                }

                for (Brick br : brickGrid) {
                    if (!br.destroyed && new Rectangle((int)b.x, (int)b.y, ballDim, ballDim).intersects(new Rectangle(br.x, br.y, br.w, br.h))) {
                        romperLadrillo(br);
                        if (currentSkill != PowerType.BOLA_DE_FUEGO || br.type == BrickType.METAL || br.type == BrickType.GRAVITY || br.type == BrickType.BOSS) {
                            int bolaCentroX = (int)b.x + (ballDim / 2);
                            if (bolaCentroX < br.x || bolaCentroX > br.x + br.w) {
                                b.dx *= -1;
                            } else {
                                b.dy *= -1;
                            }
                            b.acelerarSutil();
                        }
                        break;
                    }
                }
                
                if (b.y > HEIGHT) bIter.remove();
            }
        }

        if (balls.isEmpty()) {
            lives--;
            comboActual = 0;
            if (lives <= 0) finalizarJuego();
            else balls.add(new Ball(paddleX + 60.0, paddleY - ballDim, 0.0, 0.0));
        }

        boolean quedanBloquesDestructibles = false;
        for (Brick br : brickGrid) {
            if (!br.destroyed && br.type != BrickType.METAL && br.type != BrickType.GRAVITY) {
                quedanBloquesDestructibles = true;
                break;
            }
        }
        
        if (!quedanBloquesDestructibles && !bossFinalActivado && !modoAlertaBoss) {
            if (faseActual == 1) {
                timerTransicionFase = 90; 
                activeDrops.clear();
                balls.clear();
                balls.add(new Ball(WIDTH / 2.0, paddleY - ballDim, 0.0, 0.0));
            } else if (faseActual == 2) {
                modoAlertaBoss = true;
                timerAlertaBoss = 120;
                activeDrops.clear();
            } else {
                ejecutarVictoria();
            }
        }

        repaint();
    }

    private void romperLadrillo(Brick b) {
        if (b.type == BrickType.METAL || b.type == BrickType.GRAVITY) {
            ejecutarClipPrecalculado(clipChoque);
            return;
        }

        b.hits--;
        b.regenTimer = b.MAX_REGEN_TIME; 
        comboActual++; 

        int puntosBase = 50;
        int puntosDestruccion = 100;
        
        if (comboActual >= 10) {
            puntosBase *= 3;
            puntosDestruccion *= 3;
            floatingTexts.add(new FloatingText("¡COMBO MAX x3!", b.x, b.y - 10, new Color(255, 215, 0)));
        } else if (comboActual >= 5) {
            puntosBase *= 2;
            puntosDestruccion *= 2;
            floatingTexts.add(new FloatingText("¡RACHA x2!", b.x, b.y - 10, Color.ORANGE));
        }

        score += puntosBase;
        crearExplosion(b.x + b.w/2, b.y + b.h/2, b.colorBase);

        if (b.hits <= 0) {
            b.destroyed = true;
            score += puntosDestruccion;
            ejecutarClipPrecalculado(clipRomper); 
            if (b.type == BrickType.TNT) explotarTNT(b);
            
            if (random.nextInt(100) < 35) { 
                PowerType dropType;
                if (faseActual == 2 && random.nextInt(100) < 45) {
                    dropType = random.nextBoolean() ? PowerType.TRAMPA_INVERSA : PowerType.TRAMPA_LENTA;
                } else {
                    dropType = PowerType.values()[random.nextInt(4)];
                }
                activeDrops.add(new PowerUp(b.x, b.y, dropType));
            }
        }
    }

    private void explotarTNT(Brick center) {
        ejecutarClipPrecalculado(clipExplosion);
        for (Brick b : brickGrid) {
            if (!b.destroyed && b.type != BrickType.METAL && b.type != BrickType.GRAVITY && b.type != BrickType.BOSS && Math.abs(b.x - center.x) <= center.w + 5 && Math.abs(b.y - center.y) <= center.h + 5) {
                b.destroyed = true;
                score += 100; 
                crearExplosion(b.x + b.w/2, b.y + b.h/2, b.colorBase);
            }
        }
    }

    private void ejecutarClipPrecalculado(Clip clip) {
        if (clip == null || soundMuted) return; 
        new Thread(() -> {
            if (clip.isRunning()) { clip.stop(); }
            clip.setFramePosition(0); 
            clip.start();
        }).start();
    }

    private void crearExplosion(int x, int y, Color c) {
        for (int i = 0; i < 12; i++) particles.add(new Particle(x, y, c));
    }

    private void aplicarPowerUp(PowerType t) {
        if (t == PowerType.TRAMPA_INVERSA || t == PowerType.TRAMPA_LENTA) {
            activeDebuff = t;
            debuffTimer = 240; 
            ejecutarClipPrecalculado(clipExplosion);
            floatingTexts.add(new FloatingText("¡SISTEMA CORROMPIDO!", paddleX - 20, paddleY - 20, Color.RED));
        } else {
            currentSkill = t;
            skillTimer = 600; 
            switch(t) {
                case MULTIBOLA:
                    if (balls.size() > 0 && balls.size() < 5) {
                        Ball base = balls.get(0);
                        balls.add(new Ball(base.x, base.y, Math.abs(base.dx), -Math.abs(base.dy)));
                        balls.add(new Ball(base.x, base.y, -Math.abs(base.dx), -Math.abs(base.dy)));
                    }
                    break;
                case PALA_EXTENDIDA: currentPaddleWidth = 200; break;
                default: break;
            }
        }
    }

    private void finalizarJuego() {
        gameOver = true; 
        running = false; 
        btnPausaFlotante.setVisible(false); 
        btnMuteAudio.setVisible(false); 
        gameTimer.stop();
        
        if (clipChoque != null && clipChoque.isRunning()) clipChoque.stop();
        if (clipExplosion != null && clipExplosion.isRunning()) clipExplosion.stop();
        ejecutarClipPrecalculado(clipDerrota); 
        
        if (jugadorActual != null && score > jugadorActual.getPuntos_break()) {
            jugadorActual.setPuntos_break(score); 
            new Thread(() -> {
                try { repo.save(jugadorActual); } catch(Exception e) { System.err.println(e.getMessage()); }
            }).start();
        }
        repaint();
    }

    private void ejecutarVictoria() {
        victory = true; running = false; btnPausaFlotante.setVisible(false); btnMuteAudio.setVisible(false); gameTimer.stop();
        ejecutarClipPrecalculado(clipVictoria); 
        if (jugadorActual != null && score > jugadorActual.getPuntos_break()) {
            jugadorActual.setPuntos_break(score); 
            new Thread(() -> {
                try { repo.save(jugadorActual); } catch(Exception e) { System.err.println(e.getMessage()); }
            }).start();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (!running && !gameOver && !victory && timerTransicionFase <= 0 && !modoAlertaBoss) {
            g2d.setColor(new Color(25, 25, 40, 50));
            for (int i = 0; i < WIDTH; i += 40) g2d.drawLine(i, 0, i, HEIGHT);
            for (int j = 0; j < HEIGHT; j += 40) g2d.drawLine(0, j, WIDTH, j);

            g2d.setColor(Color.CYAN);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 36));
            g2d.drawString("BREAKBRICK ARCADE", 215, 75);
            
            if (score > maxRecordGlobalCache) maxRecordGlobalCache = score;
            
            g2d.setColor(Color.YELLOW);
            g2d.setFont(new Font("Consolas", Font.BOLD, 18));
            String strHighScore = "RÉCORD GLOBAL DEL SISTEMA: " + maxRecordGlobalCache + " PTS";
            g2d.drawString(strHighScore, (WIDTH - g2d.getFontMetrics().stringWidth(strHighScore)) / 2, 115);

            int startX = 65; int startY = 160; int paddingY = 42;

            g2d.setColor(new Color(0, 255, 190));
            g2d.setFont(new Font("Consolas", Font.BOLD, 15));
            g2d.drawString("MANUAL DE MODIFICADORES TÁCTICOS:", startX, startY - 15);

            if (imgMultiBolas != null) g2d.drawImage(imgMultiBolas, startX, startY, null);
            else { g2d.setColor(Color.CYAN); g2d.fillOval(startX, startY, 25, 25); }
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2d.drawString("MULTIBOLA (M): Clona instantáneamente las esferas en juego para triplicar el daño.", startX + 38, startY + 17);

            startY += paddingY;
            if (imgPalaExtendida != null) g2d.drawImage(imgPalaExtendida, startX, startY, null);
            else { g2d.setColor(Color.PINK); g2d.fillOval(startX, startY, 25, 25); }
            g2d.setColor(Color.WHITE);
            g2d.drawString("PALA EXTENDIDA (E): Incrementa el ancho de la base de rebote para asegurar el control.", startX + 38, startY + 17);

            startY += paddingY;
            if (imgBolaFuego != null) g2d.drawImage(imgBolaFuego, startX, startY, null);
            else { g2d.setColor(Color.ORANGE); g2d.fillOval(startX, startY, 25, 25); }
            g2d.setColor(Color.WHITE);
            g2d.drawString("BOLA DE FUEGO (F): Perfora bloques destructibles sin rebotar, arrasando filas y blindaje.", startX + 38, startY + 17);

            startY += paddingY;
            if (imgPegajosa != null) g2d.drawImage(imgPegajosa, startX, startY, null);
            else { g2d.setColor(Color.GREEN); g2d.fillOval(startX, startY, 25, 25); }
            g2d.setColor(Color.WHITE);
            g2d.drawString("PALA PEGAJOSA (P): Atrapa la bola al impactar, permitiendo relanzarla con [ESPACIO].", startX + 38, startY + 17);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 14));
            g2d.drawString("CONTROLES: MOVER CON FLECHAS [<-] / [->] O [A] / [D]", startX, startY + 65);
            g2d.drawString("DISPARAR / VOLVER A LANZAR BOLA: [BARRA ESPACIADORA]", startX, startY + 90);
            
            g2d.setColor(new Color(0, 255, 190));
            String strStart = "--- PULSA CUALQUIER TECLA PARA EMPEZAR PARTIDA ---";
            g2d.drawString(strStart, (WIDTH - g2d.getFontMetrics().stringWidth(strStart)) / 2, HEIGHT - 100);
            return;
        }

        
        g2d.setPaint(new GradientPaint(0, 0, new Color(20, 30, 48), WIDTH, 0, new Color(10, 15, 24)));
        g2d.fillRect(0, 0, WALL_THICKNESS, HEIGHT - 50);
        g2d.fillRect(WIDTH - WALL_THICKNESS, 0, WALL_THICKNESS, HEIGHT - 50);
        g2d.fillRect(0, 0, WIDTH, WALL_THICKNESS);

        g2d.setColor(Color.CYAN);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawLine(WALL_THICKNESS, WALL_THICKNESS, WALL_THICKNESS, HEIGHT - 50);
        g2d.drawLine(WIDTH - WALL_THICKNESS, WALL_THICKNESS, WIDTH - WALL_THICKNESS, HEIGHT - 50);
        g2d.drawLine(WALL_THICKNESS, WALL_THICKNESS, WIDTH - WALL_THICKNESS, WALL_THICKNESS);

       
        if (bossFinalActivado) {
            if (laserAvisando) {
                boolean parpadeo = (timerAvisoLaser % 10 < 5);
                g2d.setColor(new Color(255, 0, 0, parpadeo ? 100 : 30));
                g2d.fillRect(laserActualX - 5, (int)bossY + bossHeight, 40, HEIGHT);
                
                g2d.setColor(parpadeo ? Color.RED : new Color(150, 0, 0));
                Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(new BasicStroke(parpadeo ? 3f : 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[]{10.0f, 10.0f}, 0.0f));
                g2d.drawLine(laserActualX + 15, (int)bossY + bossHeight, laserActualX + 15, HEIGHT);
                g2d.setStroke(oldStroke);
            }
            if (laserCayendo) {
                int beamStart = (int)bossY + bossHeight;
                int beamHeight = (int)laserCaidaY - beamStart;
                
                if (beamHeight > 0) {
                    g2d.setColor(new Color(255, 0, 0, 200));
                    g2d.fillRect(laserActualX - 5, beamStart, 40, beamHeight);
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(laserActualX + 5, beamStart, 20, beamHeight);
                    g2d.setColor(new Color(255, 255, 0, 200));
                    g2d.fillRect(laserActualX + 10, beamStart, 10, beamHeight);
                }
            }
        }

       
        for (Brick b : brickGrid) {
            if (!b.destroyed) {
                if (b.type == BrickType.INVISIBLE) {
                    boolean mostrar = (System.currentTimeMillis() % 2000 < 200);
                    for (Ball ball : balls) if (Math.abs(ball.x - b.x) < 60 && Math.abs(ball.y - b.y) < 60) mostrar = true;
                    if (!mostrar) continue;
                }
                
                if (b.type == BrickType.GRAVITY) {
                    g2d.setColor(new Color(15, 0, 25));
                    g2d.fillRect(b.x, b.y, b.w, b.h);
                    
                    int pulse = (int)(Math.sin(System.currentTimeMillis() / 200.0) * 5);
                    g2d.setColor(new Color(128, 0, 128, 150));
                    g2d.drawOval(b.x + (b.w/2) - 10 + pulse, b.y + (b.h/2) - 10 + pulse, 20 - pulse*2, 20 - pulse*2);
                    g2d.setColor(Color.MAGENTA);
                    g2d.fillOval(b.x + (b.w/2) - 3, b.y + (b.h/2) - 3, 6, 6);
                }
                else if (b.type == BrickType.METAL) {
                    g2d.setPaint(new GradientPaint(b.x, b.y, new Color(140, 145, 155), b.x, b.y + b.h, new Color(75, 80, 85)));
                    g2d.fillRect(b.x, b.y, b.w, b.h);
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.drawRect(b.x + 1, b.y + 1, b.w - 2, b.h - 2);
                    g2d.fillRect(b.x + 4, b.y + 4, 3, 3);
                    g2d.fillRect(b.x + b.w - 7, b.y + 4, 3, 3);
                } 
                else if (b.type == BrickType.TNT) {
                    int tx = b.x; int ty = b.y; int tw = b.w + 1; int th = b.h;
                    GradientPaint tntBody = new GradientPaint(tx, ty, new Color(240, 25, 25), tx, ty + th, new Color(130, 5, 5));
                    g2d.setPaint(tntBody);
                    g2d.fillRect(tx, ty, tw, th);

                    g2d.setColor(new Color(25, 25, 25));
                    g2d.fillRect(tx, ty + 2, tw, 3);          
                    g2d.fillRect(tx, ty + th - 5, tw, 3);     

                    g2d.setColor(new Color(15, 15, 15));
                    g2d.fillRect(tx + 8, ty + 5, tw - 16, th - 10);
                    g2d.setColor(new Color(220, 160, 10)); 
                    g2d.drawRect(tx + 8, ty + 5, tw - 16, th - 10);

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Impact", Font.PLAIN, 13));
                    g2d.drawString("TNT", tx + (tw / 2) - 10, ty + 17);
                } 
                else {
                    Color colorActual = b.colorBase;
                    if (b.hits == 2) colorActual = b.colorBase.darker();
                    if (b.hits == 1) colorActual = b.colorBase.darker().darker();

                    g2d.setColor(new Color(colorActual.getRed(), colorActual.getGreen(), colorActual.getBlue(), 45));
                    g2d.fillRoundRect(b.x - 3, b.y - 3, b.w + 6, b.h + 6, 6, 6);

                    GradientPaint glassEffect = new GradientPaint(b.x, b.y, colorActual.brighter(), b.x, b.y + b.h, colorActual.darker());
                    g2d.setPaint(glassEffect);
                    g2d.fillRoundRect(b.x, b.y, b.w + 1, b.h, 4, 4);

                    g2d.setColor(new Color(255, 255, 255, 120));
                    g2d.fillRect(b.x + 2, b.y + 2, b.w - 3, 3);

                    g2d.setColor(colorActual.brighter());
                    g2d.setStroke(new BasicStroke(1.5f));
                    g2d.drawRoundRect(b.x, b.y, b.w + 1, b.h, 4, 4);

                    if (b.isRegen) {
                        g2d.setColor(new Color(50, 255, 50, 150));
                        g2d.fillOval(b.x + b.w - 12, b.y + 4, 8, 8); 
                        
                        if (b.hits < b.maxHits) {
                            int barW = (int)((b.w - 4) * (b.regenTimer / (double)b.MAX_REGEN_TIME));
                            g2d.setColor(Color.GREEN);
                            g2d.fillRect(b.x + 2, b.y + b.h - 4, barW, 2);
                        }
                    }

                    if (b.maxHits > 1) {
                        g2d.setColor(new Color(255, 255, 255, 180)); 
                        if (b.hits == 2) {
                            g2d.setStroke(new BasicStroke(1.0f));
                            g2d.drawLine(b.x + 8, b.y + 4, b.x + 20, b.y + 14);
                            g2d.drawLine(b.x + 20, b.y + 14, b.x + 15, b.y + b.h - 4);
                        } else if (b.hits == 1) {
                            g2d.setStroke(new BasicStroke(1.5f));
                            g2d.drawLine(b.x + 8, b.y + 4, b.x + 22, b.y + 12);
                            g2d.drawLine(b.x + 22, b.y + 12, b.x + 14, b.y + b.h - 4);
                            g2d.drawLine(b.x + 22, b.y + 12, b.x + b.w - 10, b.y + 6);
                            g2d.drawLine(b.x + 16, b.y + 17, b.x + 5, b.y + b.h - 8);
                        }
                    }
                }
            }
        }

       
        if (bossFinalActivado) {
            Color cCuerpo1 = bossEnraged ? new Color(220, 20, 20) : new Color(110, 10, 10);
            Color cCuerpo2 = bossEnraged ? new Color(150, 0, 0)   : new Color(30, 0, 5);
            
            g2d.setPaint(new GradientPaint((int)bossX, (int)bossY, cCuerpo1, (int)bossX + bossWidth, (int)bossY + bossHeight, cCuerpo2));
            g2d.fillRoundRect((int)bossX, (int)bossY, bossWidth, bossHeight, 12, 12);
            g2d.setColor(Color.RED); g2d.setStroke(new BasicStroke(3f)); g2d.drawRoundRect((int)bossX, (int)bossY, bossWidth, bossHeight, 12, 12);
            
            
            int coreX = (int)bossX + bossWidth / 2 - 15;
            g2d.setColor(new Color(50, 255, 50, 180)); 
            g2d.fillRoundRect(coreX, (int)bossY + 5, 30, bossHeight - 10, 8, 8);
            g2d.setColor(Color.WHITE); g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(coreX, (int)bossY + 5, 30, bossHeight - 10, 8, 8);
            
            g2d.setColor(Color.DARK_GRAY); g2d.fillRect((int)bossX + 15, (int)bossY + 10, bossWidth - 30, 6);
            g2d.setColor(Color.GREEN); int anchoSalud = (int)((bossWidth - 30) * (bossHits / (double)bossMaxHits)); g2d.fillRect((int)bossX + 15, (int)bossY + 10, anchoSalud, 6);
            
            g2d.setColor(Color.WHITE); g2d.setFont(new Font("Impact", Font.PLAIN, 14));
            g2d.drawString("JEFE FINAL", (int)bossX + (bossWidth / 2) - 58, (int)bossY + 34);
        }

        
        for (Bomb b : activeBombs) {
            if (b.reflected) {
                g2d.setColor(Color.GREEN); g2d.fillOval((int)b.x, (int)b.y, 16, 16);
                g2d.setColor(Color.CYAN); g2d.drawOval((int)b.x, (int)b.y, 16, 16);
            } else if (imgBomba != null) {
                g2d.drawImage(imgBomba, (int)b.x, (int)b.y, null);
            } else {
                g2d.setColor(Color.RED); g2d.fillOval((int)b.x, (int)b.y, 16, 16);
            }
        }

        if (visualExplosionTimer > 0 && imgBombaExplotando != null) {
            g2d.drawImage(imgBombaExplotando, visualExplosionX, visualExplosionY, null);
        }

        
        Color paddleColor1 = Color.CYAN;
        Color paddleColor2 = new Color(5, 40, 95);
        if (activeDebuff != null) {
            paddleColor1 = Color.RED;
            paddleColor2 = Color.BLACK;
        }
        g2d.setPaint(new GradientPaint(paddleX, paddleY, paddleColor1, paddleX, paddleY + paddleHeight, paddleColor2));
        g2d.fillRoundRect(paddleX, paddleY, currentPaddleWidth, paddleHeight, 8, 8);
        g2d.setColor(Color.WHITE); g2d.drawRoundRect(paddleX, paddleY, currentPaddleWidth, paddleHeight, 8, 8);
        
        g2d.setColor(currentSkill == PowerType.BOLA_DE_FUEGO ? Color.ORANGE : Color.WHITE);
        for (Ball b : balls) g2d.fillOval((int)b.x, (int)b.y, ballDim, ballDim);

       
        if (nieblaActiva && running && !gameOver && !victory && timerTransicionFase <= 0) {
            int fx = WIDTH / 2, fy = HEIGHT / 2; if (!balls.isEmpty()) { fx = (int)balls.get(0).x + 7; fy = (int)balls.get(0).y + 7; }
            RadialGradientPaint niebla = new RadialGradientPaint(fx, fy, 165f, new float[]{0.0f, 0.4f, 1.0f}, new Color[]{new Color(0,0,0,0), new Color(0,0,0,140), new Color(5,5,10,255)});
            g2d.setPaint(niebla); g2d.fillRect(WALL_THICKNESS, WALL_THICKNESS, WIDTH - WALL_THICKNESS * 2, HEIGHT - WALL_THICKNESS - 50);
        }

        if (activeDebuff != null) {
            g2d.setFont(new Font("Impact", Font.ITALIC, 32)); 
            g2d.setColor(new Color(255, 0, 0, (debuffTimer % 20 < 10) ? 180 : 50));
            String danger = "¡PELIGRO: " + activeDebuff.getNombreEs() + "!";
            g2d.drawString(danger, (WIDTH - g2d.getFontMetrics().stringWidth(danger)) / 2, HEIGHT / 2);
        }

        if (modoAlertaBoss && (timerAlertaBoss % 20 < 10)) {
            g2d.setColor(new Color(20, 10, 10, 130)); 
            g2d.fillRect(0, 0, WIDTH, HEIGHT);
            g2d.setFont(new Font("Impact", Font.BOLD, 46)); g2d.setColor(Color.RED);
            String txtAlerta = "¡ALERTA: JEFE FINAL DETECTADO!";
            g2d.drawString(txtAlerta, (WIDTH - g2d.getFontMetrics().stringWidth(txtAlerta)) / 2, HEIGHT / 2);
        }

        for (PowerUp p : activeDrops) {
            boolean isTrap = (p.type == PowerType.TRAMPA_INVERSA || p.type == PowerType.TRAMPA_LENTA);
            boolean glitching = isTrap && (System.currentTimeMillis() % 1000 < 150);

            if (glitching) {
                g2d.setColor(Color.RED);
                g2d.fillRect((int)p.x, (int)p.y, 25, 25);
                g2d.setColor(Color.YELLOW);
                g2d.drawRect((int)p.x, (int)p.y, 25, 25);
                g2d.setFont(new Font("Consolas", Font.BOLD, 22));
                g2d.drawString("!", (int)p.x + 6, (int)p.y + 20);
            } else {
                PowerType drawType = isTrap ? p.fakeIdentity : p.type;
                if (drawType == PowerType.BOLA_DE_FUEGO && imgBolaFuego != null) g2d.drawImage(imgBolaFuego, (int) p.x, (int) p.y, null);
                else if (drawType == PowerType.PALA_EXTENDIDA && imgPalaExtendida != null) g2d.drawImage(imgPalaExtendida, (int) p.x, (int) p.y, null);
                else if (drawType == PowerType.PEGAJOSA && imgPegajosa != null) g2d.drawImage(imgPegajosa, (int) p.x, (int) p.y, null);
                else if (drawType == PowerType.MULTIBOLA && imgMultiBolas != null) g2d.drawImage(imgMultiBolas, (int) p.x, (int) p.y, null);
            }
        }
        
        for (Particle p : particles) { g2d.setColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(), Math.min(255, p.life * 10))); g2d.fillRect((int)p.x, (int)p.y, 4, 4); }
        g2d.setFont(new Font("Consolas", Font.BOLD, 15)); for (FloatingText ft : floatingTexts) { g2d.setColor(ft.color); g2d.drawString(ft.texto, ft.x, ft.y); }

        g2d.setColor(new Color(16, 18, 30)); g2d.fillRect(0, HEIGHT - 50, WIDTH, 50); g2d.setColor(Color.CYAN); g2d.drawRect(0, HEIGHT - 50, WIDTH - 1, 49);
        
        g2d.setFont(new Font("Consolas", Font.BOLD, 16)); g2d.setColor(comboActual >= 5 ? Color.ORANGE : Color.WHITE); 
        g2d.drawString("SCORE:" + score, 10, HEIGHT - 18);
        
        if (comboActual > 1) { 
            g2d.setFont(new Font("Consolas", Font.BOLD, 13)); 
            g2d.drawString("COMBO:x" + comboActual, 100, HEIGHT - 18); 
        }
        
        for (int i = 0; i < lives; i++) {
            if (imgVida != null) g2d.drawImage(imgVida, 160 + (i * 20), HEIGHT - 34, 18, 18, null);
        }

        g2d.setColor(Color.WHITE); 
        g2d.setFont(new Font("Consolas", Font.BOLD, 13)); 
        g2d.drawString("FASE:" + (bossFinalActivado ? "BOSS" : faseActual), 230, HEIGHT - 18);
        
        if (nieblaActiva) { 
            g2d.setColor(Color.ORANGE); 
            g2d.drawString(String.format("NIEBLA:%.1fs", timerNiebla / 60.0), 305, HEIGHT - 18); 
        } else if (currentSkill != null) { 
            g2d.setColor(Color.YELLOW); 
            g2d.drawString("[" + currentSkill.getNombreEs() + "]", 305, HEIGHT - 18); 
        } else if (activeDebuff != null) {
            g2d.setColor(Color.RED); 
            g2d.drawString("[" + activeDebuff.getNombreEs() + "]", 305, HEIGHT - 18);
        }

        if (jugadorActual != null && "admin".equalsIgnoreCase(jugadorActual.getUsername())) {
            g2d.setColor(new Color(255, 100, 255)); 
            g2d.setFont(new Font("Consolas", Font.BOLD, 12));
            g2d.drawString("PASAR FASE: 1-3", 500, HEIGHT - 18);
        }

        if (!gameOver && !paused && !victory && timerTransicionFase <= 0 && !modoAlertaBoss) {
            g2d.setColor(hoverPauseIcon ? new Color(80, 80, 110, 200) : new Color(40, 45, 60, 150)); g2d.fillRoundRect(btnPauseIconRect.x, btnPauseIconRect.y, btnPauseIconRect.width, btnPauseIconRect.height, 8, 8);
            g2d.setColor(Color.CYAN); g2d.setStroke(new BasicStroke(2)); g2d.drawRoundRect(btnPauseIconRect.x, btnPauseIconRect.y, btnPauseIconRect.width, btnPauseIconRect.height, 8, 8);
            g2d.setColor(Color.WHITE); g2d.fillRect(743, 9, 4, 16); g2d.fillRect(753, 9, 4, 16);
        }

        if (paused && !gameOver) { g2d.setColor(new Color(10, 10, 20, 180)); g2d.fillRect(0, 0, WIDTH, HEIGHT); g2d.setColor(Color.CYAN); g2d.setFont(new Font("Segoe UI", Font.BOLD, 36)); g2d.drawString("JUEGO EN PAUSA", 260, 180); }
        if (timerTransicionFase > 0) { g2d.setColor(new Color(10, 10, 15, 150)); g2d.fillRect(0, 0, WIDTH, HEIGHT); g2d.setFont(new Font("Impact", Font.ITALIC, 48)); g2d.setColor(Color.ORANGE); String txtSiguiente = "PREPÁRATE: SIGUIENTE FASE"; g2d.drawString(txtSiguiente, (WIDTH - g2d.getFontMetrics().stringWidth(txtSiguiente)) / 2, HEIGHT / 2 - 10); }

        if (gameOver) {
            g2d.setColor(new Color(10, 10, 20, 235)); g2d.fillRect(0, 0, WIDTH, HEIGHT); g2d.setColor(new Color(30, 35, 50)); g2d.fillRoundRect(150, 100, 500, 400, 20, 20); g2d.setColor(Color.RED); g2d.setStroke(new BasicStroke(3)); g2d.drawRoundRect(150, 100, 500, 400, 20, 20);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 38)); String strTituloGameOver = "PARTIDA FINALIZADA"; g2d.drawString(strTituloGameOver, (WIDTH - g2d.getFontMetrics().stringWidth(strTituloGameOver)) / 2, 165);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20)); g2d.setColor(Color.WHITE); String nombreUsuario = (jugadorActual != null) ? jugadorActual.getUsername().toUpperCase() : "INVITADO"; g2d.drawString("USUARIO: " + nombreUsuario, 200, 230);
            g2d.setColor(Color.CYAN); g2d.drawString("PUNTOS CONSEGUIDOS: " + score + " PTS", 200, 285); g2d.setColor(new Color(255, 255, 255, 40)); g2d.drawLine(200, 360, 600, 360);
            dibujarBotonSimetrico(g2d, btnRetryRect, "REINTENTAR", hoverRetry ? new Color(50, 205, 50) : new Color(40, 110, 40)); dibujarBotonSimetrico(g2d, btnExitRect, "SALIR AL MENÚ", hoverExit ? new Color(220, 50, 50) : new Color(115, 40, 40));
        }

        if (victory) {
            g2d.setColor(new Color(10, 10, 20, 235)); g2d.fillRect(0, 0, WIDTH, HEIGHT); g2d.setColor(new Color(25, 35, 30)); g2d.fillRoundRect(150, 100, 500, 400, 20, 20); g2d.setColor(Color.GREEN); g2d.setStroke(new BasicStroke(3)); g2d.drawRoundRect(150, 100, 500, 400, 20, 20);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 38)); String strTituloVictory = "¡VICTORIA TOTAL!"; g2d.drawString(strTituloVictory, (WIDTH - g2d.getFontMetrics().stringWidth(strTituloVictory)) / 2, 165);
            g2d.setFont(new Font("Consolas", Font.BOLD, 20)); g2d.setColor(Color.WHITE); String nombreUsuario = (jugadorActual != null) ? jugadorActual.getUsername().toUpperCase() : "GANADOR RETRO"; g2d.drawString("JUGADOR: " + nombreUsuario, 200, 230);
            g2d.setColor(Color.CYAN); g2d.drawString("PUNTOS TOTALES: " + score + " PTS", 200, 285); g2d.setColor(new Color(255, 255, 255, 40)); g2d.drawLine(200, 360, 600, 360);
            dibujarBotonSimetrico(g2d, btnRetryRect, "NUEVA PARTIDA", hoverRetry ? new Color(50, 205, 50) : new Color(40, 110, 40)); dibujarBotonSimetrico(g2d, btnExitRect, "SALIR AL MENÚ", hoverExit ? new Color(220, 50, 50) : new Color(115, 40, 40));
        }
    }

    private void dibujarBotonSimetrico(Graphics2D g2d, Rectangle r, String txt, Color c) {
        g2d.setColor(c); g2d.fillRoundRect(r.x, r.y, r.width, r.height, 12, 12); g2d.setColor(Color.WHITE); g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        g2d.drawString(txt, r.x + (r.width - g2d.getFontMetrics().stringWidth(txt)) / 2, r.y + 28);
    }

    public void detenerJuego() {
        if (gameTimer != null) gameTimer.stop(); 
        running = false;
    }
}