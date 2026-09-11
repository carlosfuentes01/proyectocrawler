<?php
require_once 'cabecera.php';
require_once 'conexion.php';

try {
    $stmt = $conexion->query("SELECT id, numeroa, numerob, resultado, estado FROM test ORDER BY id DESC");
    echo json_encode($stmt->fetchAll(PDO::FETCH_ASSOC));
} catch (\PDOException $e) {
    http_response_code(500);
    echo json_encode(["error" => $e->getMessage()]);
}