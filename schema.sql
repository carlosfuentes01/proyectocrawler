-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Servidor: base_datos
-- Tiempo de generación: 12-09-2026 a las 04:56:39
-- Versión del servidor: 8.0.46
-- Versión de PHP: 8.3.33

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `taller1_db`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `documento`
--

CREATE TABLE `documento` (
  `id_documento` int NOT NULL,
  `id_fuente` int NOT NULL,
  `id_persona` int NOT NULL,
  `relacion` enum('RELACIONADO','NO_RELACIONADO') NOT NULL DEFAULT 'NO_RELACIONADO',
  `titulo` varchar(255) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `pais` varchar(45) DEFAULT NULL,
  `fecha_publicacion` date DEFAULT NULL,
  `fecha_consulta` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `contenido_textual` longtext,
  `estado_url` enum('PENDIENTE','EN_PROCESAMIENTO','PROCESADA','DESCARTADA','ERROR') NOT NULL DEFAULT 'PENDIENTE',
  `verificacion_identidad` enum('MISMA_PERSONA','POSIBLE_COINCIDENCIA','PERSONA_DIFERENTE','NO_DETERMINADO') DEFAULT 'NO_DETERMINADO',
  `clasificacion_contextual` enum('POSITIVO','NEUTRO','NEGATIVO','NO_DETERMINADO') DEFAULT 'NO_DETERMINADO',
  `motivo_descarte` varchar(255) DEFAULT NULL,
  `nombre_hilo` varchar(60) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Volcado de datos para la tabla `documento`
--

INSERT INTO `documento` (`id_documento`, `id_fuente`, `id_persona`, `relacion`, `titulo`, `url`, `pais`, `fecha_publicacion`, `fecha_consulta`, `contenido_textual`, `estado_url`, `verificacion_identidad`, `clasificacion_contextual`, `motivo_descarte`, `nombre_hilo`) VALUES
(1, 1, 3, 'NO_RELACIONADO', NULL, 'https://www.facebook.com/profile.php?id=100077063270376', NULL, NULL, '2026-09-12 02:17:53', NULL, 'DESCARTADA', 'NO_DETERMINADO', 'NO_DETERMINADO', 'El contenido no menciona explicitamente a Colombia.', NULL);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `documento`
--
ALTER TABLE `documento`
  ADD PRIMARY KEY (`id_documento`),
  ADD KEY `fk_documento_fuente` (`id_fuente`),
  ADD KEY `fk_documento_persona` (`id_persona`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `documento`
--
ALTER TABLE `documento`
  MODIFY `id_documento` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15670;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `documento`
--
ALTER TABLE `documento`
  ADD CONSTRAINT `fk_documento_fuente` FOREIGN KEY (`id_fuente`) REFERENCES `fuente` (`id_fuente`),
  ADD CONSTRAINT `fk_documento_persona` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
