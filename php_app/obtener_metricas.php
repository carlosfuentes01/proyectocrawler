<?php
require_once 'cabecera.php';
 
// Pide los datos a Java (que es quien determina/calcula las metricas),
// en vez de que PHP consulte la tabla directamente.
$respuestaJava = @file_get_contents("http://servicio_java:8083/crawler/metricas");
 
if ($respuestaJava === false) {
    http_response_code(502);
    echo json_encode(["error" => "No se pudo contactar al servicio Java para las metricas."]);
    exit;
}
 
echo $respuestaJava;
 