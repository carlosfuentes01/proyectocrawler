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
    $sql = "SELECT
                COUNT(*) AS total,
                SUM(estado_url = 'PENDIENTE')         AS pendientes,
                SUM(estado_url = 'EN_PROCESAMIENTO')  AS en_proceso,
                SUM(estado_url = 'PROCESADA')         AS procesadas,
                SUM(estado_url = 'DESCARTADA')        AS descartadas,
                SUM(estado_url = 'ERROR')             AS errores
            FROM documento
            WHERE 1 = 1";

    $parametros = [];
    if ($idPersonaFiltro !== null && ctype_digit((string)$idPersonaFiltro)) {
        $sql .= " AND id_persona = ?";
        $parametros[] = (int) $idPersonaFiltro;
    }
    if ($idFuenteFiltro !== null && ctype_digit((string)$idFuenteFiltro)) {
        $sql .= " AND id_fuente = ?";
        $parametros[] = (int) $idFuenteFiltro;
    }

    $sentenciaPreparada = $conexion->prepare($sql);
    $sentenciaPreparada->execute($parametros);
    $fila = $sentenciaPreparada->fetch(PDO::FETCH_ASSOC);

    $progreso = [
        "total"       => (int) ($fila['total']       ?? 0),
        "pendientes"  => (int) ($fila['pendientes']  ?? 0),
        "enProceso"   => (int) ($fila['en_proceso']  ?? 0),
        "procesadas"  => (int) ($fila['procesadas']  ?? 0),
        "descartadas" => (int) ($fila['descartadas'] ?? 0),
        "errores"     => (int) ($fila['errores']     ?? 0),
    ];

    $progreso["enCurso"] = ($progreso["pendientes"] > 0) || ($progreso["enProceso"] > 0);

    header('Content-Type: application/json');
    echo json_encode($progreso);
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al consultar progreso",
        "detalle" => $excepcion->getMessage()
    ]);
}