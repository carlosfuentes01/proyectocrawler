CREATE DATABASE IF NOT EXISTS taller1_db;
USE taller1_db;

--  persona a consultar
CREATE TABLE IF NOT EXISTS persona (
    id_persona INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(45) NOT NULL,
    apellido VARCHAR(45),
    pais VARCHAR(45) NOT NULL,
    ciudad VARCHAR(45),
    profesion VARCHAR(45),
    empresa VARCHAR(45),
    alias VARCHAR(45),
    palabra_clave TEXT
);

 --fuentes públicas por país
CREATE TABLE IF NOT EXISTS fuente (
    id_fuente INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    url_inicial VARCHAR(255) NOT NULL,
    estado ENUM('ACTIVA','INACTIVA') NOT NULL DEFAULT 'ACTIVA',
    pais VARCHAR(45) NOT NULL,
    tipo VARCHAR(45)
);

-- documentos encontrados por el crawler
CREATE TABLE IF NOT EXISTS documento (
    id_documento INT AUTO_INCREMENT PRIMARY KEY,
    id_fuente INT NOT NULL,
    id_persona INT NOT NULL,
    relacion ENUM('RELACIONADO','NO_RELACIONADO') NOT NULL DEFAULT 'NO_RELACIONADO',
    titulo VARCHAR(255),
    url VARCHAR(255) NOT NULL,
    pais VARCHAR(45),
    fecha_publicacion DATE NULL,
    fecha_consulta DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    contenido_textual LONGTEXT,
    estado_url ENUM('PENDIENTE','EN_PROCESAMIENTO','PROCESADA','DESCARTADA','ERROR') NOT NULL DEFAULT 'PENDIENTE',
    verificacion_identidad ENUM('MISMA_PERSONA','POSIBLE_COINCIDENCIA','PERSONA_DIFERENTE','NO_DETERMINADO') DEFAULT 'NO_DETERMINADO',
    clasificacion_contextual ENUM('POSITIVO','NEUTRO','NEGATIVO','NO_DETERMINADO') DEFAULT 'NO_DETERMINADO',
    motivo_descarte VARCHAR(255) NULL,
    CONSTRAINT fk_documento_fuente FOREIGN KEY (id_fuente) REFERENCES fuente(id_fuente),
    CONSTRAINT fk_documento_persona FOREIGN KEY (id_persona) REFERENCES persona(id_persona),
    UNIQUE KEY uk_documento_url_persona (url, id_persona)
);

-- Métricas de concurrencia (1 vs. N workers)
CREATE TABLE IF NOT EXISTS metrica_concurrencia (
    id_metrica INT AUTO_INCREMENT PRIMARY KEY,
    num_workers INT NOT NULL,
    urls_procesadas INT NOT NULL,
    duracion_ms BIGINT NOT NULL,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- (testeo.html)
CREATE TABLE IF NOT EXISTS resultado (
    id INT AUTO_INCREMENT PRIMARY KEY,
    resultado INT NOT NULL
);

-- Fuente semilla de ejemplo para Colombia (RF2)
--INSERT INTO fuente (nombre, url_inicial, estado, pais, tipo)
--VALUES ('El Tiempo', 'https://www.eltiempo.com', 'ACTIVA', 'Colombia', 'NOTICIAS');
CREATE TABLE `test` (
  `id` int NOT NULL,
  `resultado` int DEFAULT NULL,
  `numeroa` float DEFAULT NULL,
  `numerob` float DEFAULT NULL,
  `estado` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;