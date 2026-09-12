<?php
require_once 'cabecera.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(["error" => "Solo GET."]);
    exit;
}

// Pide los datos a Java (que es quien determina/calcula las metricas),
// en vez de que PHP consulte la tabla directamente.
$urlJava = "http://servicio_java:8083/crawler/metricas";

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
        "error"     => "No se pudo contactar al servicio Java para las metricas.",
        "detalle"   => $err,
        "http_code" => $httpCode
    ]);
    exit;
}

header('Content-Type: application/json');
echo $respuestaJava;