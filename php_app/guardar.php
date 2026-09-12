<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Solo POST."]);
    exit;
}

$datosRecibidos = json_decode(file_get_contents("php://input"), true);

$nombre       = trim($datosRecibidos['nombre']        ?? '');
$apellido     = trim($datosRecibidos['apellido']      ?? '');
$pais         = trim($datosRecibidos['pais']          ?? 'Colombia');
$ciudad       = trim($datosRecibidos['ciudad']        ?? '');
$profesion    = trim($datosRecibidos['profesion']     ?? '');
$empresa      = trim($datosRecibidos['empresa']       ?? '');
$alias        = trim($datosRecibidos['alias']         ?? '');
$palabraClave = trim($datosRecibidos['palabra_clave'] ?? '');

if ($nombre === '') {
    http_response_code(400);
    echo json_encode(["error" => "El nombre es obligatorio."]);
    exit;
}

try {
    $sentenciaPreparada = $conexion->prepare(
        "INSERT INTO persona (nombre, apellido, pais, ciudad, profesion, empresa, alias, palabra_clave)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    );
    $sentenciaPreparada->execute([
        $nombre,
        $apellido     !== '' ? $apellido     : null,
        $pais         !== '' ? $pais         : 'Colombia',
        $ciudad       !== '' ? $ciudad       : null,
        $profesion    !== '' ? $profesion    : null,
        $empresa      !== '' ? $empresa      : null,
        $alias        !== '' ? $alias        : null,
        $palabraClave !== '' ? $palabraClave : null,
    ]);

    header('Content-Type: application/json');
    echo json_encode([
        "status"    => "OK",
        "message"   => "Persona guardada con ID " . $conexion->lastInsertId(),
        "idPersona" => (int) $conexion->lastInsertId()
    ]);
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al guardar persona",
        "detalle" => $excepcion->getMessage()
    ]);
}