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
        "SELECT id_fuente, nombre, url_inicial, estado, pais, tipo
         FROM fuente ORDER BY id_fuente DESC"
    );
    $sentenciaPreparada->execute();

    header('Content-Type: application/json');
    echo json_encode($sentenciaPreparada->fetchAll(PDO::FETCH_ASSOC));
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al listar fuentes",
        "detalle" => $excepcion->getMessage()
    ]);
}