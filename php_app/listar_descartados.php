<?php
require_once 'cabecera.php';
require_once 'conexion.php';
 
try {
    $sentenciaPreparada = $conexion->prepare(
        "SELECT id_documento, titulo, url, pais, fecha_consulta, motivo_descarte
         FROM documento
         WHERE estado_url = 'DESCARTADA'
         ORDER BY id_documento DESC"
    );
    $sentenciaPreparada->execute();
    echo json_encode($sentenciaPreparada->fetchAll(PDO::FETCH_ASSOC));
} catch (\PDOException $excepcion) {
    http_response_code(500);
    echo json_encode(["error" => $excepcion->getMessage()]);
}