<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(["error" => "Solo GET."]);
    exit;
}

$identificadorDocumento = $_GET['idDocumento'] ?? null;

if ($identificadorDocumento === null || !ctype_digit((string)$identificadorDocumento)) {
    http_response_code(400);
    echo json_encode(["error" => "Parametro 'idDocumento' requerido y debe ser numerico."]);
    exit;
}

try {
    $sentenciaPreparada = $conexion->prepare("SELECT * FROM documento WHERE id_documento = ?");
    $sentenciaPreparada->execute([$identificadorDocumento]);
    $filaEncontrada = $sentenciaPreparada->fetch(PDO::FETCH_ASSOC);

    if (!$filaEncontrada) {
        http_response_code(404);
        echo json_encode(["error" => "No existe un documento con ese id."]);
        exit;
    }

    header('Content-Type: application/json');
    echo json_encode($filaEncontrada);
} catch (\PDOException $excepcion) {
    http_response_code(500);
    echo json_encode(["error" => $excepcion->getMessage()]);
}