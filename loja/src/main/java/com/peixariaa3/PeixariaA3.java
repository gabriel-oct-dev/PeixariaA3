package com.peixariaa3;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

// ==========================================
// TELA DE LOGIN / ESCOLHA INICIAL
// ==========================================
class TelaLogin extends JFrame {

    private final Color AZUL = new Color(12, 52, 76);
    private final Color AZUL_CLARO = new Color(20, 115, 145);
    private final Color BRANCO = Color.WHITE;
    private final Color FUNDO = new Color(240, 245, 248);

    public TelaLogin() {
        setTitle("CONECTA_PESCA| Bem-vindo");
        setSize(480, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel painelPrincipal = new JPanel(new BorderLayout());
        painelPrincipal.setBackground(FUNDO);

        // Cabeçalho
        JPanel painelTitulo = new JPanel();
        painelTitulo.setBackground(AZUL);
        painelTitulo.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));
        JLabel lblTitulo = new JLabel("CONECTA_PESCA", SwingConstants.CENTER);
        lblTitulo.setForeground(BRANCO);
        lblTitulo.setFont(new Font("Arial", Font.BOLD, 24));
        painelTitulo.add(lblTitulo);
        painelPrincipal.add(painelTitulo, BorderLayout.NORTH);

        // Abas de Opções
        JTabbedPane abas = new JTabbedPane();
        abas.setFont(new Font("Arial", Font.BOLD, 13));

        // Aba 1: Entrar / Login
        JPanel painelEntrar = new JPanel(new GridBagLayout());
        painelEntrar.setBackground(BRANCO);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtUsuario = new JTextField(15);
        JPasswordField txtSenha = new JPasswordField(15);
        
        JButton btnEntrar = new JButton("Entrar no Sistema");
        estilizarBotaoPadrao(btnEntrar); // Mesmo estilo dos botões de consulta

