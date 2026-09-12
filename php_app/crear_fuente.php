<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Solo POST."]);
    exit;
}

$datosRecibidos = json_decode(file_get_contents("php://input"), true);

$nombre  = trim($datosRecibidos['nombre']  ?? '');
$urlBase = trim($datosRecibidos['urlBase'] ?? '');
$pais    = trim($datosRecibidos['pais']    ?? 'Colombia');
$tipo    = trim($datosRecibidos['tipo']    ?? '');

if ($nombre === '' || $urlBase === '') {
    http_response_code(400);
    echo json_encode(["error" => "nombre y urlBase son obligatorios."]);
    exit;
}

try {
    $sentenciaPreparada = $conexion->prepare(
        "INSERT INTO fuente (nombre, url_inicial, estado, pais, tipo)
         VALUES (?, ?, 'ACTIVA', ?, ?)"
    );
    $sentenciaPreparada->execute([
        $nombre,
        $urlBase,
        $pais !== '' ? $pais : 'Colombia',
        $tipo !== '' ? $tipo : null,
    ]);

    header('Content-Type: application/json');
    echo json_encode([
        "status"   => "OK",
        "idFuente" => (int) $conexion->lastInsertId(),
        "nombre"   => $nombre,
        "urlBase"  => $urlBase
    ]);
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al crear fuente",
        "detalle" => $excepcion->getMessage()
    ]);
}