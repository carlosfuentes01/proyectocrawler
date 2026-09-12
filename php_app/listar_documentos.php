<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(["error" => "Solo GET."]);
    exit;
}

try {
    $sentenciaPreparada = $conexion->prepare(
        "SELECT id_documento, titulo, url, pais, fecha_publicacion, fecha_consulta,
                estado_url, verificacion_identidad, clasificacion_contextual,
                SUBSTRING(contenido_textual, 1, 300) AS fragmento_contenido
         FROM documento
         WHERE estado_url <> 'DESCARTADA'
         ORDER BY id_documento DESC"
    );
    $sentenciaPreparada->execute();

    header('Content-Type: application/json');
    echo json_encode($sentenciaPreparada->fetchAll(PDO::FETCH_ASSOC));
} catch (\PDOException $excepcion) {
    http_response_code(500);
    echo json_encode(["error" => $excepcion->getMessage()]);
}