        gbc.gridx = 0; gbc.gridy = 0; painelEntrar.add(new JLabel("Usuário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; painelEntrar.add(txtUsuario, gbc);
        gbc.gridx = 0; gbc.gridy = 1; painelEntrar.add(new JLabel("Senha:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; painelEntrar.add(txtSenha, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; painelEntrar.add(btnEntrar, gbc);

        btnEntrar.addActionListener(e -> {
            String usuario = txtUsuario.getText();
            String senha = new String(txtSenha.getPassword());

            if (usuario.equals("root") && senha.equals("03102005")) {
                dispose();
                SwingUtilities.invokeLater(() -> new PeixariaA3().setVisible(true));
            } else {
                JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos!", "Erro de Login", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Aba 2: Cadastrar
        JPanel painelCadastrar = new JPanel(new GridBagLayout());
        painelCadastrar.setBackground(BRANCO);
        JButton btnIrCadastro = new JButton("Ir para Tela de Cadastro");
        estilizarBotaoPadrao(btnIrCadastro);
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        painelCadastrar.add(new JLabel("Deseja registrar um novo produto/pescador?"), gbc);
        gbc.gridy = 1;
        painelCadastrar.add(btnIrCadastro, gbc);

        btnIrCadastro.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Para cadastrar novos registros, entre no sistema com sua conta de Administrador.");
        });

        // Aba 3: Consultas Rápidas
        JPanel painelConsultas = new JPanel(new GridLayout(2, 1, 15, 15));
        painelConsultas.setBackground(BRANCO);
        painelConsultas.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JButton btnConsultaMunicipios = new JButton("Consultar Municípios");
        JButton btnConsultaEstoques = new JButton("Consultar Estoque");
        estilizarBotaoPadrao(btnConsultaMunicipios);
        estilizarBotaoPadrao(btnConsultaEstoques);

        btnConsultaMunicipios.addActionListener(e -> {
            new TelaConsultaRapida("Municípios", "SELECT id_municipio, nome, uf FROM municipios").setVisible(true);
        });

        btnConsultaEstoques.addActionListener(e -> {
            new TelaConsultaRapida("Estoques", "SELECT e.id_estoque, p.nome AS pescador, pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao, e.disponibilidade FROM estoques e INNER JOIN pescadores p ON e.id_pescador = p.id_pescador INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe").setVisible(true);
        });

        painelConsultas.add(btnConsultaMunicipios);
        painelConsultas.add(btnConsultaEstoques);

        abas.add("Entrar", painelEntrar);
        abas.add("Cadastrar", painelCadastrar);
        abas.add("Consultas", painelConsultas);

        painelPrincipal.add(abas, BorderLayout.CENTER);
        add(painelPrincipal);
    }

    // Estilo padrão unificado (igual aos botões de consulta)
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
    private static final String URL = "jdbc:mysql://localhost:3306/PeixariaA3?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String SENHA = "03102005";

    public TelaConsultaRapida(String titulo, String sql) {
        setTitle("Peixaria A3 | Consulta de " + titulo);
        setSize(700, 400);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JTable tabela = new JTable();
        tabela.setRowHeight(28);
        JScrollPane scroll = new JScrollPane(tabela);

        DefaultTableModel modelo = new DefaultTableModel();
        try (Connection con = DriverManager.getConnection(URL, USUARIO, SENHA);
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                modelo.addColumn(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                Object[] linha = new Object[cols];
                for (int i = 1; i <= cols; i++) {
                    linha[i - 1] = rs.getObject(i);
                }
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
public class PeixariaA3 extends JFrame {

    private final Color AZUL = new Color(12, 52, 76);
    private final Color AZUL_CLARO = new Color(20, 115, 145);
    private final Color FUNDO = new Color(240, 245, 248);
    private final Color BRANCO = Color.WHITE;

    private JPanel conteudo;
    private CardLayout layout;

    private final Map<String, JTable> tabelas = new HashMap<>();
    private final Map<String, String> consultas = new HashMap<>();

    private static final String URL =
            "jdbc:mysql://localhost:3306/PeixariaA3?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USUARIO = "root";
    private static final String SENHA = "03102005";

    private Connection conectar() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL não encontrado.", e);
        }
        return DriverManager.getConnection(URL, USUARIO, SENHA);
    }

    public PeixariaA3() {
        setTitle("Peixaria A3 | Sistema de Gerenciamento");
        setSize(1150, 720);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        criarMenu();
        criarConteudo();
    }

    private void criarMenu() {
        JPanel menu = new JPanel();
        menu.setBackground(AZUL);
        menu.setPreferredSize(new Dimension(235, 700));
        menu.setLayout(new GridLayout(10, 1, 5, 5));

        JLabel titulo = new JLabel("  PEIXARIA A3");
        titulo.setForeground(BRANCO);
        titulo.setFont(new Font("Arial", Font.BOLD, 23));
        menu.add(titulo);

        adicionarBotao(menu, "Dashboard", "dashboard");
        adicionarBotao(menu, "Pescadores", "pescadores");
        adicionarBotao(menu, "Municípios", "municipios");
        adicionarBotao(menu, "Peixes", "peixes");
        adicionarBotao(menu, "Estoques", "estoques");

        JButton sair = new JButton("Sair / Trocar Conta");
        estilizarBotao(sair);

        sair.addActionListener(e -> {
            int resposta = JOptionPane.showConfirmDialog(
                    this, "Deseja realmente sair do sistema?", "Confirmar saída", JOptionPane.YES_NO_OPTION
            );
            if (resposta == JOptionPane.YES_OPTION) {
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
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void adicionarBotao(JPanel menu, String nome, String pagina) {
        JButton botao = new JButton(nome);
        estilizarBotao(botao);
        botao.addActionListener(e -> {
            layout.show(conteudo, pagina);
            if (tabelas.containsKey(pagina)) {
                atualizarTabela(pagina);
            }
        });
        menu.add(botao);
    }

    private void criarConteudo() {
        layout = new CardLayout();
        conteudo = new JPanel(layout);
        conteudo.setBackground(FUNDO);

        conteudo.add(criarDashboard(), "dashboard");
        conteudo.add(criarPagina("Pescadores", "pescadores", "SELECT id_pescador, nome, email, celular, ativo FROM pescadores"), "pescadores");
        conteudo.add(criarPagina("Municípios", "municipios", "SELECT id_municipio, nome, uf FROM municipios"), "municipios");
        conteudo.add(criarPagina("Peixes", "peixes", "SELECT id_peixe, nome_popular, nome_cientifico FROM peixes"), "peixes");
        conteudo.add(criarPagina("Estoques", "estoques", "SELECT e.id_estoque, p.nome AS pescador, pe.nome_popular AS peixe, e.quantidade_kg, e.preco_por_kg, e.estado_conservacao, e.disponibilidade FROM estoques e INNER JOIN pescadores p ON e.id_pescador = p.id_pescador INNER JOIN peixes pe ON e.id_peixe = pe.id_peixe"), "estoques");

        add(conteudo, BorderLayout.CENTER);
        layout.show(conteudo, "dashboard");
    }

    private JPanel criarDashboard() {
        JPanel painel = new JPanel(new BorderLayout(20, 20));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titulo = new JLabel("Dashboard");
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
        cards.add(criarCard("Pescadores cadastrados", contar("pescadores")));
        cards.add(criarCard("Tipos de peixes", contar("peixes")));
        cards.add(criarCard("Registros de estoque", contar("estoques")));

        painel.add(cabecalho, BorderLayout.NORTH);
        painel.add(cards, BorderLayout.CENTER);
        return painel;
    }

    private int contar(String tabela) {
        String sql = "SELECT COUNT(*) FROM " + tabela;
        try (Connection con = conectar(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            mostrarErroBanco(e);
        }
        return 0;
    }

    private JPanel criarCard(String nome, int valor) {
        JPanel card = new JPanel(new GridLayout(2, 1, 5, 5));
        card.setBackground(BRANCO);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(215, 225, 230)), BorderFactory.createEmptyBorder(20, 10, 20, 10)));

        JLabel texto = new JLabel(nome, SwingConstants.CENTER);
        texto.setFont(new Font("Arial", Font.BOLD, 16));
        texto.setForeground(AZUL);

        JLabel numero = new JLabel(String.valueOf(valor), SwingConstants.CENTER);
        numero.setFont(new Font("Arial", Font.BOLD, 34));
        numero.setForeground(AZUL_CLARO);

        card.add(texto);
        card.add(numero);
        return card;
    }

    private JPanel criarPagina(String titulo, String identificador, String sql) {
        JPanel painel = new JPanel(new BorderLayout(15, 15));
        painel.setBackground(FUNDO);
        painel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel nome = new JLabel(titulo);
        nome.setFont(new Font("Arial", Font.BOLD, 28));
        nome.setForeground(AZUL);

        JTable tabela = new JTable();
        tabela.setRowHeight(32);
        tabela.setFont(new Font("Arial", Font.PLAIN, 14));
        tabela.setSelectionBackground(new Color(210, 230, 240));
        tabela.setSelectionForeground(AZUL);

        tabela.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        tabela.getTableHeader().setBackground(AZUL);
        tabela.getTableHeader().setForeground(BRANCO);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.getViewport().setBackground(BRANCO);

        JButton atualizar = new JButton("Atualizar dados");
        estilizarBotao(atualizar);
        atualizar.addActionListener(e -> atualizarTabela(identificador));

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.setBackground(FUNDO);
        botoes.add(atualizar);

        painel.add(nome, BorderLayout.NORTH);
        painel.add(scroll, BorderLayout.CENTER);
        painel.add(botoes, BorderLayout.SOUTH);

        tabelas.put(identificador, tabela);
        consultas.put(identificador, sql);

        SwingUtilities.invokeLater(() -> atualizarTabela(identificador));
        return painel;
    }

    private void atualizarTabela(String identificador) {
        JTable tabela = tabelas.get(identificador);
        String sql = consultas.get(identificador);
        if (tabela == null || sql == null) return;
        atualizarTabela(sql, tabela);
    }

    private void atualizarTabela(String sql, JTable tabela) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        try (Connection con = conectar(); Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData dados = rs.getMetaData();
            int colunas = dados.getColumnCount();

            for (int i = 1; i <= colunas; i++) {
                modelo.addColumn(dados.getColumnLabel(i));
            }

            while (rs.next()) {
                Object[] linha = new Object[colunas];
                for (int i = 1; i <= colunas; i++) {
                    linha[i - 1] = rs.getObject(i);
                }
                modelo.addRow(linha);
            }
            tabela.setModel(modelo);
        } catch (SQLException e) {
            mostrarErroBanco(e);
        }
    }

    private void mostrarErroBanco(SQLException e) {
        JOptionPane.showMessageDialog(this, "Erro de banco de dados: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("Erro ao carregar tema.");
            }
            new TelaLogin().setVisible(true);
        });
    }
}