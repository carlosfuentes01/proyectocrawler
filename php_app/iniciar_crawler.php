<?php
require_once 'cabecera.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $datosRecibidos = json_decode(file_get_contents("php://input"), true);

    $identificadorFuente = $datosRecibidos['idFuente'] ?? null;
    $identificadorPersona = $datosRecibidos['idPersona'] ?? null;
    $urlSemilla = $datosRecibidos['urlSemilla'] ?? null;
    $numeroDeHilos = $datosRecibidos['numeroHilos'] ?? 2;

    if (!$identificadorFuente || !$identificadorPersona || !$urlSemilla) {
        http_response_code(400);
        echo json_encode(["error" => "idFuente, idPersona y urlSemilla son requeridos."]);
        exit;
    }

    $urlJava = "http://servicio_java:8083/crawler/iniciar"
        . "?idFuente=" . urlencode($identificadorFuente)
        . "&idPersona=" . urlencode($identificadorPersona)
        . "&urlSemilla=" . urlencode($urlSemilla)
        . "&numeroHilos=" . urlencode($numeroDeHilos);

    $respuestaJava = @file_get_contents($urlJava);

    if ($respuestaJava === false) {
        http_response_code(502);
        echo json_encode(["error" => "No se pudo contactar al servicio Java."]);
        exit;
    }

    echo $respuestaJava;
}