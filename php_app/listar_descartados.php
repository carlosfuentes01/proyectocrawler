<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(["error" => "Solo GET."]);
    exit;
}

$idPersonaFiltro = $_GET['idPersona'] ?? null;
$idFuenteFiltro  = $_GET['idFuente']  ?? null;

try {
    $sql = "SELECT d.id_documento, d.id_persona, d.id_fuente,
                   p.nombre AS nombre_persona, p.apellido AS apellido_persona,
                   f.nombre AS nombre_fuente,
                   d.titulo, d.url, d.pais, d.fecha_consulta, d.motivo_descarte,
                   d.nombre_hilo
            FROM documento d
            LEFT JOIN persona p ON p.id_persona = d.id_persona
            LEFT JOIN fuente  f ON f.id_fuente  = d.id_fuente
            WHERE d.estado_url = 'DESCARTADA'";

    $parametros = [];
    if ($idPersonaFiltro !== null && ctype_digit((string)$idPersonaFiltro)) {
        $sql .= " AND d.id_persona = ?";
        $parametros[] = (int) $idPersonaFiltro;
    }
    if ($idFuenteFiltro !== null && ctype_digit((string)$idFuenteFiltro)) {
        $sql .= " AND d.id_fuente = ?";
        $parametros[] = (int) $idFuenteFiltro;
    }

    $sql .= " ORDER BY d.id_documento DESC";

    $sentenciaPreparada = $conexion->prepare($sql);
    $sentenciaPreparada->execute($parametros);

    header('Content-Type: application/json');
    echo json_encode($sentenciaPreparada->fetchAll(PDO::FETCH_ASSOC));
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al listar descartados",
        "detalle" => $excepcion->getMessage()
    ]);
}