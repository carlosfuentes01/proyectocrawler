<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Solo POST."]);
    exit;
}

$datosRecibidos = json_decode(file_get_contents("php://input"), true);

$idFuente    = $datosRecibidos['idFuente']    ?? null;
$idPersona   = $datosRecibidos['idPersona']   ?? null;
$numeroHilos = $datosRecibidos['numeroHilos'] ?? 2;

if (!$idFuente || !$idPersona) {
    http_response_code(400);
    echo json_encode(["error" => "idFuente e idPersona son requeridos."]);
    exit;
}

// La URL semilla ya no viene del navegador: se toma de la fuente.
try {
    $sentenciaFuente = $conexion->prepare(
        "SELECT url_inicial FROM fuente WHERE id_fuente = ?"
    );
    $sentenciaFuente->execute([$idFuente]);
    $filaFuente = $sentenciaFuente->fetch(PDO::FETCH_ASSOC);

    if (!$filaFuente || empty($filaFuente['url_inicial'])) {
        http_response_code(404);
        echo json_encode(["error" => "No existe la fuente indicada o no tiene url_inicial."]);
        exit;
    }

    $urlSemilla = $filaFuente['url_inicial'];
} catch (Throwable $excepcion) {
    http_response_code(500);
    echo json_encode(["error" => "Error al obtener la fuente: " . $excepcion->getMessage()]);
    exit;
}

// Reenvia a Java usando la url_inicial de la fuente
$urlJava = "http://servicio_java:8083/crawler/iniciar"
    . "?idFuente="    . urlencode($idFuente)
    . "&idPersona="   . urlencode($idPersona)
    . "&urlSemilla="  . urlencode($urlSemilla)
    . "&numeroHilos=" . urlencode($numeroHilos);

$ch = curl_init($urlJava);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
$respuestaJava = curl_exec($ch);
$httpCode      = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$err           = curl_error($ch);
curl_close($ch);

if ($respuestaJava === false || $httpCode === 0 || $httpCode >= 500) {
    http_response_code(502);
    echo json_encode([
        "error"     => "No se pudo contactar al servicio Java.",
        "detalle"   => $err,
        "http_code" => $httpCode,
        "urlJava"   => $urlJava
    ]);
    exit;
}

header('Content-Type: application/json');
echo $respuestaJava;