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

    FOREIGN KEY (id_municipio)
        REFERENCES municipios(id_municipio)
);

CREATE TABLE peixes (
    id_peixe INT AUTO_INCREMENT PRIMARY KEY,
    nome_popular VARCHAR(100) NOT NULL,
    nome_cientifico VARCHAR(150)
);

CREATE TABLE estoques (
    id_estoque INT AUTO_INCREMENT PRIMARY KEY,
    id_pescador INT NOT NULL,
    id_peixe INT NOT NULL,
    quantidade_kg DECIMAL(10,2) NOT NULL,
    preco_por_kg DECIMAL(10,2) NOT NULL,
    estado_conservacao ENUM('FRESCO', 'CONGELADO') NOT NULL,
    disponibilidade BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (id_pescador)
        REFERENCES pescadores(id_pescador),

    FOREIGN KEY (id_peixe)
        REFERENCES peixes(id_peixe),

    CHECK (quantidade_kg > 0),
    CHECK (preco_por_kg > 0)
);

INSERT INTO municipios (nome, uf) VALUES 
('Florianópolis', 'SC'),
('Santos', 'SP'),
('Belém', 'PA');

INSERT INTO pescadores (id_municipio, nome, email, senha_hash, celular) VALUES 
(1, 'Geovane Silva', 'geovane@peixaria.com', '123456', '(48) 99999-1111'),
(2, 'Carlos Souza', 'carlos@peixaria.com', '123456', '(13) 98888-2222'),
(3, 'Ana Ribeiro', 'ana@peixaria.com', '123456', '(91) 97777-3333');

INSERT INTO peixes (nome_popular, nome_cientifico) VALUES 
('Tainha', 'Mugil liza'),
('Robalo', 'Centropomus undecimalis'),
('Pintado', 'Pseudoplatystoma corruscans');

INSERT INTO estoques (id_pescador, id_peixe, quantidade_kg, preco_por_kg, estado_conservacao, disponibilidade) VALUES 
(1, 1, 50.50, 25.00, 'FRESCO', TRUE),
(2, 2, 30.00, 45.50, 'CONGELADO', TRUE),
(3, 3, 100.00, 38.00, 'FRESCO', TRUE);


SELECT * FROM municipios;


SELECT * FROM pescadores;


SELECT * FROM peixes;


SELECT * FROM estoques;
