<?php
require_once 'cabecera.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["error" => "Solo POST."]);
    exit;
}

$datos = json_decode(file_get_contents("php://input"), true);
$n1 = $datos['num1'] ?? null;
$n2 = $datos['num2'] ?? null;

if ($n1 === null || $n2 === null) {
    http_response_code(400);
    echo json_encode(["error" => "num1 y num2 son requeridos."]);
    exit;
}

$urlJava = "http://servicio_java:8083/procesar?numeroa="
         . urlencode($n1) . "&numerob=" . urlencode($n2);

$ch = curl_init($urlJava);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
curl_setopt($ch, CURLOPT_TIMEOUT, 5);
$respuesta = curl_exec($ch);
$httpCode  = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$err       = curl_error($ch);
curl_close($ch);

if ($respuesta === false || $httpCode === 0 || $httpCode >= 500) {
    http_response_code(502);
    echo json_encode([
        "error"     => "No se pudo contactar al servicio Java.",
        "detalle"   => $err,
        "http_code" => $httpCode
    ]);
    exit;
}

header('Content-Type: application/json');
echo $respuesta;