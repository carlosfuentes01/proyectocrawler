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
    // Conteos por estado para esa persona/fuente
    $sql = "SELECT
                COUNT(*) AS total_documentos,
                SUM(estado_url = 'PROCESADA')        AS procesadas,
                SUM(estado_url = 'DESCARTADA')       AS descartadas,
                SUM(estado_url = 'ERROR')            AS errores,
                SUM(estado_url = 'PENDIENTE')        AS pendientes,
                SUM(estado_url = 'EN_PROCESAMIENTO') AS en_proceso
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

    // Duracion de la ultima ejecucion del crawler
    $duracionUltimaEjecucionMs = null;
    $fechaUltimaEjecucion = null;
    $workersUltimaEjecucion = null;
    try {
        $stmtMetrica = $conexion->query(
            "SELECT duracion_ms, fecha, num_workers
             FROM metrica_concurrencia
             ORDER BY id_metrica DESC LIMIT 1"
        );
        $metrica = $stmtMetrica->fetch(PDO::FETCH_ASSOC);
        if ($metrica) {
            $duracionUltimaEjecucionMs = (int) $metrica['duracion_ms'];
            $fechaUltimaEjecucion = $metrica['fecha'];
            $workersUltimaEjecucion = (int) $metrica['num_workers'];
        }
    } catch (Throwable $e) {
        // sin metricas aun
    }

    header('Content-Type: application/json');
    echo json_encode([
        "totalDocumentos" => (int) ($fila['total_documentos'] ?? 0),
        "procesadas"      => (int) ($fila['procesadas']      ?? 0),
        "descartadas"     => (int) ($fila['descartadas']     ?? 0),
        "errores"         => (int) ($fila['errores']         ?? 0),
        "pendientes"      => (int) ($fila['pendientes']      ?? 0),
        "enProceso"       => (int) ($fila['en_proceso']      ?? 0),
        "duracionUltimaEjecucionMs" => $duracionUltimaEjecucionMs,
        "fechaUltimaEjecucion"      => $fechaUltimaEjecucion,
        "workersUltimaEjecucion"    => $workersUltimaEjecucion
    ]);
} catch (Throwable $excepcion) {
    http_response_code(500);
    header('Content-Type: application/json');
    echo json_encode([
        "error" => "Error al consultar metricas",
        "detalle" => $excepcion->getMessage()
    ]);
}