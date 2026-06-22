package com.retro.main;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.File;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.retro.main.repository.UsuarioRepository;
import com.retro.main.repository.ReclamacionRepository;
import com.retro.main.repository.LogRegistroRepository;
import com.retro.main.model.Usuario;
import com.retro.main.model.Reclamacion;
import com.retro.main.model.LogRegistro;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

import com.retro.games.snake.SnakeGame;
import com.retro.games.pong.PongGame;
import com.retro.games.puzzle2048.Game2048;
import com.retro.games.tetris.TetrisGame;


@SpringBootApplication
@ComponentScan(basePackages = "com.retro") 
@EntityScan("com.retro.main.model")
@EnableJpaRepositories("com.retro.main.repository")
@Component
public class MenuPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    
    private JLabel lblReloj;
    private JPanel panelFondo;
    private JPanel panelIconos; 
    
    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private ReclamacionRepository reclamacionRepo;
    
    @Autowired
    private LogRegistroRepository logRepo;
    
    private Usuario usuarioSesion;
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private final String MENSAJE_REQUISITOS_PASS = "ERROR: Mínimo 6 caracteres, una mayúscula, una minúscula y un número.";

    private Color colorFondo1 = new Color(7, 7, 12);
    private Color colorFondo2 = new Color(18, 22, 32);
    private float volumenGlobalBGM = 0.8f; 
    private List<String> logsAutenticacionLocal = new ArrayList<>();
    private JPanel panelJuegoActivoRef = null;
    private static final java.util.Map<String, String> registroOperadoresBaneadosConMotivo = new java.util.HashMap<>();
    
    private static final List<String> registroOperadoresBaneados = new ArrayList<>();
    private static final java.util.Map<String, String> reclamacionesPendientes = new java.util.HashMap<>();

    public MenuPrincipal() {
        setTitle("ARCADE OS v4.0 - OFFICIAL RELEASE");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 890); 
        setLocationRelativeTo(null);
        
        panelFondo = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, colorFondo1, 0, getHeight(), colorFondo2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                boolean modoOscuro = (usuarioSesion == null) ? true : usuarioSesion.isDarkMode();
                if (modoOscuro) {
                    g2d.setColor(new Color(255, 255, 255, 2));
                    for (int y = 0; y < getHeight(); y += 4) {
                        g2d.drawLine(0, y, getWidth(), y);
                    }
                }
            }
        };
        
        panelFondo.setLayout(new BoxLayout(panelFondo, BoxLayout.Y_AXIS));
        panelFondo.setBorder(new EmptyBorder(12, 25, 12, 25));

        construirBarraEstadoSuperior();
        construirAccesoControlPerfiles();
        construirBloqueTitularYControles();
        construirPanelCreditosYApagado();
        
        add(panelFondo);
    }
    
    @PostConstruct
    public void inicializarLogsBD() {
        if (logRepo != null) {
            List<LogRegistro> historialDB = logRepo.findAll();
            for (LogRegistro l : historialDB) {
                logsAutenticacionLocal.add(l.getContenido());
            }
        }
        registrarLog("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] KERNEL BOOT: Ejecución correcta del sistema.");
    }

    private void registrarLog(String mensaje) {
        logsAutenticacionLocal.add(mensaje);
        if (logRepo != null) {
            logRepo.save(new LogRegistro(mensaje));
        }
    }
    
    private void construirBarraEstadoSuperior() {
        JPanel barraEstado = new JPanel(new BorderLayout());
        barraEstado.setOpaque(false);
        barraEstado.setMaximumSize(new Dimension(600, 25));
        
        JLabel lblStatus = new JLabel(" SYSTEM STATUS: ONLINE");
        lblStatus.setForeground(new Color(120, 125, 135));
        lblStatus.setFont(new Font("Monospaced", Font.BOLD, 11));
        
        lblReloj = new JLabel();
        lblReloj.setForeground(Color.CYAN);
        lblReloj.setFont(new Font("Monospaced", Font.BOLD, 12));
        iniciarReloj();
        
        barraEstado.add(lblStatus, BorderLayout.WEST);
        barraEstado.add(lblReloj, BorderLayout.EAST);
        panelFondo.add(barraEstado);
    }

    private void construirAccesoControlPerfiles() {
        panelIconos = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelIconos.setOpaque(false);
        panelIconos.setMaximumSize(new Dimension(600, 40));
        
        JButton btnInfoTFG = new JButton("INFO");
        btnInfoTFG.setPreferredSize(new Dimension(55, 36));
        btnInfoTFG.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnInfoTFG.setFocusPainted(false);
        btnInfoTFG.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnInfoTFG.setBorder(new LineBorder(new Color(0, 255, 255, 50), 1));
        btnInfoTFG.setOpaque(true);
        btnInfoTFG.setBackground(new Color(25, 25, 33));
        btnInfoTFG.setForeground(Color.CYAN); 
        btnInfoTFG.addActionListener(e -> mostrarDefensaPopupTFG());
        
        panelIconos.add(btnInfoTFG);
        
        JButton btnPerfil = crearBotonCuadrado("res/perfil_icon.png", e -> { 
            if(usuarioSesion != null) mostrarPerfil(); 
            else JOptionPane.showMessageDialog(this, "Acceso denegado: Autentique un operador."); 
        });
        JButton btnAjustes = crearBotonCuadrado("res/ajustes_icon.png", e -> { 
            if(usuarioSesion != null) mostrarAjustes(); 
            else JOptionPane.showMessageDialog(this, "Acceso denegado: Autentique un operador."); 
        });
        
        panelIconos.add(btnPerfil);
        panelIconos.add(btnAjustes);
        panelFondo.add(panelIconos);
        panelFondo.add(Box.createRigidArea(new Dimension(0, 10)));
    }

    private void construirBloqueTitularYControles() {
        JLabel titulo = new JLabel("ARCADE MULTIGAME");
        titulo.setForeground(Color.CYAN);
        titulo.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titulo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        panelFondo.add(titulo);
        
        panelFondo.add(Box.createRigidArea(new Dimension(0, 20)));

        panelFondo.add(crearBotonPro("NUEVO JUGADOR / SESIÓN", null, e -> mostrarAutenticacion()));
        panelFondo.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel panelMosaico = new JPanel();
        panelMosaico.setOpaque(false);
        panelMosaico.setLayout(new GridLayout(3, 2, 12, 12)); 
        panelMosaico.setMaximumSize(new Dimension(420, 332)); 
        panelMosaico.setPreferredSize(new Dimension(420, 332));

        panelMosaico.add(crearBotonMatrizCuadrada("SNAKE ARCADE", "res/snake_icon.png", e -> {
            if (usuarioSesion == null) JOptionPane.showMessageDialog(this, "Operación desestimada: Requiere registro activo.");
            else lanzarJuego(new SnakeGame(usuarioSesion, usuarioRepo), "Snake Arcade");
        }));
        
        panelMosaico.add(crearBotonMatrizCuadrada("PONG RETRO", "res/pong_icon.png", e -> lanzarJuego(new PongGame(), "Pong Retro")));
        
        panelMosaico.add(crearBotonMatrizCuadrada("2048 PUZZLE", "res/2048_icon.png", e -> {
            if (usuarioSesion == null) JOptionPane.showMessageDialog(this, "Operación desestimada: Requiere registro activo.");
            else lanzarJuego(new Game2048(usuarioSesion, usuarioRepo), "2048 Puzzle");
        }));
        
        panelMosaico.add(crearBotonMatrizCuadrada("TETRIS CLASSIC", "res/tetris_icon.png", e -> {
            if (usuarioSesion == null) JOptionPane.showMessageDialog(this, "Operación desestimada: Requiere registro activo.");
            else lanzarJuego(new TetrisGame(usuarioSesion, usuarioRepo), "Tetris Classic");
        }));

        panelMosaico.add(crearBotonMatrizCuadrada("CATCH OR DROP", "res/fotoCatch.png", e -> {
            if (usuarioSesion == null) {
                JOptionPane.showMessageDialog(this, "Operación desestimada: Requiere registro activo.");
            } else {
                lanzarJuego(new com.retro.games.catchgame.CatchGame(usuarioSesion, usuarioRepo), "Catch or Drop");
            }
        }));

        panelMosaico.add(crearBotonMatrizCuadrada("BREAKBRICK", "res/imagenBreakBrick.png", e -> {
            if (usuarioSesion == null) {
                JOptionPane.showMessageDialog(this, "Operación desestimada: Requiere registro activo.");
            } else {
                lanzarJuego(new com.retro.games.breakbrick.BreakBrick(usuarioSesion, usuarioRepo), "BreakBrick");
            }
        }));
        
        panelFondo.add(panelMosaico);
        panelFondo.add(Box.createRigidArea(new Dimension(0, 10)));

        panelFondo.add(crearBotonPro("VER RANKING DE MÉRITOS", "res/trofeo_neon.png", e -> mostrarRanking()));
        panelFondo.add(Box.createRigidArea(new Dimension(0, 20)));
        panelFondo.add(Box.createRigidArea(new Dimension(0, 20)));
    }

    private void construirPanelCreditosYApagado() {
        JPanel panelInfo = new JPanel(new GridLayout(0, 1));
        panelInfo.setOpaque(false);
        panelInfo.setBorder(new LineBorder(new Color(0, 255, 255, 40), 1));
        panelInfo.setMaximumSize(new Dimension(420, 80));

        String infoTexto = "<html><center><font color='cyan'><b>SISTEMA ARCADE v4.0</b></font><br>"
                + "<font color='#9499a6'>PROYECTO TFG - PERSISTENCIA DE DATOS RELACIONAL<br>"
                + "OPERADORES: JORGE & VÍCTOR | ESTADO: COMPILACIÓN VERIFICADA</font></center></html>";

        JLabel lblInfo = new JLabel(infoTexto);
        lblInfo.setHorizontalAlignment(SwingConstants.CENTER);
        panelInfo.add(lblInfo);
        panelInfo.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        panelFondo.add(panelInfo);

        panelFondo.add(Box.createRigidArea(new Dimension(0, 15)));

        JButton btnSalirApp = crearBotonPro("SALIR DE LA APP", null, e -> System.exit(0));
        btnSalirApp.setBackground(new Color(45, 12, 12));
        btnSalirApp.setForeground(new Color(255, 70, 70));
        btnSalirApp.setBorder(new LineBorder(new Color(255, 0, 0, 80), 1));
        panelFondo.add(btnSalirApp);
    }

    private void mostrarDefensaPopupTFG() {
        JDialog d = new JDialog(this, "DOCUMENTACIÓN OFICIAL DEL PROYECTO TFG", true);
        d.setSize(480, 740);
        d.setLocationRelativeTo(this);
        
        JPanel pPanelNegro = new JPanel();
        pPanelNegro.setBackground(new Color(10, 10, 15));
        pPanelNegro.setLayout(new BorderLayout());
        pPanelNegro.setBorder(new EmptyBorder(15, 15, 15, 15));
        d.setContentPane(pPanelNegro);
        d.getRootPane().setBorder(new LineBorder(Color.CYAN, 2));

        JPanel pScrollContenido = new JPanel();
        pScrollContenido.setOpaque(false);
        pScrollContenido.setLayout(new BoxLayout(pScrollContenido, BoxLayout.Y_AXIS));

        JPanel pAutores = new JPanel(new GridLayout(0, 1, 6, 6));
        pAutores.setOpaque(false);
        pAutores.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN, 1), " AUTORÍA Y TITULACIÓN ", 0, 0, new Font("Monospaced", Font.BOLD, 12), Color.CYAN));
        
        JLabel lA1 = new JLabel("  AUTORES: Jorge & Víctor"); lA1.setForeground(Color.WHITE); lA1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel lA2 = new JLabel("  PROMOCIÓN: Desarrollo de Aplicaciones Multiplataforma (DAM)"); lA2.setForeground(new Color(200, 220, 255)); lA2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JLabel lA3 = new JLabel("  PROYECTO: Trabajo de Fin de Grado (TFG) - Arcade OS"); lA3.setForeground(Color.CYAN); lA3.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        
        pAutores.add(lA1); pAutores.add(lA2); pAutores.add(lA3);
        pScrollContenido.add(pAutores);
        pScrollContenido.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel pTecnologias = new JPanel();
        pTecnologias.setOpaque(false);
        pTecnologias.setLayout(new BoxLayout(pTecnologias, BoxLayout.Y_AXIS));
        pTecnologias.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN, 1), " ARQUITECTURA DEL SISTEMA ", 0, 0, new Font("Monospaced", Font.BOLD, 11), Color.CYAN));
        
        String txtTechData = "• Entorno de Ejecución: Java Virtual Machine (JVM v17+)\n" +
                             "• Framework Núcleo: Spring Boot Context Engine & IoC Core\n" +
                             "• Capa de Persistencia: Spring Data JPA + ORM Hibernate Engine\n" +
                             "• Motor de Renderizado Físico: Java Component Graphics 2D Canvas & Swing\n" +
                             "• Seguridad de Acceso: BCrypt Cryptographic Salted Hashing Encryption";
                             
        JTextArea areaTech = new JTextArea(txtTechData);
        areaTech.setOpaque(false); areaTech.setEditable(false);
        areaTech.setForeground(Color.WHITE); areaTech.setFont(new Font("Consolas", Font.PLAIN, 11));
        areaTech.setLineWrap(true); areaTech.setWrapStyleWord(true);
        areaTech.setBorder(new EmptyBorder(5, 10, 5, 5));
        pTecnologias.add(areaTech);
        
        pScrollContenido.add(pTecnologias);
        pScrollContenido.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel pFAQ = new JPanel();
        pFAQ.setLayout(new BoxLayout(pFAQ, BoxLayout.Y_AXIS));
        pFAQ.setOpaque(false);
        pFAQ.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.CYAN, 1), " CUESTIONARIO DE DEFENSA ", 0, 0, new Font("Monospaced", Font.BOLD, 11), Color.CYAN));

        String[][] bancoFAQ = {
            {"¿Cómo se integra Spring Boot con Swing?", 
             "Se utiliza SpringApplicationBuilder configurando el entorno headless en false. Esto permite levantar el contenedor de inversión de control (IoC) inyectando los repositorios JPA directamente sobre los JFrames y JPanels."},
            {"¿Por qué se ha implementado cifrado BCrypt?", 
             "Para cumplir con el estándar OWASP de seguridad. BCrypt aplica un algoritmo de hashing asimétrico con salt aleatorio que impide ataques de diccionario o descifrado inverso en caso de brechas en la base de datos."},
            {"¿Cómo funciona el refresco de los juegos?", 
             "Cada juego encapsula un javax.swing.Timer sincronizado a 16 milisegundos de tasa de refresco, lo que fuerza un ciclo constante de actualización de coordenadas lógicas y llamadas síncronas al método paintComponent logrando 60 FPS estables."},
            {"¿Cómo se gestiona la fuga de memoria (Memory Leaks) en Swing?", 
             "Al cerrar una ventana secundaria se llama a dispose() y se detienen explícitamente todos los hilos y subprocesos activos de los Javax.swing.Timer. Esto corta las referencias fuertes y permite que el Garbage Collector limpie los búferes de imagen acumulados de la RAM."},
            {"¿Qué patrón arquitectónico sigue la persistencia de datos?", 
             "Sigue el patrón de Repositorio sobre una arquitectura en capas. Spring Data JPA desacopla la capa de negocio de la consulta SQL directa abstrayendo las transacciones CRUD mediante proxies dinámicos generados en tiempo de ejecución."},
            {"¿Cómo maneja la aplicación la concurrencia en los hilos de audio?", 
             "Los archivos de efectos de sonido (.wav) se cargan de forma asíncrona bifurcando la ejecución en nuevos hilos independientes (Thread) cada vez que se produce un impacto. Esto evita congelar el hilo principal de eventos de la interfaz (Event Dispatch Thread)."},
            {"¿Por qué se eligió Hibernate en lugar de JDBC plano?", 
             "Hibernate automatiza la correspondencia objeto-relacional (ORM), lo que previene ataques de inyección SQL mediante consultas parametrizadas nativas y optimiza el rendimiento usando una caché de primer nivel para reducir las lecturas directas a la base de datos."}
        };

        for (String[] celda : bancoFAQ) {
            JPanel pPreguntaFila = new JPanel(new BorderLayout());
            pPreguntaFila.setOpaque(false);
            pPreguntaFila.setBorder(new LineBorder(new Color(0, 255, 255, 30), 1));

            JButton btnPregunta = new JButton(celda[0] + "   ▼");
            btnPregunta.setHorizontalAlignment(SwingConstants.LEFT);
            btnPregunta.setBackground(new Color(20, 20, 30));
            btnPregunta.setForeground(Color.CYAN);
            btnPregunta.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btnPregunta.setFocusPainted(false);
            btnPregunta.setBorderPainted(false);
            btnPregunta.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JTextArea txtRespuesta = new JTextArea(celda[1]);
            txtRespuesta.setLineWrap(true); txtRespuesta.setWrapStyleWord(true);
            txtRespuesta.setEditable(false); txtRespuesta.setOpaque(false);
            txtRespuesta.setForeground(Color.WHITE);
            txtRespuesta.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            txtRespuesta.setBorder(new EmptyBorder(8, 12, 8, 12));
            txtRespuesta.setVisible(false); 

            btnPregunta.addActionListener(e -> {
                boolean estadoActual = !txtRespuesta.isVisible();
                txtRespuesta.setVisible(estadoActual);
                btnPregunta.setText(celda[0] + (estadoActual ? "   ▲" : "   ▼"));
                d.revalidate();
                d.repaint();
            });

            pPreguntaFila.add(btnPregunta, BorderLayout.NORTH);
            pPreguntaFila.add(txtRespuesta, BorderLayout.CENTER);
            pFAQ.add(pPreguntaFila);
            pFAQ.add(Box.createRigidArea(new Dimension(0, 6)));
        }
        pScrollContenido.add(pFAQ);

        JScrollPane scrollGeneral = new JScrollPane(pScrollContenido);
        scrollGeneral.setOpaque(false);
        scrollGeneral.getViewport().setOpaque(false);
        scrollGeneral.setBorder(null);
        scrollGeneral.getVerticalScrollBar().setUnitIncrement(12);  
        pPanelNegro.add(scrollGeneral, BorderLayout.CENTER);

        JPanel pPieCierre = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pPieCierre.setOpaque(false);
        pPieCierre.setBorder(new EmptyBorder(10, 0, 5, 0));
        
        JButton btnCerrar = new JButton("VOLVER AL MENÚ PRINCIPAL");
        estilizarBotonPopup(btnCerrar);
        btnCerrar.addActionListener(e -> d.dispose());
        pPieCierre.add(btnCerrar);
        pPanelNegro.add(pPieCierre, BorderLayout.SOUTH);
        
        d.setVisible(true);
    }

    private void aplicarTema() {
        this.getContentPane().repaint();
        boolean modoOscuro = (usuarioSesion == null || usuarioSesion.isDarkMode());
        Color btnBg = modoOscuro ? new Color(25, 25, 33) : new Color(215, 215, 222);
        Color txt = modoOscuro ? Color.WHITE : Color.BLACK;

        for (java.awt.Component comp : panelFondo.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel subMod = (JPanel) comp;
                for (java.awt.Component interior : subMod.getComponents()) {
                    if (interior instanceof JButton) {
                        JButton btn = (JButton) interior;
                        if (!btn.getText().equals("SALIR DE LA APP")) {
                            btn.setBackground(btnBg);
                            btn.setForeground(txt);
                        }
                    }
                }
            } else if (comp instanceof JButton) {
                JButton btn = (comp instanceof JButton) ? (JButton) comp : null;
                if (btn != null && !btn.getText().equals("SALIR DE LA APP")) {
                    btn.setBackground(btnBg);
                    btn.setForeground(txt);
                }
            }
        }
        for (java.awt.Component comp : panelIconos.getComponents()) {
            if (comp instanceof JButton) {
                ((JButton) comp).setBackground(btnBg);
                ((JButton) comp).setForeground(txt);
            }
        }
    }

    private JButton crearBotonCuadrado(String rutaIcono, java.awt.event.ActionListener accion) {
        JButton boton = new JButton();
        boton.setPreferredSize(new Dimension(36, 36));
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setBorder(new LineBorder(new Color(0, 255, 255, 50), 1));
        boton.setOpaque(true);
        boton.setBackground(new Color(25, 25, 33));
        boton.setMargin(new Insets(0, 0, 0, 0));
        
        ImageIcon icon = new ImageIcon(rutaIcono);
        if (icon.getIconWidth() > 0) {
            Image img = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            boton.setIcon(new ImageIcon(img));
        } else { 
            boton.setText("?"); 
        }
        boton.addActionListener(accion);
        return boton;
    }

    private JButton crearBotonPro(String texto, String rutaIcono, java.awt.event.ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(420, 46));
        boton.setMinimumSize(new Dimension(420, 46));
        boton.setMaximumSize(new Dimension(420, 46));
        boton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        boton.setHorizontalAlignment(SwingConstants.CENTER); 
        boton.setHorizontalTextPosition(SwingConstants.RIGHT); 
        boton.setOpaque(true);
        boton.setBackground(new Color(25, 25, 33));
        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setBorder(new LineBorder(new Color(0, 255, 255, 35), 1));
        
        if (rutaIcono != null) {
            try {
                ImageIcon icon = new ImageIcon(rutaIcono);
                if (icon.getIconWidth() > 0) {
                    Image img = icon.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                    boton.setIcon(new ImageIcon(img));
                    boton.setIconTextGap(15);
                }
            } catch (Exception e) {}
        }
        
        boton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                boton.setBorder(new LineBorder(Color.CYAN, 1));
                boton.setBackground(new Color(35, 35, 48));
            }
            public void mouseExited(MouseEvent e) { 
                boton.setBorder(new LineBorder(new Color(0, 255, 255, 35), 1));
                boolean dark = (usuarioSesion == null || usuarioSesion.isDarkMode());
                boton.setBackground(dark ? new Color(25, 25, 33) : new Color(215, 215, 222));
            }
        });
        
        boton.addActionListener(accion);
        return boton;
    }

    private JButton crearBotonMatrizCuadrada(String texto, String rutaIcono, java.awt.event.ActionListener accion) {
        JButton boton = new JButton(texto);
        boton.setPreferredSize(new Dimension(195, 100));
        boton.setMinimumSize(new Dimension(195, 100));
        boton.setMaximumSize(new Dimension(195, 100));
        
        boton.setHorizontalAlignment(SwingConstants.CENTER);
        boton.setVerticalAlignment(SwingConstants.CENTER);
        boton.setHorizontalTextPosition(SwingConstants.CENTER);
        boton.setVerticalTextPosition(SwingConstants.BOTTOM);
        
        boton.setOpaque(true);
        boton.setBackground(new Color(25, 25, 33));
        boton.setForeground(Color.WHITE);
        boton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setBorder(new LineBorder(new Color(0, 255, 255, 35), 1));
        
        if (rutaIcono != null) {
            try {
                ImageIcon icon = new ImageIcon(rutaIcono);
                if (icon.getIconWidth() > 0) {
                    Image img = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    boton.setIcon(new ImageIcon(img));
                    boton.setIconTextGap(8); 
                }
            } catch (Exception e) {}
        }
        
        boton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { 
                boton.setBorder(new LineBorder(Color.CYAN, 1));
                boton.setBackground(new Color(35, 35, 48));
            }
            public void mouseExited(MouseEvent e) { 
                boton.setBorder(new LineBorder(new Color(0, 255, 255, 35), 1));
                boolean dark = (usuarioSesion == null || usuarioSesion.isDarkMode());
                boton.setBackground(dark ? new Color(25, 25, 33) : new Color(215, 215, 222));
            }
        });
        
        boton.addActionListener(accion);
        return boton;
    }

    private void iniciarReloj() { 
        new Timer(1000, e -> lblReloj.setText(new SimpleDateFormat("HH:mm:ss  ").format(new Date()))).start(); 
    }
    
    private void lanzarJuego(JPanel panelJuego, String tituloVentana) {
        long startTime = System.currentTimeMillis(); 
        JFrame v = new JFrame(tituloVentana);
        v.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        v.setResizable(false); 
        v.add(panelJuego); 
        v.pack(); 
        v.setLocationRelativeTo(null); 
        
        panelJuegoActivoRef = panelJuego; 
        
        if (panelJuego instanceof SnakeGame) {
            ((SnakeGame) panelJuego).setVolumenMasterBGM(volumenGlobalBGM);
        } else if (panelJuego instanceof TetrisGame) {
            ((TetrisGame) panelJuego).setVolumenMasterBGM(volumenGlobalBGM);
        }
        
        v.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                long duration = System.currentTimeMillis() - startTime;
                if (usuarioSesion != null) {
                    int seconds = (int)(duration / 1000);
                    int ptsPartida = Math.max(0, 5000 - seconds);
                    
                    if (panelJuego instanceof SnakeGame) {
                        usuarioSesion.setPuntos_snake(ptsPartida);
                    } else if (panelJuego instanceof Game2048) {
                        usuarioSesion.setPuntos_2048(ptsPartida);
                    } else if (panelJuego instanceof TetrisGame) {
                        usuarioSesion.setPuntos_tetris(ptsPartida);
                    }
                    usuarioRepo.save(usuarioSesion);
                }
                
                if (panelJuego instanceof PongGame) {
                    ((PongGame) panelJuego).detenerJuego();
                }
                if (panelJuego instanceof SnakeGame) {
                    ((SnakeGame) panelJuego).pararMusica();
                    ((SnakeGame) panelJuego).detenerJuego();
                } 
                if (panelJuego instanceof TetrisGame) {
                    ((TetrisGame) panelJuego).detenerJuego();
                }
                if (panelJuego instanceof com.retro.games.catchgame.CatchGame) {
                    ((com.retro.games.catchgame.CatchGame) panelJuego).detenerJuego();
                }
                if (panelJuego instanceof com.retro.games.breakbrick.BreakBrick) {
                    ((com.retro.games.breakbrick.BreakBrick) panelJuego).detenerJuego();
                }
                panelJuegoActivoRef = null; 
            }
        });
        v.setVisible(true);
        panelJuego.requestFocusInWindow();
    }

    private void mostrarAutenticacion() {
        if (usuarioSesion != null) {
            JOptionPane.showMessageDialog(this, "Operation cancelada: Ya existe una sesión activa: " + usuarioSesion.getUsername());
            return;
        }

        JDialog dialog = createStyledDialog("AUTENTICACIÓN DE OPERADOR", 450, 360); 
        dialog.setLayout(new BorderLayout());

        Color fondoArcadeOscuro = new Color(20, 20, 30);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(fondoArcadeOscuro); 
        tabbedPane.setForeground(Color.CYAN);
        tabbedPane.setBorder(null); 

        JPanel panelLogin = new JPanel(new GridBagLayout()); 
        panelLogin.setBackground(fondoArcadeOscuro); panelLogin.setOpaque(true);
        
        JPanel panelRegistro = new JPanel(new GridBagLayout()); 
        panelRegistro.setBackground(fondoArcadeOscuro); panelRegistro.setOpaque(true);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font fontLabels = new Font("Monospaced", Font.BOLD, 13);
        Color colorLabels = Color.WHITE;

        JTextField userLogin = new JTextField(); 
        userLogin.setPreferredSize(new Dimension(170, 28));
        userLogin.setBackground(new Color(15, 15, 20)); userLogin.setForeground(Color.WHITE);
        userLogin.setCaretColor(Color.CYAN); userLogin.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        userLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPasswordField passLogin = new JPasswordField(); 
        passLogin.setPreferredSize(new Dimension(170, 28));
        passLogin.setBackground(new Color(15, 15, 20)); passLogin.setForeground(Color.WHITE);
        passLogin.setCaretColor(Color.CYAN); passLogin.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        passLogin.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnVerLogin = conmutarBotonOcultacion(passLogin);

        JButton btnLogin = new JButton("ENTRAR AL SISTEMA");
        estilizarBotonPopup(btnLogin);

        JLabel lblUserL = new JLabel("USUARIO:"); lblUserL.setForeground(colorLabels); lblUserL.setFont(fontLabels);
        JLabel lblPassL = new JLabel("PASS:"); lblPassL.setForeground(colorLabels); lblPassL.setFont(fontLabels);
        
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 0; panelLogin.add(lblUserL, gbc);
        gbc.gridx = 1; panelLogin.add(userLogin, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; panelLogin.add(lblPassL, gbc);
        gbc.gridx = 1; panelLogin.add(passLogin, gbc);
        gbc.gridx = 2; panelLogin.add(btnVerLogin, gbc); 
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.insets = new Insets(15, 8, 8, 8);
        panelLogin.add(btnLogin, gbc);

        btnLogin.addActionListener(e -> {
            String username = userLogin.getText().trim();
            String password = new String(passLogin.getPassword());

            if (username.equalsIgnoreCase("admin") && password.equals("admin1234")) {
                Usuario admin = usuarioRepo.findAll().stream().filter(u -> u.getUsername().equalsIgnoreCase("admin")).findFirst().orElse(null);
                if (admin == null) {
                    admin = new Usuario();
                    admin.setUsername("admin");
                    admin.setPassword(encoder.encode("admin1234"));
                    admin.setDarkMode(true);
                    usuarioRepo.save(admin);
                }
                usuarioSesion = admin;
                aplicarTema();
                registrarLog("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] LOGIN: SuperUsuario 'admin' autenticado.");
                JOptionPane.showMessageDialog(dialog, "Acceso de SuperUsuario concedido.");
                dialog.dispose();
                return;
            }

            Usuario u = usuarioRepo.findAll().stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .findFirst().orElse(null);

            if (u != null && u.isBaneado()) {
                String motivo = registroOperadoresBaneadosConMotivo.getOrDefault(username.toLowerCase(), "Infracción de normas del sistema.");
                
                JDialog dBloqueo = createStyledDialog("SEGURIDAD DE SISTEMA", 450, 450);
                dBloqueo.setLayout(new BorderLayout(10, 10));
                
                JLabel lTexto = new JLabel("<html><center><font color='red' size='5'><b>ACCESO DENEGADO</b></font><br>"
                        + "<font color='white'>Operador baneado.</font><br><br>"
                        + "<font color='gray'>Motivo: " + motivo + "</font></center></html>");
                lTexto.setHorizontalAlignment(SwingConstants.CENTER);
                
                JLabel lRecla = new JLabel("RECLAMACIONES:");
                lRecla.setForeground(Color.CYAN);
                lRecla.setFont(new Font("Monospaced", Font.BOLD, 12));
                lRecla.setBorder(new EmptyBorder(0, 10, 0, 10));
                
                JTextArea areaRecla = new JTextArea(5, 20);
                areaRecla.setBackground(new Color(15, 15, 20));
                areaRecla.setForeground(Color.WHITE);
                areaRecla.setBorder(new LineBorder(Color.CYAN, 1));
                areaRecla.setLineWrap(true);
                
                JButton btnEnviar = new JButton("ENVIAR RECLAMACIÓN");
                estilizarBotonPopup(btnEnviar);
                btnEnviar.addActionListener(ev -> {
                    if(areaRecla.getText().trim().isEmpty()){
                        JOptionPane.showMessageDialog(dBloqueo, "El campo de reclamación está vacío.");
                    } else {
                        reclamacionRepo.save(new Reclamacion(username, areaRecla.getText()));
                        JOptionPane.showMessageDialog(dBloqueo, "Reclamación enviada al Administrador.");
                        dBloqueo.dispose();
                    }
                });
                
                JPanel pCentro = new JPanel(new BorderLayout(5, 5));
                pCentro.setOpaque(false);
                pCentro.add(lRecla, BorderLayout.NORTH);
                pCentro.add(new JScrollPane(areaRecla), BorderLayout.CENTER);
                
                dBloqueo.add(lTexto, BorderLayout.NORTH);
                dBloqueo.add(pCentro, BorderLayout.CENTER);
                dBloqueo.add(btnEnviar, BorderLayout.SOUTH);
                
                dBloqueo.setVisible(true);
                return;
            }

            if (u != null && encoder.matches(password, u.getPassword())) {
                
                if (u.isAvisarDesbaneo()) {
                    JDialog dNotificacion = createStyledDialog("NOTIFICACIÓN GENERAL", 440, 300);
                    dNotificacion.setLayout(new BorderLayout(15, 15));
                    
                    JLabel lblAlerta = new JLabel("<html><center><font color='cyan' size='6'><b>¡SISTEMA REACTIVADO!</b></font><br><br>"
                            + "<font color='white' size='4'>Su operador ha sido desbloqueado por el Administrador.</font><br><br>"
                            + "<font color='gray'>Las credenciales de acceso se han restablecido correctamente.</font></center></html>");
                    lblAlerta.setHorizontalAlignment(SwingConstants.CENTER);
                    lblAlerta.setBorder(new EmptyBorder(15, 15, 15, 15));
                    
                    JButton btnEntrarAviso = new JButton("COMPRENDIDO - ENTRAR");
                    estilizarBotonPopup(btnEntrarAviso);
                    btnEntrarAviso.addActionListener(evtAviso -> dNotificacion.dispose());
                    
                    dNotificacion.add(lblAlerta, BorderLayout.CENTER);
                    dNotificacion.add(btnEntrarAviso, BorderLayout.SOUTH);
                    dNotificacion.setVisible(true);
                    
                    u.setAvisarDesbaneo(false);
                    usuarioRepo.save(u); 
                }

                usuarioSesion = u;
                aplicarTema(); 
                registrarLog("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] LOGIN: Operador '" + username + "' conectado.");
                JOptionPane.showMessageDialog(dialog, "Token validado. Bienvenido: " + username);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Fallo de firma: Par de credenciales incorrecto.");
            }
        });
        gbc.gridwidth = 1; gbc.insets = new Insets(6, 6, 6, 6); 

        JLabel lblErrorReg = new JLabel(" ");
        lblErrorReg.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblErrorReg.setForeground(new Color(255, 80, 80));
        lblErrorReg.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField userReg = new JTextField(); 
        userReg.setPreferredSize(new Dimension(170, 28));
        userReg.setBackground(new Color(15, 15, 20)); userReg.setForeground(Color.WHITE);
        userReg.setCaretColor(Color.CYAN); userReg.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        userReg.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPasswordField passReg = new JPasswordField(); 
        passReg.setPreferredSize(new Dimension(170, 28));
        passReg.setBackground(new Color(15, 15, 20)); passReg.setForeground(Color.WHITE);
        passReg.setCaretColor(Color.CYAN); passReg.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        passReg.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JPasswordField passRegConfirm = new JPasswordField(); 
        passRegConfirm.setPreferredSize(new Dimension(170, 28));
        passRegConfirm.setBackground(new Color(15, 15, 20)); passRegConfirm.setForeground(Color.WHITE);
        passRegConfirm.setCaretColor(Color.CYAN); passRegConfirm.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        passRegConfirm.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnVerReg1 = conmutarBotonOcultacion(passReg);
        JButton btnVerReg2 = conmutarBotonOcultacion(passRegConfirm);

        JButton btnReg = new JButton("CREAR NUEVA CUENTA");
        estilizarBotonPopup(btnReg);

        JLabel lblUserR = new JLabel("NUEVO USER:"); lblUserR.setForeground(colorLabels); lblUserR.setFont(fontLabels);
        JLabel lblPassR = new JLabel("PASSWORD:"); lblPassR.setForeground(colorLabels); lblPassR.setFont(fontLabels);
        JLabel lblConfR = new JLabel("CONFIRMAR:"); lblConfR.setForeground(colorLabels); lblConfR.setFont(fontLabels);

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.insets = new Insets(0, 4, 10, 4);
        panelRegistro.add(lblErrorReg, gbc);

        gbc.gridwidth = 1; gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0; gbc.gridy = 1; panelRegistro.add(lblUserR, gbc);
        gbc.gridx = 1; panelRegistro.add(userReg, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; panelRegistro.add(lblPassR, gbc);
        gbc.gridx = 1; panelRegistro.add(passReg, gbc);
        gbc.gridx = 2; panelRegistro.add(btnVerReg1, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; panelRegistro.add(lblConfR, gbc);
        gbc.gridx = 1; panelRegistro.add(passRegConfirm, gbc);
        gbc.gridx = 2; panelRegistro.add(btnVerReg2, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; gbc.insets = new Insets(12, 6, 6, 6);
        panelRegistro.add(btnReg, gbc);

        btnReg.addActionListener(e -> {
            String username = userReg.getText().trim();
            String password = new String(passReg.getPassword());
            String confirm = new String(passRegConfirm.getPassword());

            if (username.isEmpty() || password.isEmpty()) return;
            
            if (!password.equals(confirm)) {
                lblErrorReg.setText("ERROR: Los campos de contraseña no concuerdan.");
                return;
            }

            if (!validarRobustezContrasena(password)) {
                lblErrorReg.setText(MENSAJE_REQUISITOS_PASS);
                return;
            }

            boolean existe = usuarioRepo.findAll().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));

            if (existe) {
                lblErrorReg.setText("ERROR: El identificador de usuario ya existe en persistencia.");
            } else {
                Usuario nuevo = new Usuario();
                nuevo.setUsername(username);
                nuevo.setPassword(encoder.encode(password));
                nuevo.setDarkMode(true);
                usuarioSesion = usuarioRepo.save(nuevo);
                aplicarTema(); 
                registrarLog("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] REGISTRO: Nuevo usuario '" + username + "' persistido.");
                JOptionPane.showMessageDialog(dialog, "Transacción completada: Cuenta creada con éxito.");
                dialog.dispose();
            }
        });

        tabbedPane.addTab(" INICIAR SESIÓN ", panelLogin);
        tabbedPane.addTab(" CREAR JUGADOR ", panelRegistro);
        dialog.add(tabbedPane, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
    
    private void mostrarPerfil() {
        JDialog d = createStyledDialog("PERFIL DEL OPERADOR", 460, 480);
        d.setLayout(new BorderLayout());

        JPanel panelContenedor = new JPanel();
        panelContenedor.setOpaque(false);
        panelContenedor.setLayout(new BoxLayout(panelContenedor, BoxLayout.Y_AXIS));
        panelContenedor.setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel tarjetaIdentidad = new JPanel(new BorderLayout(15, 0));
        tarjetaIdentidad.setOpaque(false);
        tarjetaIdentidad.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
        tarjetaIdentidad.setMaximumSize(new Dimension(420, 70));
        tarjetaIdentidad.setPreferredSize(new Dimension(420, 70));

        JPanel panelAvatar = new JPanel(new GridBagLayout());
        panelAvatar.setBackground(new Color(0, 255, 255, 20));
        panelAvatar.setPreferredSize(new Dimension(65, 70));
        JLabel lblAvatar = new JLabel("ID");
        lblAvatar.setFont(new Font("Monospaced", Font.BOLD, 22));
        lblAvatar.setForeground(Color.CYAN);
        panelAvatar.add(lblAvatar);
        tarjetaIdentidad.add(panelAvatar, BorderLayout.WEST);

        JPanel panelInfoUser = new JPanel(new GridLayout(2, 1, 0, 2));
        panelInfoUser.setOpaque(false);
        panelInfoUser.setBorder(new EmptyBorder(12, 10, 12, 10));
        
        JLabel lblNick = new JLabel("OPERADOR: " + usuarioSesion.getUsername().toUpperCase());
        lblNick.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblNick.setForeground(Color.WHITE);
        
        JLabel lblRol = new JLabel("RANGO: JUGADOR REGISTRADO en BBDD");
        lblRol.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblRol.setForeground(Color.GRAY);
        
        panelInfoUser.add(lblNick);
        panelInfoUser.add(lblRol);
        tarjetaIdentidad.add(panelInfoUser, BorderLayout.CENTER);
        
        panelContenedor.add(tarjetaIdentidad);
        panelContenedor.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel panelHistorial = new JPanel(new GridLayout(0, 2, 10, 12));
        panelHistorial.setOpaque(false);
        panelHistorial.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(255, 255, 255, 20), 1), 
            " HISTORIAL DE LOGROS ARCADE ", 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            new Font("Monospaced", Font.BOLD, 11), 
            Color.CYAN
        ));

        Font fontLabel = new Font("Monospaced", Font.BOLD, 13);
        Color colLabel = Color.WHITE;

        addStatRow(panelHistorial, "  SNAKE ARCADE:", usuarioSesion.getPuntos_snake() + " pts", fontLabel, colLabel);
        addStatRow(panelHistorial, "  2048 PUZZLE:", usuarioSesion.getPuntos_2048() + " pts", fontLabel, colLabel);
        addStatRow(panelHistorial, "  TETRIS CLASSIC:", usuarioSesion.getPuntos_tetris() + " pts", fontLabel, colLabel);
        addStatRow(panelHistorial, "  CATCH OR DROP:", usuarioSesion.getPuntos_catch() + " pts", fontLabel, colLabel);
        addStatRow(panelHistorial, "  BREAKBRICK:", usuarioSesion.getPuntos_break() + " pts", fontLabel, colLabel);

        int scoreTotal = calcularTotal(usuarioSesion);
        addStatRow(panelHistorial, "  SCORE TOTAL:", scoreTotal + " pts", fontLabel, Color.YELLOW);

        panelContenedor.add(panelHistorial);
        panelContenedor.add(Box.createRigidArea(new Dimension(0, 20)));

        JPanel panelSeguridad = new JPanel(new GridBagLayout());
        panelSeguridad.setOpaque(false);
        panelSeguridad.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(new Color(255, 255, 255, 20), 1), 
            " CONTROL DE SEGURIDAD ", 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            new Font("Monospaced", Font.BOLD, 11), 
            new Color(255, 80, 80)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JButton btnCambiarClave = new JButton("CAMBIAR CONTRASEÑA");
        estilizarBotonPopup(btnCambiarClave);
        btnCambiarClave.setBorder(new LineBorder(Color.CYAN, 1));
        
        btnCambiarClave.addActionListener(e -> {
            JDialog subDialog = new JDialog(d, "MODIFICAR FIRMA DIGITAL", true);
            subDialog.setSize(520, 410); 
            subDialog.setLocationRelativeTo(d);
            
            JPanel pForm = new JPanel(new GridBagLayout());
            pForm.setBackground(new Color(20, 20, 30));
            pForm.setBorder(new EmptyBorder(15, 15, 15, 15));
            subDialog.setContentPane(pForm);
            subDialog.getRootPane().setBorder(new LineBorder(Color.CYAN, 1));
            
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(6, 6, 6, 6);
            c.fill = GridBagConstraints.HORIZONTAL;
            
            Font fLabels = new Font("Monospaced", Font.BOLD, 12);
            
            JLabel lblFeedbackError = new JLabel(" ");
            lblFeedbackError.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lblFeedbackError.setHorizontalAlignment(SwingConstants.CENTER);
            lblFeedbackError.setForeground(new Color(255, 80, 80)); 
            
            JTextField txtUser = new JTextField(14);
            JPasswordField txtOldPass1 = new JPasswordField(14);
            JPasswordField txtOldPass2 = new JPasswordField(14);
            JPasswordField txtNewPass = new JPasswordField(14);
            
            JTextField[] camposInput = {txtUser, txtOldPass1, txtOldPass2, txtNewPass};
            for(JTextField f : camposInput) {
                f.setBackground(new Color(15, 15, 20));
                f.setForeground(Color.WHITE);
                f.setCaretColor(Color.CYAN);
                f.setBorder(new LineBorder(new Color(0, 255, 255, 60), 1));
                f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            }
            
            JButton btnVerMod1 = conmutarBotonOcultacion(txtOldPass1);
            JButton btnVerMod2 = conmutarBotonOcultacion(txtOldPass2);
            JButton btnVerMod3 = conmutarBotonOcultacion(txtNewPass);
            
            JLabel lUser = new JLabel("NOMBRE DE USUARIO:"); lUser.setForeground(Color.WHITE); lUser.setFont(fLabels);
            JLabel lOld1 = new JLabel("CONTRASEÑA ANTIGUA (1):"); lOld1.setForeground(Color.LIGHT_GRAY); lOld1.setFont(fLabels);
            JLabel lOld2 = new JLabel("CONTRASEÑA ANTIGUA (2):"); lOld2.setForeground(Color.LIGHT_GRAY); lOld2.setFont(fLabels);
            JLabel lNew = new JLabel("NUEVA CONTRASEÑA:"); lNew.setForeground(Color.CYAN); lNew.setFont(fLabels);
            
            JButton btnGuardarForm = new JButton("GUARDAR CAMBIOS");
            estilizarBotonPopup(btnGuardarForm);
            
            c.gridx = 0; c.gridy = 0; c.gridwidth = 3; c.insets = new Insets(0, 6, 12, 6);
            pForm.add(lblFeedbackError, c);
            
            c.gridwidth = 1; c.insets = new Insets(6, 6, 6, 6);
            c.gridx = 0; c.gridy = 1; pForm.add(lUser, c); c.gridx = 1; pForm.add(txtUser, c);
            
            c.gridx = 0; c.gridy = 2; pForm.add(lOld1, c); c.gridx = 1; pForm.add(txtOldPass1, c); c.gridx = 2; pForm.add(btnVerMod1, c);
            c.gridx = 0; c.gridy = 3; pForm.add(lOld2, c); c.gridx = 1; pForm.add(txtOldPass2, c); c.gridx = 2; pForm.add(btnVerMod2, c);
            c.gridx = 0; gbc.gridy = 4; pForm.add(lNew, c);  c.gridx = 1; pForm.add(txtNewPass, c);  c.gridx = 2; pForm.add(btnVerMod3, c);
            
            c.gridx = 0; c.gridy = 5; c.gridwidth = 3; c.insets = new Insets(20, 6, 6, 6);
            pForm.add(btnGuardarForm, c);
            
            btnGuardarForm.addActionListener(ev -> {
                String valUser = txtUser.getText().trim();
                String valOld1 = new String(txtOldPass1.getPassword());
                String valOld2 = new String(txtOldPass2.getPassword());
                String valNew  = new String(txtNewPass.getPassword()).trim();
                
                if (!valUser.equalsIgnoreCase(usuarioSesion.getUsername())) {
                    lblFeedbackError.setText("ERROR: El usuario no coincide con el operador activo.");
                    return;
                }
                if (!valOld1.equals(valOld2)) {
                    lblFeedbackError.setText("ERROR: Las contraseñas antiguas no coinciden entre sí.");
                    return;
                }
                if (!encoder.matches(valOld1, usuarioSesion.getPassword())) {
                    lblFeedbackError.setText("ERROR: La contraseña antigua introducida es incorrecta.");
                    return;
                }
                if (!validarRobustezContrasena(valNew)) {
                    lblFeedbackError.setText(MENSAJE_REQUISITOS_PASS);
                    return;
                }
                
                usuarioSesion.setPassword(encoder.encode(valNew));
                usuarioRepo.save(usuarioSesion);
                
                JOptionPane.showMessageDialog(d, "🔒 Contraseña guardada correctamente. Credenciales actualizadas.", "ÉXITO EN EL SISTEMA", JOptionPane.INFORMATION_MESSAGE);
                subDialog.dispose();
                d.dispose(); 
            });
            
            subDialog.setVisible(true);
        });
        
        panelSeguridad.add(btnCambiarClave, gbc);
        panelContenedor.add(panelSeguridad);

        d.add(panelContenedor, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void mostrarAjustes() {
        JDialog d = createStyledDialog("CENTRO DE CONFIGURACIÓN AVANZADA", 460, 560);
        d.setLayout(new BorderLayout(0, 0));

        JLabel lblTitulo = new JLabel("AJUSTES INTERNOS", JLabel.CENTER);
        lblTitulo.setFont(new Font("Monospaced", Font.BOLD, 26));
        lblTitulo.setForeground(Color.CYAN);
        lblTitulo.setBorder(new EmptyBorder(15, 0, 15, 0));
        d.add(lblTitulo, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(25, 25, 35));
        tabs.setForeground(Color.CYAN);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel pEstiloAudio = new JPanel(new GridBagLayout());
        pEstiloAudio.setOpaque(false);
        GridBagConstraints gbcEstilo = new GridBagConstraints();
        gbcEstilo.fill = GridBagConstraints.HORIZONTAL;
        gbcEstilo.insets = new Insets(8, 15, 8, 15);
        gbcEstilo.gridx = 0; gbcEstilo.gridy = 0;

        JCheckBox checkDark = new JCheckBox("  MODO OSCURO ACTIVO", usuarioSesion.isDarkMode());
        checkDark.setOpaque(false); checkDark.setForeground(Color.WHITE);
        checkDark.setFont(new Font("Consolas", Font.BOLD, 13));
        pEstiloAudio.add(checkDark, gbcEstilo);

        gbcEstilo.gridy++;
        JLabel lTema = new JLabel("TEMA DEL LIENZO DE FONDO:");
        lTema.setForeground(Color.LIGHT_GRAY); lTema.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pEstiloAudio.add(lTema, gbcEstilo);

        gbcEstilo.gridy++;
        JComboBox<String> comboTemas = new JComboBox<>(new String[]{"Cyber Cyan", "Retro Red", "Midnight Purple"});
        comboTemas.setBackground(new Color(35, 35, 45)); comboTemas.setForeground(Color.WHITE);
        comboTemas.addActionListener(e -> {
            String tema = (String) comboTemas.getSelectedItem();
            if ("Retro Red".equals(tema)) { colorFondo1 = new Color(35, 6, 6); colorFondo2 = new Color(65, 12, 12); }
            else if ("Midnight Purple".equals(tema)) { colorFondo1 = new Color(18, 6, 28); colorFondo2 = new Color(38, 12, 48); }
            else { colorFondo1 = new Color(7, 7, 12); colorFondo2 = new Color(18, 22, 32); }
            panelFondo.repaint();
        });
        pEstiloAudio.add(comboTemas, gbcEstilo);

        gbcEstilo.gridy++;
        JLabel lVol = new JLabel("VOLUMEN DE MÚSICA DE FONDO (BGM):");
        lVol.setForeground(Color.CYAN); lVol.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pEstiloAudio.add(lVol, gbcEstilo);

        gbcEstilo.gridy++;
        JSlider sliderVol = new JSlider(0, 100, (int)(volumenGlobalBGM * 100));
        sliderVol.setOpaque(false); sliderVol.setForeground(Color.CYAN);
        sliderVol.setPaintTicks(true); sliderVol.setMajorTickSpacing(25);
        sliderVol.addChangeListener(e -> {
            volumenGlobalBGM = sliderVol.getValue() / 100.0f;
            if (panelJuegoActivoRef != null) {
                if (panelJuegoActivoRef instanceof SnakeGame) ((SnakeGame) panelJuegoActivoRef).setVolumenMasterBGM(volumenGlobalBGM);
                if (panelJuegoActivoRef instanceof TetrisGame) ((TetrisGame) panelJuegoActivoRef).setVolumenMasterBGM(volumenGlobalBGM);
            }
        });
        pEstiloAudio.add(sliderVol, gbcEstilo);
        
        tabs.addTab(" GENERAL ", pEstiloAudio);

        JPanel pHistorial = new JPanel(new BorderLayout(5, 5));
        pHistorial.setOpaque(false); pHistorial.setBorder(new EmptyBorder(10, 10, 10, 10));
        JTextArea areaConsola = new JTextArea();
        areaConsola.setEditable(false); areaConsola.setBackground(new Color(12, 12, 18));
        areaConsola.setForeground(new Color(180, 255, 180)); areaConsola.setFont(new Font("Monospaced", Font.PLAIN, 12));
        for (String log : logsAutenticacionLocal) { areaConsola.append(log + "\n"); }
        pHistorial.add(new JScrollPane(areaConsola), BorderLayout.CENTER);

        JPanel pDiag = new JPanel(new GridBagLayout());
        pDiag.setOpaque(false);
        GridBagConstraints cDiag = new GridBagConstraints();
        cDiag.fill = GridBagConstraints.HORIZONTAL; cDiag.insets = new Insets(10, 15, 10, 15);
        cDiag.gridx = 0; cDiag.gridy = 0;

        JLabel lblRam = new JLabel("MEMORIA RAM ASIGNADA: CALCULANDO MB...");
        lblRam.setForeground(Color.GREEN); lblRam.setFont(new Font("Monospaced", Font.BOLD, 12));
        pDiag.add(lblRam, cDiag);

        Timer timerRam = new Timer(1000, e -> {
            long memUsada = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
            lblRam.setText("MEMORIA RAM ASIGNADA: " + memUsada + " MB / JVM TOTAL");
        });
        timerRam.start();
        d.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { timerRam.stop(); } });

        cDiag.gridy++;
        JButton btnLimpiar = new JButton("EJECUTAR LIMPIEZA DE ARCHIVOS .LOG");
        estilizarBotonPopup(btnLimpiar);
        
        btnLimpiar.addActionListener(e -> {
            File ruta = new File(".");
            File[] logs = ruta.listFiles((d1, n) -> nameEndsWithTmpLog(n));
            int eliminados = 0;
            if (logs != null) for (File f : logs) { if (f.delete()) eliminados++; }
            
            logsAutenticacionLocal.clear(); 
            if (logRepo != null) logRepo.deleteAll();
            
            String msgPurga = "[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] CONSOLA: Historial purgado por el Administrador.";
            registrarLog(msgPurga);
            areaConsola.setText(msgPurga + "\n");
            
            mostrarToast(d, "Limpieza completada con éxito. Archivos eliminados: " + eliminados);
        });
        boolean esAdmin = usuarioSesion != null && "admin".equalsIgnoreCase(usuarioSesion.getUsername());

        if (esAdmin) { 
            pDiag.add(btnLimpiar, cDiag); 
            tabs.addTab(" LOGS ", pHistorial); 
            
            JPanel pRecla = new JPanel(new BorderLayout(5, 5));
            pRecla.setOpaque(false);
            pRecla.setBorder(new EmptyBorder(10, 10, 10, 10));
            
            DefaultListModel<String> modelList = new DefaultListModel<>();
            List<Reclamacion> listaReclamaciones = reclamacionRepo.findAll(); 

            for (Reclamacion r : listaReclamaciones) {
                modelList.addElement("OPERADOR: " + r.getUsername().toUpperCase());
                modelList.addElement("RECLAMACIÓN: " + r.getMensaje());
                modelList.addElement("------------------------------------");
            }
            
            JList<String> listRecla = new JList<>(modelList);
            listRecla.setBackground(new Color(15, 15, 20));
            listRecla.setForeground(Color.CYAN); 
            listRecla.setFont(new Font("Monospaced", Font.BOLD, 12));
            listRecla.setBorder(new LineBorder(Color.CYAN, 1));
            
            JButton btnAceptar = new JButton("ACEPTAR APELACIÓN Y REACTIVAR");
            estilizarBotonPopup(btnAceptar);
            btnAceptar.setBackground(new Color(25, 60, 25)); 
            
            btnAceptar.addActionListener(ev -> {
                String seleccion = listRecla.getSelectedValue();
                if (seleccion != null && seleccion.startsWith("OPERADOR: ")) {
                    String nick = seleccion.replace("OPERADOR: ", "").trim();
                    
                    Usuario usr = usuarioRepo.findAll().stream()
                        .filter(user -> user.getUsername().equalsIgnoreCase(nick))
                        .findFirst().orElse(null);
                        
                    if(usr != null) {
                        usr.setBaneado(false); 
                        usr.setAvisarDesbaneo(true); 
                        usuarioRepo.save(usr); 
                    }
                    
                    List<Reclamacion> recls = reclamacionRepo.findAll();
                    for(Reclamacion r : recls) {
                        if(r.getUsername().equalsIgnoreCase(nick)) {
                            reclamacionRepo.delete(r);
                        }
                    }
                    
                    registroOperadoresBaneadosConMotivo.remove(nick.toLowerCase());
                    JOptionPane.showMessageDialog(d, "Usuario " + nick + " ha sido reactivado y su reclamación archivada.");
                    d.dispose(); 
                } else {
                    JOptionPane.showMessageDialog(d, "Seleccione una línea de operador válida.");
                }
            });
            
            pRecla.add(new JScrollPane(listRecla), BorderLayout.CENTER);
            pRecla.add(btnAceptar, BorderLayout.SOUTH); 
            tabs.addTab(" RECLAMACIONES ", pRecla);
        } else {
            JLabel lRestriccion = new JLabel("[MÓDULO RESERVADO PARA OPERADORES ROOT]");
            lRestriccion.setForeground(Color.RED); lRestriccion.setFont(new Font("Monospaced", Font.ITALIC, 11));
            pDiag.add(lRestriccion, cDiag);
        }
        tabs.addTab(" DIAGNÓSTICO ", pDiag);

        d.add(tabs, BorderLayout.CENTER);

        JPanel pAccionesPie = new JPanel(new GridBagLayout());
        pAccionesPie.setOpaque(false);
        pAccionesPie.setBorder(new EmptyBorder(10, 20, 20, 20));
        GridBagConstraints cPie = new GridBagConstraints();
        cPie.fill = GridBagConstraints.HORIZONTAL; cPie.gridx = 0; cPie.gridy = 0; cPie.insets = new Insets(5, 0, 5, 0);

        JButton btnInfo = new JButton("ABRIR GUÍA DE CONTROLES DE JUEGOS");
        estilizarBotonPopup(btnInfo);
        btnInfo.addActionListener(e -> mostrarInfoAtajos());
        pAccionesPie.add(btnInfo, cPie);

        cPie.gridy++;
        JButton btnGuardar = new JButton("CONFIRMAR Y APLICAR CAMBIOS");
        estilizarBotonPopup(btnGuardar);
        btnGuardar.addActionListener(e -> {
            usuarioSesion.setDarkMode(checkDark.isSelected());
            usuarioRepo.save(usuarioSesion);
            aplicarTema(); d.dispose();
            mostrarToast(this, "Ajustes del sistema aplicados con éxito.");
        });
        pAccionesPie.add(btnGuardar, cPie);

        cPie.gridy++;
        JButton btnLogout = new JButton("CERRAR SESIÓN DE USUARIO");
        estilizarBotonPopup(btnLogout); btnLogout.setBackground(new Color(85, 25, 25));
        btnLogout.addActionListener(e -> {
            registrarLog("[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] LOGOUT: Fin de sesión de " + usuarioSesion.getUsername());
            usuarioSesion = null; aplicarTema(); d.dispose();
            JOptionPane.showMessageDialog(this, "Sesión finalizada de forma segura.");
        });
        pAccionesPie.add(btnLogout, cPie);

        d.add(pAccionesPie, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private boolean nameEndsWithTmpLog(String name) {
        return name.endsWith(".tmp") || name.endsWith(".log");
    }

    private void mostrarToast(Window framePadre, String msg) {
        JDialog toast = new JDialog(framePadre);
        toast.setUndecorated(true);
        toast.setLayout(new GridBagLayout());
        toast.setBackground(new Color(10, 15, 22, 235));
        toast.getRootPane().setBorder(new LineBorder(Color.CYAN, 1));
        
        JLabel texto = new JLabel(msg);
        texto.setForeground(Color.WHITE);
        texto.setFont(new Font("Segoe UI", Font.BOLD, 12));
        toast.add(texto);
        
        toast.setSize(360, 45);
        toast.setLocation(framePadre.getX() + (framePadre.getWidth() - 360)/2, framePadre.getY() + 380);
        toast.setVisible(true);
        
        new Timer(2400, e -> toast.dispose()).start();
    }

    private void mostrarInfoAtajos() {
        JDialog d = createStyledDialog("CENTRO DE AYUDA Y CONTROLES", 400, 500);
        d.setLayout(new BorderLayout());

        JPanel pAcordeon = new JPanel();
        pAcordeon.setLayout(new BoxLayout(pAcordeon, BoxLayout.Y_AXIS));
        pAcordeon.setBackground(new Color(20, 20, 30));
        pAcordeon.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[][] secciones = {
            {"SNAKE ARCADE", "Muévete con WASD o Flechas. El Admin puede pausar con 'P' o completar niveles con 'RePág'."},
            {"TETRIS CLASSIC", "Flechas para mover, Arriba para rotar. Espacio para caída instantánea."},
            {"2048 PUZZLE", "Desliza las fichas con las flechas. Combina números iguales para alcanzar el 2048."},
            {"PONG RETRO", "Jugador 1: W/S. Jugador 2: Flechas Arriba/Abajo. ¡Mantén la pelota en juego!"}
        };

        for (String[] seccion : secciones) {
            JPanel pItem = new JPanel(new BorderLayout());
            pItem.setOpaque(false);
            pItem.setBorder(new LineBorder(new Color(0, 255, 255, 30), 1));
            pItem.setMaximumSize(new Dimension(380, 50));

            JButton btnHeader = new JButton(seccion[0] + "  ▼");
            btnHeader.setHorizontalAlignment(SwingConstants.LEFT);
            btnHeader.setBackground(new Color(30, 30, 45));
            btnHeader.setForeground(Color.CYAN);
            btnHeader.setFocusPainted(false);
            btnHeader.setBorderPainted(false);
            btnHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));

            JTextArea txtInfo = new JTextArea(seccion[1]);
            txtInfo.setLineWrap(true); txtInfo.setWrapStyleWord(true);
            txtInfo.setEditable(false); txtInfo.setOpaque(false);
            txtInfo.setForeground(Color.WHITE);
            txtInfo.setVisible(false); 

            btnHeader.addActionListener(e -> {
                boolean visible = !txtInfo.isVisible();
                txtInfo.setVisible(visible);
                btnHeader.setText(seccion[0] + (visible ? "  ▲" : "  ▼"));
                pItem.setMaximumSize(new Dimension(380, visible ? 120 : 50));
                d.revalidate();
                d.repaint();
            });

            pItem.add(btnHeader, BorderLayout.NORTH);
            pItem.add(txtInfo, BorderLayout.CENTER);
            pAcordeon.add(pItem);
            pAcordeon.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        d.add(new JScrollPane(pAcordeon), BorderLayout.CENTER);
        JButton btnCerrar = new JButton("VOLVER AL CENTRO DE CONTROL");
        estilizarBotonPopup(btnCerrar);
        btnCerrar.addActionListener(e -> d.dispose());
        d.add(btnCerrar, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void mostrarRanking() {
        JDialog ventanaRanking = createStyledDialog("RANKING GLOBAL DE MÉRITOS", 900, 680);
        ventanaRanking.setLayout(new BorderLayout(10, 10));
        
        List<Usuario> usuarios = usuarioRepo.findAll();
        usuarios.removeIf(u -> "admin".equalsIgnoreCase(u.getUsername()));
        usuarios.removeIf(Usuario::isBaneado);
        usuarios.sort((u1, u2) -> Integer.compare(calcularTotal(u2), calcularTotal(u1)));
        
        JPanel panelSuperior = new JPanel();
        panelSuperior.setOpaque(false);
        panelSuperior.setLayout(new BoxLayout(panelSuperior, BoxLayout.Y_AXIS));
        panelSuperior.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel pBusqueda = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        pBusqueda.setOpaque(false);
        JLabel lblBuscar = new JLabel("BUSCAR JUGADOR: ");
        lblBuscar.setForeground(Color.CYAN);
        lblBuscar.setFont(new Font("Monospaced", Font.BOLD, 13));
        
        JTextField txtBuscar = new JTextField(18);
        txtBuscar.setBackground(new Color(15, 15, 20));
        txtBuscar.setForeground(Color.WHITE);
        txtBuscar.setCaretColor(Color.CYAN);
        txtBuscar.setBorder(new LineBorder(Color.CYAN, 1));
        txtBuscar.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pBusqueda.add(lblBuscar); pBusqueda.add(txtBuscar);
        panelSuperior.add(pBusqueda);
        panelSuperior.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel pPodio = new JPanel(new GridLayout(1, 3, 15, 0));
        pPodio.setOpaque(false);
        pPodio.setPreferredSize(new Dimension(800, 90));
        pPodio.setMaximumSize(new Dimension(800, 90));

        String top1 = (usuarios.size() > 0) ? usuarios.get(0).getUsername().toUpperCase() + " (" + calcularTotal(usuarios.get(0)) + ")" : "---";
        String top2 = (usuarios.size() > 1) ? usuarios.get(1).getUsername().toUpperCase() + " (" + calcularTotal(usuarios.get(1)) + ")" : "---";
        String top3 = (usuarios.size() > 2) ? usuarios.get(2).getUsername().toUpperCase() + " (" + calcularTotal(usuarios.get(2)) + ")" : "---";

        JPanel card2 = new JPanel(new GridLayout(2, 1)); card2.setBackground(new Color(30, 35, 45));
        card2.setBorder(new LineBorder(new Color(192, 192, 192, 150), 1));
        JLabel lPlata = new JLabel("🥈 2nd PLACE", JLabel.CENTER); lPlata.setForeground(new Color(192, 192, 192)); lPlata.setFont(new Font("Monospaced", Font.BOLD, 12));
        JLabel lPlataUser = new JLabel(top2, JLabel.CENTER); lPlataUser.setForeground(Color.WHITE); lPlataUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card2.add(lPlata); card2.add(lPlataUser);

        JPanel card1 = new JPanel(new GridLayout(2, 1)); card1.setBackground(new Color(45, 40, 30));
        card1.setBorder(new LineBorder(new Color(255, 215, 0), 2)); 
        JLabel lOro = new JLabel("👑 1st CHAMPION", JLabel.CENTER); lOro.setForeground(new Color(255, 215, 0)); lOro.setFont(new Font("Monospaced", Font.BOLD, 13));
        JLabel lOroUser = new JLabel(top1, JLabel.CENTER); lOroUser.setForeground(Color.WHITE); lOroUser.setFont(new Font("Segoe UI", Font.BOLD, 14));
        card1.add(lOro); card1.add(lOroUser);

        JPanel card3 = new JPanel(new GridLayout(2, 1)); card3.setBackground(new Color(35, 28, 25));
        card3.setBorder(new LineBorder(new Color(210, 105, 30, 150), 1));
        JLabel lBronce = new JLabel("🥉 3rd PLACE", JLabel.CENTER); lBronce.setForeground(new Color(210, 105, 30)); lBronce.setFont(new Font("Monospaced", Font.BOLD, 12));
        JLabel lBronceUser = new JLabel(top3, JLabel.CENTER); lBronceUser.setForeground(Color.WHITE); lBronceUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        card3.add(lBronce); card3.add(lBronceUser);

        pPodio.add(card2); pPodio.add(card1); pPodio.add(card3);
        panelSuperior.add(pPodio);
        ventanaRanking.add(panelSuperior, BorderLayout.NORTH);

        String[] columnas = {"POS", "JUGADOR", "TIME SNAKE", "MAX 2048", "PUNTOS TETRIS", "P. CATCH", "P. BREAK", "TOTAL"};
        DefaultTableModel modelo = new DefaultTableModel(columnas, 0) { 
            private static final long serialVersionUID = 1L;
            @Override public boolean isCellEditable(int r, int c) { return false; } 
        };
        
        int pos = 1;
        for (Usuario u : usuarios) {
            String labelPos = (pos==1) ? "[1st]" : (pos==2) ? "[2nd]" : (pos==3) ? "[3rd]" : String.valueOf(pos) + "º";
            modelo.addRow(new Object[]{ labelPos, u.getUsername(), u.getPuntos_snake() + "s", u.getPuntos_2048(), u.getPuntos_tetris(), u.getPuntos_catch(), u.getPuntos_break(), calcularTotal(u) });
            pos++;
        }
        
        JTable tabla = new JTable(modelo);
        tabla.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tabla.setBackground(new Color(25, 25, 35));
        tabla.setForeground(Color.WHITE);
        tabla.setRowHeight(28);
        tabla.setSelectionBackground(new Color(0, 255, 255, 40));
        tabla.setSelectionForeground(Color.CYAN);
        tabla.setGridColor(new Color(0, 255, 255, 30));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(80);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        tabla.setRowSorter(sorter);
        txtBuscar.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                String texto = txtBuscar.getText();
                if (texto.trim().length() == 0) { sorter.setRowFilter(null); } 
                else { sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto, 1)); } 
            }
        });
        
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                java.awt.Component c = super.getTableCellRendererComponent(t, val, isSelected, hasFocus, row, col);
                if (isSelected) {
                    c.setForeground(Color.CYAN);
                } else {
                    String posText = t.getValueAt(row, 0).toString();
                    if ("[1st]".equals(posText)) { c.setForeground(new Color(255, 215, 0)); } 
                    else if ("[2nd]".equals(posText)) { c.setForeground(new Color(192, 192, 192)); } 
                    else if ("[3rd]".equals(posText)) { c.setForeground(new Color(210, 105, 30)); } 
                    else { c.setForeground(Color.WHITE); }
                }
                return c;
            }
        });
        
        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setBackground(new Color(40, 40, 50));
        header.setForeground(Color.CYAN);
        header.setBorder(new LineBorder(new Color(0, 255, 255, 50), 1));

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.getViewport().setBackground(new Color(20, 20, 30));
        scrollPane.setBorder(new LineBorder(new Color(0, 255, 255, 30), 1));
        
        JPanel panelContenedor = new JPanel(new BorderLayout());
        panelContenedor.setOpaque(false);
        panelContenedor.setBorder(new EmptyBorder(0, 15, 15, 15));
        panelContenedor.add(scrollPane, BorderLayout.CENTER);
        ventanaRanking.add(panelContenedor, BorderLayout.CENTER);
        
        boolean esAdmin = usuarioSesion != null && "admin".equalsIgnoreCase(usuarioSesion.getUsername());
        
        if (esAdmin) {
            JPanel panelAdmin = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
            panelAdmin.setOpaque(false);
            
            JButton btnBanear = new JButton("BANEAR OPERADOR");
            JButton btnEditar = new JButton("EDITAR OPERADOR");
            
            estilizarBotonPopup(btnEditar);
            estilizarBotonPopup(btnBanear);
            
            btnBanear.setBorder(new LineBorder(new Color(255, 0, 0, 100), 1));
            btnBanear.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btnBanear.setBackground(new Color(70, 20, 20)); }
                public void mouseExited(MouseEvent e) { btnBanear.setBackground(new Color(40, 40, 50)); }
            });
            
            btnBanear.addActionListener(e -> {
                int fila = tabla.getSelectedRow();
                if (fila != -1) {
                    int filaModelo = tabla.convertRowIndexToModel(fila); 
                    Usuario u = usuarios.get(filaModelo);
                    
                    JDialog dBaneo = createStyledDialog("REGISTRO DE INFRACCIÓN", 400, 250);
                    JPanel pCont = (JPanel) dBaneo.getContentPane();
                    pCont.setLayout(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    
                    JLabel lblMsg = new JLabel("<html><center>Motivo oficial del baneo para:<br><font color='cyan'>" + u.getUsername().toUpperCase() + "</font></center></html>");
                    lblMsg.setForeground(Color.WHITE);
                    lblMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    
                    JTextField txtMotivo = new JTextField(20);
                    txtMotivo.setBackground(new Color(15, 15, 20));
                    txtMotivo.setForeground(Color.WHITE);
                    txtMotivo.setCaretColor(Color.CYAN);
                    txtMotivo.setBorder(new LineBorder(Color.CYAN, 1));
                    
                    JButton btnConfirmar = new JButton("CONFIRMAR BANEO");
                    estilizarBotonPopup(btnConfirmar);
                    btnConfirmar.setBackground(new Color(85, 25, 25));
                    
                    gbc.insets = new Insets(10, 10, 10, 10);
                    gbc.gridx = 0; gbc.gridy = 0; pCont.add(lblMsg, gbc);
                    gbc.gridy = 1; pCont.add(txtMotivo, gbc);
                    gbc.gridy = 2; pCont.add(btnConfirmar, gbc);
                    
                    btnConfirmar.addActionListener(ev -> {
                        String motivo = txtMotivo.getText().trim();
                        if (motivo.isEmpty()) {
                            JOptionPane.showMessageDialog(dBaneo, "El campo 'Motivo' no puede estar vacío.");
                            return;
                        }
                        
                        u.setBaneado(true);
                        usuarioRepo.save(u); 
                        
                        registroOperadoresBaneadosConMotivo.put(u.getUsername().toLowerCase(), motivo);
                        dBaneo.dispose();
                        ventanaRanking.dispose();
                        mostrarRanking(); 
                    });
                    
                    dBaneo.setVisible(true);
                }
            });
            
            btnEditar.addActionListener(e -> {
                int fila = tabla.getSelectedRow();
                if (fila != -1) {
                    int filaModelo = tabla.convertRowIndexToModel(fila); 
                    Usuario u = usuarios.get(filaModelo);
                    
                    JDialog dEditar = createStyledDialog("EDITAR MÉRITOS: " + u.getUsername().toUpperCase(), 420, 420);
                    JPanel pCont = (JPanel) dEditar.getContentPane();
                    pCont.setLayout(new GridBagLayout());
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.insets = new Insets(8, 10, 8, 10);
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    
                    Font fLabels = new Font("Monospaced", Font.BOLD, 12);
                    
                    String[] labels = {"SNAKE (Segundos):", "2048 PUZZLE:", "TETRIS CLASSIC:", "CATCH OR DROP:", "BREAKBRICK:"};
                    int[] values = {u.getPuntos_snake(), u.getPuntos_2048(), u.getPuntos_tetris(), u.getPuntos_catch(), u.getPuntos_break()};
                    JTextField[] txtFields = new JTextField[5];
                    
                    for (int i = 0; i < 5; i++) {
                        JLabel lbl = new JLabel(labels[i]);
                        lbl.setForeground(Color.WHITE);
                        lbl.setFont(fLabels);
                        
                        txtFields[i] = new JTextField(String.valueOf(values[i]), 10);
                        txtFields[i].setBackground(new Color(15, 15, 20));
                        txtFields[i].setForeground(Color.WHITE);
                        txtFields[i].setCaretColor(Color.CYAN);
                        txtFields[i].setBorder(new LineBorder(Color.CYAN, 1));
                        txtFields[i].setFont(new Font("Consolas", Font.PLAIN, 14));
                        txtFields[i].setHorizontalAlignment(JTextField.CENTER);
                        
                        gbc.gridx = 0; gbc.gridy = i; pCont.add(lbl, gbc);
                        gbc.gridx = 1; pCont.add(txtFields[i], gbc);
                    }
                    
                    JButton btnGuardarCambios = new JButton("GUARDAR CAMBIOS");
                    estilizarBotonPopup(btnGuardarCambios);
                    
                    gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2; gbc.insets = new Insets(20, 10, 10, 10);
                    pCont.add(btnGuardarCambios, gbc);
                    
                    btnGuardarCambios.addActionListener(ev -> {
                        try {
                            u.setPuntos_snake(Integer.parseInt(txtFields[0].getText().trim()));
                            u.setPuntos_2048(Integer.parseInt(txtFields[1].getText().trim()));
                            u.setPuntos_tetris(Integer.parseInt(txtFields[2].getText().trim()));
                            u.setPuntos_catch(Integer.parseInt(txtFields[3].getText().trim()));
                            u.setPuntos_break(Integer.parseInt(txtFields[4].getText().trim()));
                            
                            usuarioRepo.save(u); 
                            JOptionPane.showMessageDialog(dEditar, "Puntuaciones actualizadas correctamente.");
                            dEditar.dispose();
                            ventanaRanking.dispose();
                            mostrarRanking(); 
                        } catch(NumberFormatException ex) {
                            JOptionPane.showMessageDialog(dEditar, "ERROR: Ingrese solo valores numéricos válidos.");
                        }
                    });
                    
                    dEditar.setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(ventanaRanking, "Seleccione un jugador de la tabla primero.");
                }
            });
            
            panelAdmin.add(btnEditar);
            panelAdmin.add(btnBanear);
            ventanaRanking.add(panelAdmin, BorderLayout.SOUTH);
        }
        ventanaRanking.setVisible(true);
    }
    
    private JDialog createStyledDialog(String title, int w, int h) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(w, h);
        d.setLocationRelativeTo(this);
        JPanel p = new JPanel() {
            private static final long serialVersionUID = 1L;
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0, new Color(20,20,30), 0, getHeight(), new Color(10,10,15)));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        d.setContentPane(p);
        d.getRootPane().setBorder(new LineBorder(Color.CYAN, 2));
        return d;
    }

    private void estilizarBotonPopup(JButton b) {
        b.setPreferredSize(new Dimension(220, 40));
        b.setBackground(new Color(40, 40, 50));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setFocusPainted(false);
        b.setBorder(new LineBorder(Color.CYAN, 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(new Color(60, 60, 70)); }
            public void mouseExited(MouseEvent e) { b.setBackground(new Color(40, 40, 50)); }
        });
    }

    private void addStatRow(JPanel p, String label, String value, Font f, Color c) {
        JLabel l1 = new JLabel(label); l1.setForeground(c); l1.setFont(f);
        JLabel l2 = new JLabel(value); l2.setForeground(Color.WHITE); l2.setFont(f);
        p.add(l1); p.add(l2);
    }

    private void salirAlMenuPrincipal() {
        detenerJuego();
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        if (topFrame != null) {
            topFrame.dispose();
        }
    }

    public void detenerJuego() {
    }

    private int calcularTotal(Usuario u) {
        if (u == null) return 0;
        int ptsSnake = (u.getPuntos_snake() > 0) ? Math.max(0, 5000 - u.getPuntos_snake()) : 0;
        return ptsSnake + u.getPuntos_2048() + u.getPuntos_tetris() + u.getPuntos_catch() + u.getPuntos_break();
    }

    private boolean validarRobustezContrasena(String password) {
        if (password == null || password.length() < 6) return false;
        
        boolean tieneMayuscula = false;
        boolean tieneMiniscula = false;
        boolean tieneNumero = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) tieneMayuscula = true;
            else if (Character.isLowerCase(c)) tieneMiniscula = true;
            else if (Character.isDigit(c)) tieneNumero = true;
        }
        
        return tieneMayuscula && tieneMiniscula && tieneNumero;
    }

    private JButton conmutarBotonOcultacion(JPasswordField campoClave) {
        JButton btnVer = new JButton("ver");
        btnVer.setPreferredSize(new Dimension(55, 28));
        btnVer.setFont(new Font("Consolas", Font.BOLD, 11));
        btnVer.setBackground(new Color(30, 30, 40));
        btnVer.setForeground(Color.CYAN);
        btnVer.setBorder(new LineBorder(new Color(0, 255, 255, 40), 1));
        btnVer.setFocusPainted(false);
        btnVer.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnVer.addActionListener(e -> {
            if (btnVer.getText().equals("ver")) {
                campoClave.setEchoChar((char) 0); 
                btnVer.setText("no ver");
                btnVer.setForeground(Color.LIGHT_GRAY);
                btnVer.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            } else {
                campoClave.setEchoChar('•'); 
                btnVer.setText("ver");
                btnVer.setForeground(Color.CYAN);
                btnVer.setBorder(new LineBorder(new Color(0, 255, 255, 40), 1));
            }
            campoClave.requestFocusInWindow(); 
        });

        return btnVer;
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception e) {}
        ConfigurableApplicationContext context = new SpringApplicationBuilder(MenuPrincipal.class).headless(false).run(args);
        SwingUtilities.invokeLater(() -> {
            MenuPrincipal frame = context.getBean(MenuPrincipal.class);
            frame.setVisible(true);
        });
    }
}