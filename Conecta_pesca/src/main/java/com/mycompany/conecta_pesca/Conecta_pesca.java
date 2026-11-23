/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.mycompany.conecta_pesca;

/**
 *
 * @author gabrielos
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

// ==========================================
// CONFIGURAÇÃO CENTRAL DE BANCO DE DADOS
// ==========================================
class ConexaoMySQL {
    private static final String URL = "jdbc:mysql://localhost:3306/PeixariaA3?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String SENHA = "03102005";

    static final String ADMIN_USUARIO = "admin";
    static final String ADMIN_SENHA = "03102005";

    public static Connection abrir() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado. Coloque o connector no classpath.", e);
        }
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    public static String hashSenha(String senha) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(senha.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

    public static boolean senhaConfere(String digitada, String armazenada) {
        if (digitada == null || armazenada == null) return false;
        return armazenada.equals(hashSenha(digitada)) || armazenada.equals(digitada);
    }

    public static void promoverHashSePreciso(Connection con, String sql, int id, String digitada, String armazenada) {
        if (armazenada != null && armazenada.equals(hashSenha(digitada))) return;
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, hashSenha(digitada));
            pst.setInt(2, id);
            pst.executeUpdate();
        } catch (SQLException ignored) {
        }
    }
}

// ==========================================
// PREPARA A TABELA DE PEDIDOS E ESPÉCIES INICIAIS
// ==========================================
class BancoInicial {
    static void preparar() {
        try (Connection con = ConexaoMySQL.abrir()) {
            try (Statement st = con.createStatement()) {
                st.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS pedidos ("
                                + "id_pedido INT AUTO_INCREMENT PRIMARY KEY, "
                                + "id_cliente INT NOT NULL, "
                                + "id_estoque INT NOT NULL, "
                                + "quantidade_kg DECIMAL(10,2) NOT NULL, "
                                + "preco_por_kg DECIMAL(10,2) NOT NULL, "
                                + "total DECIMAL(10,2) NOT NULL, "
                                + "data_pedido TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
                );
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM peixes")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    inserirEspecies(con);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível preparar o banco: " + e.getMessage(),
                    "Banco de dados", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void inserirEspecies(Connection con) throws SQLException {
        String[][] especies = {
                {"Tilápia", "Oreochromis niloticus"},
                {"Tambaqui", "Colossoma macropomum"},
                {"Pacu", "Piaractus mesopotamicus"},
                {"Pintado", "Pseudoplatystoma corruscans"},
                {"Tucunaré", "Cichla ocellaris"},
                {"Robalo", "Centropomus undecimalis"},
                {"Corvina", "Micropogonias furnieri"},
                {"Dourado", "Salminus brasiliensis"},
                {"Pescada", "Cynoscion spp."},
                {"Sardinha", "Sardinella brasiliensis"}
        };
        try (PreparedStatement pst = con.prepareStatement(
                "INSERT INTO peixes (nome_popular, nome_cientifico) VALUES (?, ?)")) {
            for (String[] especie : especies) {
                pst.setString(1, especie[0]);
                pst.setString(2, especie[1]);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }
}

// ==========================================
// TELA DE LOGIN / ESCOLHA / CADASTRO DE CLIENTE
// ==========================================
class TelaLogin extends JFrame {
    private final Color AZUL = new Color(12, 52, 76);
    private final Color BRANCO = Color.WHITE;
    private final Color FUNDO = new Color(240, 245, 248);

    public TelaLogin() {
        setTitle("CONECTA_PESCA | Login e Acesso");
        setSize(520, 460);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.setBackground(FUNDO);

        JPanel painelTitulo = new JPanel();
        painelTitulo.setBackground(AZUL);
        painelTitulo.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        JLabel lblTitulo = new JLabel("CONECTA_PESCA", SwingConstants.CENTER);
        lblTitulo.setForeground(BRANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));
        painelTitulo.add(lblTitulo);
        painelPrincipal.add(painelTitulo, BorderLayout.NORTH);

        JTabbedPane abas = new JTabbedPane();
        abas.setFont(new Font("Arial", Font.BOLD, 13));

        JPanel painelEntrar = new JPanel(new GridBagLayout());
        painelEntrar.setBackground(BRANCO);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtEmail = new JTextField(15);
        JPasswordField txtSenha = new JPasswordField(15);
        JButton btnEntrar = new JButton("Entrar no Sistema");
        estilizarBotaoPadrao(btnEntrar);

        gbc.gridx = 0; gbc.gridy = 0; painelEntrar.add(new JLabel("E-mail / Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; painelEntrar.add(txtEmail, gbc);
        gbc.gridx = 0; gbc.gridy = 1; painelEntrar.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; painelEntrar.add(txtSenha, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; painelEntrar.add(btnEntrar, gbc);

        btnEntrar.addActionListener(e -> entrar(txtEmail.getText().trim(), new String(txtSenha.getPassword())));

        JPanel painelCadastroCliente = new JPanel(new GridBagLayout());
        painelCadastroCliente.setBackground(BRANCO);
        GridBagConstraints gbcC = new GridBagConstraints();
        gbcC.insets = new Insets(8, 8, 8, 8);
        gbcC.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtNomeC = new JTextField(15);
        JTextField txtEmailC = new JTextField(15);
        JPasswordField txtSenhaC = new JPasswordField(15);
        JButton btnCadastrarC = new JButton("Cadastrar Conta de Cliente");
        estilizarBotaoPadrao(btnCadastrarC);

        gbcC.gridx = 0; gbcC.gridy = 0; painelCadastroCliente.add(new JLabel("Nome:"), gbcC);
        gbcC.gridx = 1; gbcC.gridy = 0; painelCadastroCliente.add(txtNomeC, gbcC);
        gbcC.gridx = 0; gbcC.gridy = 1; painelCadastroCliente.add(new JLabel("E-mail:"), gbcC);
        gbcC.gridx = 1; gbcC.gridy = 1; painelCadastroCliente.add(txtEmailC, gbcC);
        gbcC.gridx = 0; gbcC.gridy = 2; painelCadastroCliente.add(new JLabel("Senha:"), gbcC);
        gbcC.gridx = 1; gbcC.gridy = 2; painelCadastroCliente.add(txtSenhaC, gbcC);
        gbcC.gridx = 0; gbcC.gridy = 3; gbcC.gridwidth = 2; painelCadastroCliente.add(btnCadastrarC, gbcC);

        btnCadastrarC.addActionListener(e -> cadastrarCliente(txtNomeC, txtEmailC, txtSenhaC));

        JPanel painelConsultas = new JPanel(new GridLayout(2, 1, 15, 15));
        painelConsultas.setBackground(BRANCO);
        painelConsultas.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JButton btnMunicipios = new JButton("Consultar Municípios");
        JButton btnEstoques = new JButton("Consultar Estoque");
        estilizarBotaoPadrao(btnMunicipios);
        estilizarBotaoPadrao(btnEstoques);

        btnMunicipios.addActionListener(ev -> new TelaConsultaRapida("Municípios",
                "SELECT id_municipio, nome, uf FROM municipios").setVisible(true));
        btnEstoques.addActionListener(ev -> new TelaConsultaRapida("Estoques",
                "SELECT e.id_estoque, p.nome AS pescador, p.celular AS telefone, m.nome AS cidade, m.uf AS estado, "
                        + "pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao, e.disponibilidade "
                        + "FROM estoques e "
                        + "INNER JOIN pescadores p ON e.id_pescador = p.id_pescador "
                        + "INNER JOIN municipios m ON p.id_municipio = m.id_municipio "
                        + "INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe").setVisible(true));

        painelConsultas.add(btnMunicipios);
        painelConsultas.add(btnEstoques);

        abas.add("Entrar", painelEntrar);
        abas.add("Cadastrar Cliente", painelCadastroCliente);
        abas.add("Consultas", painelConsultas);

        painelPrincipal.add(abas, BorderLayout.CENTER);
        add(painelPrincipal);
    }

    private void entrar(String loginInput, String senha) {
        if (loginInput.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (loginInput.equals(ConexaoMySQL.ADMIN_USUARIO) && senha.equals(ConexaoMySQL.ADMIN_SENHA)) {
            JOptionPane.showMessageDialog(this, "Bem-vindo, Administrador!");
            dispose();
            SwingUtilities.invokeLater(() -> new Conecta_pesca("ADMIN", 0).setVisible(true));
            return;
        }

        SQLException falhaBanco = null;
        Integer idPescador = null;
        String nomePescador = null;

        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(
                     "SELECT id_pescador, nome, senha_hash FROM pescadores WHERE email = ?")) {
            pst.setString(1, loginInput);
            String armazenada = null;
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    armazenada = rs.getString("senha_hash");
                    if (ConexaoMySQL.senhaConfere(senha, armazenada)) {
                        idPescador = rs.getInt("id_pescador");
                        nomePescador = rs.getString("nome");
                    }
                }
            }
            if (idPescador != null) {
                ConexaoMySQL.promoverHashSePreciso(con,
                        "UPDATE pescadores SET senha_hash = ? WHERE id_pescador = ?",
                        idPescador, senha, armazenada);
            }
        } catch (SQLException ex) {
            falhaBanco = ex;
            idPescador = null;
        }

        if (idPescador != null) {
            String nome = nomePescador;
            int id = idPescador;
            JOptionPane.showMessageDialog(this, "Bem-vindo, Pescador " + nome + "!");
            dispose();
            SwingUtilities.invokeLater(() -> new Conecta_pesca("PESCADOR", id).setVisible(true));
            return;
        }

        Integer idCliente = null;
        String nomeCliente = null;

        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(
                     "SELECT id_cliente, nome, senha FROM clientes WHERE email = ?")) {
            pst.setString(1, loginInput);
            String armazenada = null;
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    armazenada = rs.getString("senha");
                    if (ConexaoMySQL.senhaConfere(senha, armazenada)) {
                        idCliente = rs.getInt("id_cliente");
                        nomeCliente = rs.getString("nome");
                    }
                }
            }
            if (idCliente != null) {
                ConexaoMySQL.promoverHashSePreciso(con,
                        "UPDATE clientes SET senha = ? WHERE id_cliente = ?",
                        idCliente, senha, armazenada);
            }
        } catch (SQLException ex) {
            if (falhaBanco == null) falhaBanco = ex;
            idCliente = null;
        }

        if (idCliente != null) {
            String nome = nomeCliente;
            int id = idCliente;
            JOptionPane.showMessageDialog(this, "Bem-vindo para suas compras, " + nome + "!");
            dispose();
            SwingUtilities.invokeLater(() -> new Conecta_pesca("CLIENTE", id).setVisible(true));
            return;
        }

        if (falhaBanco != null) {
            JOptionPane.showMessageDialog(this, "Erro de banco: " + falhaBanco.getMessage(),
                    "Erro de Login", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "E-mail ou senha incorretos!",
                    "Erro de Login", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cadastrarCliente(JTextField txtNome, JTextField txtEmail, JPasswordField txtSenha) {
        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha todos os campos para cadastro!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(
                     "INSERT INTO clientes (nome, email, senha) VALUES (?, ?, ?)")) {
            pst.setString(1, nome);
            pst.setString(2, email);
            pst.setString(3, ConexaoMySQL.hashSenha(senha));
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Cadastro realizado com sucesso! Vá na aba 'Entrar' para acessar.");
            txtNome.setText("");
            txtEmail.setText("");
            txtSenha.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void estilizarBotaoPadrao(JButton botao) {
        botao.setBackground(new Color(225, 235, 242));
        botao.setForeground(AZUL);
        botao.setFont(new Font("Arial", Font.BOLD, 13));
        botao.setFocusPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}

// ==========================================
// TELA AUXILIAR DE CONSULTA RÁPIDA
// ==========================================
class TelaConsultaRapida extends JFrame {
    public TelaConsultaRapida(String titulo, String sql) {
        setTitle("Peixaria A3 | Consulta de " + titulo);
        setSize(750, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTable tabela = new JTable();
        tabela.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(tabela);

        DefaultTableModel modelo = new DefaultTableModel();
        try (Connection con = ConexaoMySQL.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) modelo.addColumn(meta.getColumnLabel(i));

            while (rs.next()) {
                Object[] linha = new Object[cols];
                for (int i = 1; i <= cols; i++) linha[i - 1] = rs.getObject(i);
                modelo.addRow(linha);
            }
            tabela.setModel(modelo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao buscar dados: " + e.getMessage());
        }

        add(scroll);
    }
}

// ==========================================
// SISTEMA PRINCIPAL (PEIXARIA A3)
// ==========================================
public class Conecta_pesca extends JFrame {
    private static final String NOVA_ESPECIE = "＋ Cadastrar nova espécie...";
    private static final String NOVO_MUNICIPIO = "＋ Cadastrar novo município...";

    private final Color AZUL = new Color(12, 52, 76);
    private final Color AZUL_CLARO = new Color(20, 115, 145);
    private final Color FUNDO = new Color(240, 245, 248);
    private final Color BRANCO = Color.WHITE;
    private final Color VERDE = new Color(34, 139, 34);
    private final Color VERMELHO = new Color(176, 62, 62);

    private JPanel conteudo;
    private CardLayout layout;
    private JPanel painelDashboard;
    private final Map<String, JTable> tabelas = new HashMap<>();
    private final Map<String, String> consultas = new HashMap<>();

    private final String perfilUsuario;
    private final int idLogado;

    public Conecta_pesca(String perfil, int idLogado) {
        this.perfilUsuario = perfil;
        this.idLogado = idLogado;

        setTitle("Peixaria A3 | Sistema (" + perfil + ")");
        setSize(1250, 720);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        criarMenu();
        criarConteudo();
    }

    private boolean ehAdmin() {
        return "ADMIN".equals(perfilUsuario);
    }

    private boolean ehPescador() {
        return "PESCADOR".equals(perfilUsuario);
    }

    private void criarMenu() {
        JPanel menu = new JPanel(new GridLayout(10, 1, 5, 5));
        menu.setBackground(AZUL);
        menu.setPreferredSize(new Dimension(235, 700));

        JLabel titulo = new JLabel("  PEIXARIA A3");
        titulo.setForeground(BRANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 23));
        menu.add(titulo);

        adicionarBotao(menu, "Dashboard", "dashboard");

        if (ehAdmin()) {
            adicionarBotao(menu, "Pescadores", "pescadores");
            adicionarBotao(menu, "Municípios", "municipios");
            adicionarBotao(menu, "Peixes", "peixes");
            adicionarBotao(menu, "Estoques Gerais", "estoques");
        } else if (ehPescador()) {
            adicionarBotao(menu, "Meu Estoque", "estoques");
        } else {
            adicionarBotao(menu, "Comprar Peixes", "estoques");
        }

        JButton sair = new JButton("Sair / Trocar Conta");
        estilizarBotao(sair);
        sair.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Deseja realmente sair do sistema?",
                    "Confirmar saída", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                dispose();
                new TelaLogin().setVisible(true);
            }
        });

        menu.add(sair);
        add(menu, BorderLayout.WEST);
    }

    private void estilizarBotao(JButton botao) {
        botao.setBackground(AZUL_CLARO);
        botao.setForeground(BRANCO);
        botao.setFont(new Font("Arial", Font.BOLD, 14));
        botao.setFocusPainted(false);
        botao.setBorderPainted(false);
        botao.setOpaque(true);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void pintarBotao(JButton botao, Color fundo, Color texto) {
        botao.setFont(new Font("Arial", Font.BOLD, 13));
        botao.setBackground(fundo);
        botao.setForeground(texto);
        botao.setFocusPainted(false);
        botao.setBorderPainted(false);
        botao.setOpaque(true);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void adicionarBotao(JPanel menu, String nome, String pagina) {
        JButton botao = new JButton(nome);
        estilizarBotao(botao);
        botao.addActionListener(e -> {
            if ("dashboard".equals(pagina)) {
                recarregarDashboard();
                return;
            }
            layout.show(conteudo, pagina);
            if (tabelas.containsKey(pagina)) atualizarTabela(pagina);
        });
        menu.add(botao);
    }

    private void criarConteudo() {
        layout = new CardLayout();
        conteudo = new JPanel(layout);
        conteudo.setBackground(FUNDO);

        if (ehAdmin()) {
            JButton btnNovoPescador = new JButton("Cadastrar pescador");
            estilizarBotao(btnNovoPescador);
            btnNovoPescador.addActionListener(e -> abrirDialogoCadastroPescador());

            conteudo.add(criarPagina("Pescadores", "pescadores",
                    "SELECT id_pescador, nome, email, celular, ativo FROM pescadores",
                    btnNovoPescador), "pescadores");
            conteudo.add(criarPagina("Municípios", "municipios",
                    "SELECT id_municipio, nome, uf FROM municipios"), "municipios");
            conteudo.add(criarPagina("Peixes", "peixes",
                    "SELECT id_peixe, nome_popular, nome_cientifico FROM peixes"), "peixes");
            conteudo.add(criarPaginaEstoque("Estoques Gerais", "estoques",
                    "SELECT e.id_estoque, p.nome AS pescador, p.celular AS telefone, m.nome AS cidade, m.uf AS estado, "
                            + "pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao, e.disponibilidade "
                            + "FROM estoques e "
                            + "INNER JOIN pescadores p ON e.id_pescador = p.id_pescador "
                            + "INNER JOIN municipios m ON p.id_municipio = m.id_municipio "
                            + "INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe"), "estoques");
        } else if (ehPescador()) {
            String sqlPescador = "SELECT e.id_estoque, p.celular AS telefone, m.nome AS cidade, m.uf AS estado, "
                    + "pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao, e.disponibilidade "
                    + "FROM estoques e "
                    + "INNER JOIN pescadores p ON e.id_pescador = p.id_pescador "
                    + "INNER JOIN municipios m ON p.id_municipio = m.id_municipio "
                    + "INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe "
                    + "WHERE e.id_pescador = " + idLogado;
            conteudo.add(criarPaginaEstoque("Meu Estoque", "estoques", sqlPescador), "estoques");
        } else {
            String sqlCliente = "SELECT e.id_estoque, p.nome AS pescador, p.celular AS telefone, m.nome AS cidade, m.uf AS estado, "
                    + "pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao "
                    + "FROM estoques e "
                    + "INNER JOIN pescadores p ON e.id_pescador = p.id_pescador "
                    + "INNER JOIN municipios m ON p.id_municipio = m.id_municipio "
                    + "INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe "
                    + "WHERE e.disponibilidade = TRUE";
            conteudo.add(criarPaginaEstoque("Catálogo de Peixes para Compra", "estoques", sqlCliente), "estoques");
        }

        add(conteudo, BorderLayout.CENTER);
        recarregarDashboard();
    }

    private void recarregarDashboard() {
        if (painelDashboard != null) conteudo.remove(painelDashboard);
        painelDashboard = criarDashboard();
        conteudo.add(painelDashboard, "dashboard");
        layout.show(conteudo, "dashboard");
        conteudo.revalidate();
        conteudo.repaint();
    }

    private JPanel criarDashboard() {
        JPanel painel = new JPanel(new BorderLayout(20, 20));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel("Dashboard (" + perfilUsuario + ")");
        titulo.setFont(new Font("Arial", Font.BOLD, 30));
        titulo.setForeground(AZUL);
        JLabel subtitulo = new JLabel("Visão geral do sistema de gerenciamento");
        subtitulo.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitulo.setForeground(Color.GRAY);

        JPanel cabecalho = new JPanel(new GridLayout(2, 1, 5, 5));
        cabecalho.setBackground(FUNDO);
        cabecalho.add(titulo);
        cabecalho.add(subtitulo);

        JPanel cards = new JPanel(new GridLayout(1, 3, 20, 20));
        cards.setBackground(FUNDO);

        if (ehAdmin()) {
            cards.add(criarCard("Pescadores cadastrados", consultarInt("SELECT COUNT(*) FROM pescadores")));
            cards.add(criarCard("Tipos de peixes", consultarInt("SELECT COUNT(*) FROM peixes")));
            cards.add(criarCard("Registros de estoque", consultarInt("SELECT COUNT(*) FROM estoques")));
        } else if (ehPescador()) {
            cards.add(criarCard("Quilos à venda", formatarKg(consultarDecimal(
                    "SELECT COALESCE(SUM(quantidade_kg), 0) FROM estoques WHERE id_pescador = ? AND disponibilidade = TRUE",
                    idLogado))));
            cards.add(criarCard("Itens disponíveis", consultarInt(
                    "SELECT COUNT(*) FROM estoques WHERE id_pescador = ? AND disponibilidade = TRUE", idLogado)));
            cards.add(criarCard("Itens no estoque", consultarInt(
                    "SELECT COUNT(*) FROM estoques WHERE id_pescador = ?", idLogado)));
        } else {
            cards.add(criarCard("Ofertas disponíveis", consultarInt(
                    "SELECT COUNT(*) FROM estoques WHERE disponibilidade = TRUE")));
            cards.add(criarCard("Pescadores com peixe", consultarInt(
                    "SELECT COUNT(DISTINCT id_pescador) FROM estoques WHERE disponibilidade = TRUE")));
            cards.add(criarCard("Espécies à venda", consultarInt(
                    "SELECT COUNT(DISTINCT id_peixe) FROM estoques WHERE disponibilidade = TRUE")));
        }

        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(cards, BorderLayout.CENTER);
        return painel;
    }

    private int consultarInt(String sql, Object... params) {
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(sql)) {
            definirParametros(pst, params);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro de banco: " + e.getMessage());
        }
        return 0;
    }

    private BigDecimal consultarDecimal(String sql, Object... params) {
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(sql)) {
            definirParametros(pst, params);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    BigDecimal valor = rs.getBigDecimal(1);
                    return valor == null ? BigDecimal.ZERO : valor;
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro de banco: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private void definirParametros(PreparedStatement pst, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) pst.setObject(i + 1, params[i]);
    }

    private JPanel criarCard(String nome, int valor) {
        return criarCard(nome, String.valueOf(valor));
    }

    private JPanel criarCard(String nome, String valor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 5, 5));
        card.setBackground(BRANCO);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 225, 230)),
                BorderFactory.createEmptyBorder(20, 10, 20, 10)));

        JLabel texto = new JLabel(nome, SwingConstants.CENTER);
        texto.setFont(new Font("Arial", Font.BOLD, 16));
        texto.setForeground(AZUL);
        JLabel numero = new JLabel(valor, SwingConstants.CENTER);
        numero.setFont(new Font("Arial", Font.BOLD, 30));
        numero.setForeground(AZUL_CLARO);

        card.add(texto);
        card.add(numero);
        return card;
    }

    private void configurarTabela(JTable tabela) {
        tabela.setRowHeight(32);
        tabela.setFont(new Font("Arial", Font.PLAIN, 14));
        tabela.setSelectionBackground(new Color(210, 230, 240));
        tabela.setSelectionForeground(AZUL);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tabela.getTableHeader().setBackground(AZUL);
        tabela.getTableHeader().setForeground(BRANCO);
    }

    private JPanel criarPagina(String titulo, String identificador, String sql, JButton... acoesExtras) {
        JPanel painel = new JPanel(new BorderLayout(15, 15));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel nome = new JLabel(titulo);
        nome.setFont(new Font("Arial", Font.BOLD, 28));
        nome.setForeground(AZUL);

        JTable tabela = new JTable();
        configurarTabela(tabela);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.getViewport().setBackground(BRANCO);

        JButton atualizar = new JButton("Atualizar dados");
        estilizarBotao(atualizar);
        atualizar.addActionListener(e -> atualizarTabela(identificador));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.setBackground(FUNDO);
        botoes.add(atualizar);
        for (JButton acao : acoesExtras) botoes.add(acao);

        painel.add(nome, BorderLayout.NORTH);
        painel.add(scroll, BorderLayout.CENTER);
        painel.add(botoes, BorderLayout.SOUTH);

        tabelas.put(identificador, tabela);
        consultas.put(identificador, sql);
        SwingUtilities.invokeLater(() -> atualizarTabela(identificador));
        return painel;
    }

    private JPanel criarPaginaEstoque(String titulo, String identificador, String sql) {
        JPanel painel = new JPanel(new BorderLayout(15, 15));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel nome = new JLabel(titulo);
        nome.setFont(new Font("Arial", Font.BOLD, 28));
        nome.setForeground(AZUL);

        JTable tabela = new JTable();
        configurarTabela(tabela);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.getViewport().setBackground(BRANCO);

        JPanel painelFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        painelFiltro.setBackground(FUNDO);

        JLabel lblFiltroPeixe = new JLabel("Peixe:");
        lblFiltroPeixe.setFont(new Font("Arial", Font.BOLD, 13));
        lblFiltroPeixe.setForeground(AZUL);
        JTextField txtFiltroPeixe = new JTextField(10);

        JLabel lblFiltroCidade = new JLabel("Cidade:");
        lblFiltroCidade.setFont(new Font("Arial", Font.BOLD, 13));
        lblFiltroCidade.setForeground(AZUL);
        JTextField txtFiltroCidade = new JTextField(10);

        JButton btnConfirmarFiltro = new JButton("Confirmar");
        pintarBotao(btnConfirmarFiltro, VERDE, Color.WHITE);

        painelFiltro.add(lblFiltroPeixe);
        painelFiltro.add(txtFiltroPeixe);
        painelFiltro.add(lblFiltroCidade);
        painelFiltro.add(txtFiltroCidade);
        painelFiltro.add(btnConfirmarFiltro);

        JButton btnLimpar = new JButton("Limpar Filtro");
        pintarBotao(btnLimpar, VERDE, Color.WHITE);
        btnLimpar.addActionListener(e -> {
            txtFiltroPeixe.setText("");
            txtFiltroCidade.setText("");
            aplicarFiltroEstoque(tabela, "", "");
        });

        JButton btnAtualizar = new JButton("Atualizar dados");
        estilizarBotao(btnAtualizar);
        btnAtualizar.addActionListener(e -> atualizarTabela(identificador));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        botoes.setBackground(FUNDO);
        botoes.add(btnAtualizar);
        botoes.add(btnLimpar);

        if (ehAdmin() || ehPescador()) {
            JButton btnNovoPeixe = new JButton("+ Adicionar Peixe à Venda");
            pintarBotao(btnNovoPeixe, AZUL, Color.WHITE);
            btnNovoPeixe.addActionListener(e -> abrirDialogoCadastroEstoque(identificador));

            JButton btnEditar = new JButton("Editar selecionado");
            pintarBotao(btnEditar, AZUL_CLARO, Color.WHITE);
            btnEditar.addActionListener(e -> abrirDialogoEditarEstoque(identificador));

            JButton btnDesativar = new JButton("Tirar de venda");
            pintarBotao(btnDesativar, VERMELHO, Color.WHITE);
            btnDesativar.addActionListener(e -> desativarEstoque(identificador));

            botoes.add(btnNovoPeixe);
            botoes.add(btnEditar);
            botoes.add(btnDesativar);
        } else {
            JButton btnComprar = new JButton("Comprar selecionado");
            pintarBotao(btnComprar, VERDE, Color.WHITE);
            btnComprar.addActionListener(e -> abrirDialogoCompra(identificador));
            botoes.add(btnComprar);
        }

        JPanel topo = new JPanel(new BorderLayout(10, 10));
        topo.setBackground(FUNDO);
        topo.add(nome, BorderLayout.NORTH);
        topo.add(painelFiltro, BorderLayout.SOUTH);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(scroll, BorderLayout.CENTER);
        painel.add(botoes, BorderLayout.SOUTH);

        tabelas.put(identificador, tabela);
        consultas.put(identificador, sql);

        btnConfirmarFiltro.addActionListener(e -> aplicarFiltroEstoque(
                tabela, txtFiltroPeixe.getText().trim(), txtFiltroCidade.getText().trim()));

        SwingUtilities.invokeLater(() -> atualizarTabela(identificador));
        return painel;
    }

    private void aplicarFiltroEstoque(JTable tabela, String peixeText, String cidadeText) {
        DefaultTableModel modelo = (DefaultTableModel) tabela.getModel();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        tabela.setRowSorter(sorter);

        int colPeixe = indiceColuna(modelo, "peixe");
        int colCidade = indiceColuna(modelo, "cidade");
        List<RowFilter<DefaultTableModel, Integer>> filtros = new ArrayList<>();
        if (!peixeText.isEmpty() && colPeixe >= 0) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(peixeText), colPeixe));
        }
        if (!cidadeText.isEmpty() && colCidade >= 0) {
            filtros.add(RowFilter.regexFilter("(?i)" + Pattern.quote(cidadeText), colCidade));
        }
        if (filtros.isEmpty()) sorter.setRowFilter(null);
        else sorter.setRowFilter(RowFilter.andFilter(filtros));
    }

    private int indiceColuna(DefaultTableModel modelo, String nome) {
        for (int i = 0; i < modelo.getColumnCount(); i++) {
            if (nome.equalsIgnoreCase(modelo.getColumnName(i))) return i;
        }
        return -1;
    }

    private void abrirDialogoCadastroPescador() {
        JComboBox<String> cbMunicipios = new JComboBox<>();
        Map<String, Integer> mapa = new HashMap<>();

        try (Connection con = ConexaoMySQL.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_municipio, nome, uf FROM municipios ORDER BY nome")) {
            while (rs.next()) {
                String rotulo = rs.getString("nome") + " - " + rs.getString("uf");
                if (mapa.containsKey(rotulo)) rotulo = rotulo + " (#" + rs.getInt("id_municipio") + ")";
                cbMunicipios.addItem(rotulo);
                mapa.put(rotulo, rs.getInt("id_municipio"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar municípios: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        cbMunicipios.addItem(NOVO_MUNICIPIO);

        JTextField txtNome = new JTextField(16);
        JTextField txtEmail = new JTextField(16);
        JPasswordField txtSenha = new JPasswordField(16);
        JTextField txtCelular = new JTextField(16);

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Nome:"));
        form.add(txtNome);
        form.add(new JLabel("E-mail:"));
        form.add(txtEmail);
        form.add(new JLabel("Senha:"));
        form.add(txtSenha);
        form.add(new JLabel("Celular:"));
        form.add(txtCelular);
        form.add(new JLabel("Município:"));
        form.add(cbMunicipios);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Cadastrar pescador",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return;

        String nome = txtNome.getText().trim();
        String email = txtEmail.getText().trim();
        String senha = new String(txtSenha.getPassword());
        String celular = txtCelular.getText().trim();
        if (nome.isEmpty() || email.isEmpty() || senha.isEmpty() || celular.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha nome, e-mail, senha e celular.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Informe um e-mail válido.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String escolhido = (String) cbMunicipios.getSelectedItem();
            int idMunicipio;
            if (NOVO_MUNICIPIO.equals(escolhido)) {
                idMunicipio = cadastrarMunicipio();
            } else {
                Integer id = mapa.get(escolhido);
                if (id == null) {
                    JOptionPane.showMessageDialog(this, "Selecione um município.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                idMunicipio = id;
            }
            if (idMunicipio <= 0) return;

            try (Connection con = ConexaoMySQL.abrir();
                 PreparedStatement pst = con.prepareStatement(
                         "INSERT INTO pescadores (nome, email, senha_hash, celular, id_municipio, ativo) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                pst.setString(1, nome);
                pst.setString(2, email);
                pst.setString(3, ConexaoMySQL.hashSenha(senha));
                pst.setString(4, celular);
                pst.setInt(5, idMunicipio);
                pst.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Pescador cadastrado. Ele já pode entrar com o e-mail e a senha.");
            atualizarTabela("pescadores");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao cadastrar pescador: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int cadastrarMunicipio() throws SQLException {
        JTextField txtNome = new JTextField(14);
        JTextField txtUf = new JTextField(4);
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Nome:"));
        form.add(txtNome);
        form.add(new JLabel("UF:"));
        form.add(txtUf);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Novo município",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return -1;

        String nome = txtNome.getText().trim();
        String uf = txtUf.getText().trim().toUpperCase(Locale.ROOT);
        if (nome.isEmpty() || !uf.matches("[A-Z]{2}")) {
            JOptionPane.showMessageDialog(this, "Informe o nome e a UF com 2 letras.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return -1;
        }

        try (Connection con = ConexaoMySQL.abrir()) {
            try (PreparedStatement busca = con.prepareStatement(
                    "SELECT id_municipio FROM municipios WHERE LOWER(nome) = LOWER(?) AND UPPER(uf) = ?")) {
                busca.setString(1, nome);
                busca.setString(2, uf);
                try (ResultSet rs = busca.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO municipios (nome, uf) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                pst.setString(1, nome);
                pst.setString(2, uf);
                pst.executeUpdate();
                try (ResultSet keys = pst.getGeneratedKeys()) {
                    if (keys.next()) {
                        atualizarTabela("municipios");
                        return keys.getInt(1);
                    }
                }
            }
        }
        throw new SQLException("Não foi possível obter o id do município.");
    }

    private void abrirDialogoCadastroEstoque(String identificadorTabela) {
        JComboBox<String> cbPeixes = new JComboBox<>();
        Map<String, Integer> mapaPeixes = new HashMap<>();
        JComboBox<String> cbPescadores = new JComboBox<>();
        Map<String, Integer> mapaPescadores = new HashMap<>();

        try (Connection con = ConexaoMySQL.abrir()) {
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_peixe, nome_popular FROM peixes ORDER BY nome_popular")) {
                while (rs.next()) {
                    String rotulo = rs.getString("nome_popular");
                    if (mapaPeixes.containsKey(rotulo)) rotulo = rotulo + " (#" + rs.getInt("id_peixe") + ")";
                    cbPeixes.addItem(rotulo);
                    mapaPeixes.put(rotulo, rs.getInt("id_peixe"));
                }
            }
            cbPeixes.addItem(NOVA_ESPECIE);

            if (ehAdmin()) {
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id_pescador, nome FROM pescadores ORDER BY nome")) {
                    while (rs.next()) {
                        String rotulo = rs.getString("nome") + " (#" + rs.getInt("id_pescador") + ")";
                        cbPescadores.addItem(rotulo);
                        mapaPescadores.put(rotulo, rs.getInt("id_pescador"));
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar dados: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (ehAdmin() && mapaPescadores.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cadastre um pescador antes de lançar estoque.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField txtQuantidade = new JTextField(10);
        JTextField txtPreco = new JTextField(10);
        JComboBox<String> cbConservacao = new JComboBox<>(new String[]{"FRESCO", "CONGELADO"});

        JPanel form = new JPanel(new GridLayout(ehAdmin() ? 5 : 4, 2, 10, 10));
        if (ehAdmin()) {
            form.add(new JLabel("Pescador:"));
            form.add(cbPescadores);
        }
        form.add(new JLabel("Espécie do Peixe:"));
        form.add(cbPeixes);
        form.add(new JLabel("Quantidade (kg):"));
        form.add(txtQuantidade);
        form.add(new JLabel("Preço por kg (R$):"));
        form.add(txtPreco);
        form.add(new JLabel("Conservação:"));
        form.add(cbConservacao);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Cadastrar Peixe para Venda",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return;

        BigDecimal quantidade = lerDecimal(txtQuantidade.getText());
        BigDecimal preco = lerDecimal(txtPreco.getText());
        if (quantidade == null || preco == null) {
            JOptionPane.showMessageDialog(this, "Digite valores numéricos válidos para quantidade e preço. Use 10,5 ou 10.5.",
                    "Erro de Formato", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0 || preco.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Quantidade e preço devem ser maiores que zero.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idPescador;
        if (ehAdmin()) {
            Integer id = mapaPescadores.get((String) cbPescadores.getSelectedItem());
            if (id == null) {
                JOptionPane.showMessageDialog(this, "Selecione o pescador.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            idPescador = id;
        } else {
            idPescador = idLogado;
        }

        String mensagem;
        try {
            String peixeSelecionado = (String) cbPeixes.getSelectedItem();
            int idPeixe;
            if (NOVA_ESPECIE.equals(peixeSelecionado)) {
                idPeixe = cadastrarNovaEspecie();
            } else {
                Integer id = mapaPeixes.get(peixeSelecionado);
                if (id == null) {
                    JOptionPane.showMessageDialog(this, "Selecione a espécie.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                idPeixe = id;
            }
            if (idPeixe <= 0) return;

            try (Connection con = ConexaoMySQL.abrir()) {
                mensagem = salvarEstoque(con, idPescador, idPeixe, quantidade, preco,
                        (String) cbConservacao.getSelectedItem());
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar no banco de dados: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, mensagem);
        atualizarTabela(identificadorTabela);
        if (ehAdmin()) atualizarTabela("peixes");
    }

    private int cadastrarNovaEspecie() throws SQLException {
        JTextField txtPopular = new JTextField(16);
        JTextField txtCientifico = new JTextField(16);
        JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
        form.add(new JLabel("Nome popular:"));
        form.add(txtPopular);
        form.add(new JLabel("Nome científico:"));
        form.add(txtCientifico);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Nova espécie",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return -1;

        String popular = txtPopular.getText().trim();
        String cientifico = txtCientifico.getText().trim();
        if (popular.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome popular da espécie.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        if (cientifico.isEmpty()) cientifico = popular;

        try (Connection con = ConexaoMySQL.abrir()) {
            try (PreparedStatement busca = con.prepareStatement(
                    "SELECT id_peixe FROM peixes WHERE LOWER(nome_popular) = LOWER(?)")) {
                busca.setString(1, popular);
                try (ResultSet rs = busca.executeQuery()) {
                    if (rs.next()) return rs.getInt("id_peixe");
                }
            }
            try (PreparedStatement pst = con.prepareStatement(
                    "INSERT INTO peixes (nome_popular, nome_cientifico) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                pst.setString(1, popular);
                pst.setString(2, cientifico);
                pst.executeUpdate();
                try (ResultSet keys = pst.getGeneratedKeys()) {
                    if (keys.next()) return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Não foi possível obter o id da nova espécie.");
    }

    private String salvarEstoque(Connection con, int idPescador, int idPeixe, BigDecimal quantidade,
                                 BigDecimal preco, String conservacao) throws SQLException {
        Integer idExistente = null;
        try (PreparedStatement pst = con.prepareStatement(
                "SELECT id_estoque FROM estoques WHERE id_pescador = ? AND id_peixe = ? AND estado_conservacao = ? ORDER BY id_estoque LIMIT 1")) {
            pst.setInt(1, idPescador);
            pst.setInt(2, idPeixe);
            pst.setString(3, conservacao);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) idExistente = rs.getInt("id_estoque");
            }
        }

        if (idExistente != null) {
            try (PreparedStatement pst = con.prepareStatement(
                    "UPDATE estoques SET quantidade_kg = quantidade_kg + ?, preco_por_kg = ?, disponibilidade = TRUE WHERE id_estoque = ?")) {
                pst.setBigDecimal(1, quantidade);
                pst.setBigDecimal(2, preco);
                pst.setInt(3, idExistente);
                pst.executeUpdate();
            }
            return "Esse peixe já estava no estoque deste pescador. Os quilos foram somados e o preço foi atualizado.";
        }

        try (PreparedStatement pst = con.prepareStatement(
                "INSERT INTO estoques (id_pescador, id_peixe, quantidade_kg, preco_por_kg, estado_conservacao, disponibilidade) VALUES (?, ?, ?, ?, ?, TRUE)")) {
            pst.setInt(1, idPescador);
            pst.setInt(2, idPeixe);
            pst.setBigDecimal(3, quantidade);
            pst.setBigDecimal(4, preco);
            pst.setString(5, conservacao);
            pst.executeUpdate();
        }
        return "Peixe adicionado ao estoque com sucesso!";
    }

    private void abrirDialogoEditarEstoque(String identificador) {
        int idEstoque = idLinhaSelecionada(identificador);
        if (idEstoque < 0) return;

        BigDecimal quantidade;
        BigDecimal preco;
        String conservacao;
        boolean disponivel;
        String sql = "SELECT quantidade_kg, preco_por_kg, estado_conservacao, disponibilidade FROM estoques WHERE id_estoque = ?"
                + (ehPescador() ? " AND id_pescador = ?" : "");
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idEstoque);
            if (ehPescador()) pst.setInt(2, idLogado);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Item não encontrado no estoque.", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                quantidade = rs.getBigDecimal("quantidade_kg");
                preco = rs.getBigDecimal("preco_por_kg");
                conservacao = rs.getString("estado_conservacao");
                disponivel = rs.getBoolean("disponibilidade");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao carregar estoque: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField txtQuantidade = new JTextField(decimalParaCampo(quantidade), 10);
        JTextField txtPreco = new JTextField(decimalParaCampo(preco), 10);
        JComboBox<String> cbConservacao = new JComboBox<>(new String[]{"FRESCO", "CONGELADO"});
        if (conservacao != null) {
            boolean achou = false;
            for (int i = 0; i < cbConservacao.getItemCount(); i++) {
                if (conservacao.equals(cbConservacao.getItemAt(i))) achou = true;
            }
            if (!achou) cbConservacao.addItem(conservacao);
            cbConservacao.setSelectedItem(conservacao);
        }
        JCheckBox chkDisponivel = new JCheckBox("Disponível para venda", disponivel);

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Quantidade (kg):"));
        form.add(txtQuantidade);
        form.add(new JLabel("Preço por kg (R$):"));
        form.add(txtPreco);
        form.add(new JLabel("Conservação:"));
        form.add(cbConservacao);
        form.add(new JLabel(""));
        form.add(chkDisponivel);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Editar estoque",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return;

        BigDecimal novaQtd = lerDecimal(txtQuantidade.getText());
        BigDecimal novoPreco = lerDecimal(txtPreco.getText());
        if (novaQtd == null || novoPreco == null || novaQtd.compareTo(BigDecimal.ZERO) < 0 || novoPreco.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Informe quantidade zero ou maior, e um preço maior que zero.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean novaDisponibilidade = chkDisponivel.isSelected() && novaQtd.compareTo(BigDecimal.ZERO) > 0;

        String update = "UPDATE estoques SET quantidade_kg = ?, preco_por_kg = ?, estado_conservacao = ?, disponibilidade = ? WHERE id_estoque = ?"
                + (ehPescador() ? " AND id_pescador = ?" : "");
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(update)) {
            pst.setBigDecimal(1, novaQtd);
            pst.setBigDecimal(2, novoPreco);
            pst.setString(3, (String) cbConservacao.getSelectedItem());
            pst.setBoolean(4, novaDisponibilidade);
            pst.setInt(5, idEstoque);
            if (ehPescador()) pst.setInt(6, idLogado);
            if (pst.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(this, "Não foi possível atualizar esse item.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, novaQtd.compareTo(BigDecimal.ZERO) == 0
                    ? "Estoque atualizado. Com 0 kg o item saiu de venda."
                    : "Estoque atualizado.");
            atualizarTabela(identificador);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar estoque: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void desativarEstoque(String identificador) {
        int idEstoque = idLinhaSelecionada(identificador);
        if (idEstoque < 0) return;
        if (JOptionPane.showConfirmDialog(this,
                "Tirar este item de venda? Ele sai do catálogo do cliente e continua no estoque.",
                "Tirar de venda", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE estoques SET disponibilidade = FALSE WHERE id_estoque = ?"
                + (ehPescador() ? " AND id_pescador = ?" : "");
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, idEstoque);
            if (ehPescador()) pst.setInt(2, idLogado);
            if (pst.executeUpdate() == 0) {
                JOptionPane.showMessageDialog(this, "Não foi possível atualizar esse item.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "Item retirado de venda. Para voltar, use Editar selecionado.");
            atualizarTabela(identificador);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao atualizar estoque: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirDialogoCompra(String identificador) {
        int idEstoque = idLinhaSelecionada(identificador);
        if (idEstoque < 0) return;

        String nome;
        BigDecimal disponivel;
        BigDecimal preco;
        try (Connection con = ConexaoMySQL.abrir();
             PreparedStatement pst = con.prepareStatement(
                     "SELECT pe.nome_popular, e.quantidade_kg, e.preco_por_kg FROM estoques e "
                             + "INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe "
                             + "WHERE e.id_estoque = ? AND e.disponibilidade = TRUE")) {
            pst.setInt(1, idEstoque);
            try (ResultSet rs = pst.executeQuery()) {
                if (!rs.next()) {
                    JOptionPane.showMessageDialog(this, "Esse peixe não está mais disponível.",
                            "Aviso", JOptionPane.WARNING_MESSAGE);
                    atualizarTabela(identificador);
                    return;
                }
                nome = rs.getString("nome_popular");
                disponivel = rs.getBigDecimal("quantidade_kg");
                preco = rs.getBigDecimal("preco_por_kg");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao consultar estoque: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (disponivel == null) disponivel = BigDecimal.ZERO;
        if (preco == null) preco = BigDecimal.ZERO;

        JTextField txtQtd = new JTextField(8);
        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));
        form.add(new JLabel("Peixe: " + nome));
        form.add(new JLabel("Disponível: " + formatarKg(disponivel) + "   |   Preço: " + formatarReais(preco) + "/kg"));
        form.add(new JLabel("Quantos kg você quer comprar?"));
        form.add(txtQtd);

        int opcao = JOptionPane.showConfirmDialog(this, form, "Comprar peixe",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opcao != JOptionPane.OK_OPTION) return;

        BigDecimal qtd = lerDecimal(txtQtd.getText());
        if (qtd == null || qtd.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "Informe uma quantidade maior que zero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (qtd.compareTo(disponivel) > 0) {
            JOptionPane.showMessageDialog(this,
                    "A quantidade passa do estoque disponível (" + formatarKg(disponivel) + ").",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BigDecimal total = concluirCompra(idEstoque, qtd);
            JOptionPane.showMessageDialog(this, "Compra de " + nome + " registrada. Total: " + formatarReais(total));
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível concluir a compra: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
        atualizarTabela(identificador);
    }

    private BigDecimal concluirCompra(int idEstoque, BigDecimal qtdPedida) throws SQLException {
        try (Connection con = ConexaoMySQL.abrir()) {
            con.setAutoCommit(false);
            try {
                BigDecimal preco;
                try (PreparedStatement pst = con.prepareStatement(
                        "SELECT preco_por_kg FROM estoques WHERE id_estoque = ? AND disponibilidade = TRUE")) {
                    pst.setInt(1, idEstoque);
                    try (ResultSet rs = pst.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Esse peixe não está mais disponível.");
                        preco = rs.getBigDecimal("preco_por_kg");
                    }
                }
                if (preco == null) throw new SQLException("Preço do estoque inválido.");

                try (PreparedStatement pst = con.prepareStatement(
                        "UPDATE estoques SET disponibilidade = CASE WHEN quantidade_kg - ? > 0 THEN 1 ELSE 0 END, "
                                + "quantidade_kg = quantidade_kg - ? "
                                + "WHERE id_estoque = ? AND disponibilidade = TRUE AND quantidade_kg >= ?")) {
                    pst.setBigDecimal(1, qtdPedida);
                    pst.setBigDecimal(2, qtdPedida);
                    pst.setInt(3, idEstoque);
                    pst.setBigDecimal(4, qtdPedida);
                    if (pst.executeUpdate() != 1) throw new SQLException("Estoque insuficiente.");
                }

                BigDecimal total = qtdPedida.multiply(preco).setScale(2, RoundingMode.HALF_UP);
                try (PreparedStatement pst = con.prepareStatement(
                        "INSERT INTO pedidos (id_cliente, id_estoque, quantidade_kg, preco_por_kg, total) VALUES (?, ?, ?, ?, ?)")) {
                    pst.setInt(1, idLogado);
                    pst.setInt(2, idEstoque);
                    pst.setBigDecimal(3, qtdPedida);
                    pst.setBigDecimal(4, preco);
                    pst.setBigDecimal(5, total);
                    pst.executeUpdate();
                }
                con.commit();
                return total;
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    private int idLinhaSelecionada(String identificador) {
        JTable tabela = tabelas.get(identificador);
        if (tabela == null) return -1;
        int view = tabela.getSelectedRow();
        if (view < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um item da tabela.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return -1;
        }
        int modelRow = tabela.convertRowIndexToModel(view);
        Object valor = tabela.getModel().getValueAt(modelRow, 0);
        if (valor instanceof Number) return ((Number) valor).intValue();
        try {
            return Integer.parseInt(String.valueOf(valor));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Não foi possível identificar a linha selecionada.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return -1;
        }
    }

    private BigDecimal lerDecimal(String texto) {
        if (texto == null) return null;
        String limpo = texto.trim().replace(",", ".");
        if (limpo.isEmpty()) return null;
        try {
            return new BigDecimal(limpo);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String decimalParaCampo(BigDecimal valor) {
        if (valor == null) return "";
        return valor.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private String formatarKg(BigDecimal valor) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        nf.setMinimumFractionDigits(1);
        nf.setMaximumFractionDigits(2);
        return nf.format(valor == null ? BigDecimal.ZERO : valor) + " kg";
    }

    private String formatarReais(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
                .format(valor == null ? BigDecimal.ZERO : valor);
    }

    private void atualizarTabela(String identificador) {
        JTable tabela = tabelas.get(identificador);
        String sql = consultas.get(identificador);
        if (tabela == null || sql == null) return;

        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        try (Connection con = ConexaoMySQL.abrir();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData dados = rs.getMetaData();
            int colunas = dados.getColumnCount();
            for (int i = 1; i <= colunas; i++) modelo.addColumn(dados.getColumnLabel(i));

            while (rs.next()) {
                Object[] linha = new Object[colunas];
                for (int i = 1; i <= colunas; i++) linha[i - 1] = rs.getObject(i);
                modelo.addRow(linha);
            }
            tabela.setModel(modelo);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro de banco de dados: " + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            BancoInicial.preparar();
            new TelaLogin().setVisible(true);
        });
    }
}
