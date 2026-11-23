-- Recria o banco do zero. Apaga PeixariaA3 se ele já existir.
DROP DATABASE IF EXISTS PeixariaA3;
CREATE DATABASE PeixariaA3;
USE PeixariaA3;
CREATE TABLE municipios (
    id_municipio INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    uf CHAR(2) NOT NULL
);
CREATE TABLE pescadores (
    id_pescador INT AUTO_INCREMENT PRIMARY KEY,
    id_municipio INT NOT NULL,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    celular VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_municipio) REFERENCES municipios(id_municipio)
);
CREATE TABLE peixes (
    id_peixe INT AUTO_INCREMENT PRIMARY KEY,
    nome_popular VARCHAR(100) NOT NULL,
    nome_cientifico VARCHAR(150)
);
-- quantidade_kg pode chegar a 0: a compra zera o estoque e tira o peixe do catálogo.
-- O CHECK antigo (quantidade_kg > 0) quebrava essa atualização.
CREATE TABLE estoques (
    id_estoque INT AUTO_INCREMENT PRIMARY KEY,
    id_pescador INT NOT NULL,
    id_peixe INT NOT NULL,
    quantidade_kg DECIMAL(10,2) NOT NULL,
    preco_por_kg DECIMAL(10,2) NOT NULL,
    estado_conservacao ENUM('FRESCO', 'CONGELADO') NOT NULL,
    disponibilidade BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pescador) REFERENCES pescadores(id_pescador),
    FOREIGN KEY (id_peixe) REFERENCES peixes(id_peixe),
    CHECK (quantidade_kg >= 0),
    CHECK (preco_por_kg > 0)
);
CREATE TABLE clientes (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO municipios (nome, uf) VALUES
('Florianópolis', 'SC'),
('Santos', 'SP'),
('Belém', 'PA'),
('Paranaguá', 'PR'),
('Rio Grande', 'RS'),
('Fortaleza', 'CE'),
('Salvador', 'BA'),
('Recife', 'PE'),
('Vitória', 'ES'),
('São Luís', 'MA'),
('Natal', 'RN'),
('Manaus', 'AM');
-- Senha de todo pescador e cliente de teste: 123456
-- O Java aceita o texto e troca pelo hash no primeiro login.
INSERT INTO pescadores (id_municipio, nome, email, senha_hash, celular) VALUES
(1, 'Geovane Silva', 'geovane@peixaria.com', '123456', '(48) 99999-1111'),
(2, 'Carlos Souza', 'carlos@peixaria.com', '123456', '(13) 98888-2222'),
(3, 'Ana Ribeiro', 'ana@peixaria.com', '123456', '(91) 97777-3333'),
(4, 'João Pacheco', 'joao@peixaria.com', '123456', '(41) 98888-4444'),
(5, 'Lúcia Mendes', 'lucia@peixaria.com', '123456', '(53) 97777-5555'),
(6, 'Pedro Alves', 'pedro@peixaria.com', '123456', '(85) 96666-6666'),
(7, 'Marina Dias', 'marina@peixaria.com', '123456', '(71) 95555-7777'),
(8, 'Rafael Nunes', 'rafael@peixaria.com', '123456', '(81) 94444-8888'),
(9, 'Beatriz Lima', 'beatriz@peixaria.com', '123456', '(27) 93333-9999'),
(10, 'Hugo Ferreira', 'hugo@peixaria.com', '123456', '(98) 92222-1010'),
(11, 'Camila Rocha', 'camila@peixaria.com', '123456', '(84) 91111-2020'),
(12, 'Igor Bentes', 'igor@peixaria.com', '123456', '(92) 99000-3030');
INSERT INTO peixes (nome_popular, nome_cientifico) VALUES
('Tainha', 'Mugil liza'),
('Robalo', 'Centropomus undecimalis'),
('Pintado', 'Pseudoplatystoma corruscans'),
('Tilápia', 'Oreochromis niloticus'),
('Tambaqui', 'Colossoma macropomum'),
('Pacu', 'Piaractus mesopotamicus'),
('Tucunaré', 'Cichla ocellaris'),
('Corvina', 'Micropogonias furnieri'),
('Dourado', 'Salminus brasiliensis'),
('Pescada', 'Cynoscion spp.'),
('Sardinha', 'Sardinella brasiliensis'),
('Anchova', 'Pomatomus saltatrix'),
('Atum', 'Thunnus albacares'),
('Cação', 'Rhizoprionodon spp.'),
('Linguado', 'Paralichthys spp.'),
('Bagre', 'Genidens genidens'),
('Pirarucu', 'Arapaima gigas'),
('Jaraqui', 'Semaprochilodus insignis');
-- Cada linha usa um pescador e um peixe que já existem acima.
INSERT INTO estoques (id_pescador, id_peixe, quantidade_kg, preco_por_kg, estado_conservacao, disponibilidade) VALUES
(1, 1, 50.50, 25.00, 'FRESCO', TRUE),
(1, 4, 18.00, 18.90, 'FRESCO', TRUE),
(1, 11, 40.00, 12.00, 'CONGELADO', TRUE),
(2, 2, 30.00, 45.50, 'CONGELADO', TRUE),
(2, 8, 22.40, 32.00, 'FRESCO', TRUE),
(2, 15, 8.00, 55.00, 'FRESCO', FALSE),
(3, 3, 100.00, 38.00, 'FRESCO', TRUE),
(3, 5, 75.00, 28.50, 'CONGELADO', TRUE),
(3, 7, 16.75, 42.00, 'FRESCO', TRUE),
(4, 8, 27.00, 30.00, 'FRESCO', TRUE),
(4, 12, 14.20, 36.00, 'CONGELADO', TRUE),
(5, 9, 19.50, 48.00, 'FRESCO', TRUE),
(5, 10, 33.00, 22.50, 'CONGELADO', TRUE),
(6, 2, 12.00, 52.00, 'FRESCO', TRUE),
(6, 13, 9.80, 70.00, 'CONGELADO', TRUE),
(7, 14, 21.00, 29.90, 'FRESCO', TRUE),
(7, 1, 15.00, 24.00, 'CONGELADO', TRUE),
(8, 11, 60.00, 11.50, 'FRESCO', TRUE),
(8, 16, 25.00, 18.00, 'FRESCO', TRUE),
(9, 10, 18.30, 26.00, 'FRESCO', TRUE),
(9, 15, 6.50, 58.00, 'CONGELADO', TRUE),
(10, 8, 40.00, 27.00, 'CONGELADO', TRUE),
(10, 3, 13.00, 41.00, 'FRESCO', TRUE),
(11, 12, 11.00, 39.00, 'FRESCO', TRUE),
(11, 4, 28.00, 17.50, 'CONGELADO', TRUE),
(12, 17, 22.00, 64.00, 'FRESCO', TRUE),
(12, 18, 35.00, 16.00, 'FRESCO', TRUE),
(12, 6, 20.00, 23.00, 'CONGELADO', FALSE);
INSERT INTO clientes (nome, email, senha) VALUES
('Mariana Costa', 'mariana@email.com', '123456'),
('Lucas Andrade', 'lucas@email.com', '123456'),
('Fernanda Melo', 'fernanda@email.com', '123456'),
('Paulo Henrique', 'paulo@email.com', '123456'),
('Aline Souza', 'aline@email.com', '123456');
