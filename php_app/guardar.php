<?php
require_once 'cabecera.php';
require_once 'conexion.php';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $datos = json_decode(file_get_contents("php://input"), true);

    if (empty($datos['nombre']) || empty($datos['pais'])) {
        http_response_code(400);
        echo json_encode(["error" => "nombre y pais son obligatorios."]);
        exit;
    }

    try {
        $stmt = $conexion->prepare(
            "INSERT INTO persona (nombre, apellido, pais, ciudad, profesion, empresa, alias, palabra_clave)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        );
        $stmt->execute([
            $datos['nombre'],
            $datos['apellido'] ?? null,
            $datos['pais'],
            $datos['ciudad'] ?? null,
            $datos['profesion'] ?? null,
            $datos['empresa'] ?? null,
            $datos['alias'] ?? null,
            $datos['palabra_clave'] ?? null
        ]);
        echo json_encode(["message" => "¡Persona guardada con éxito en MySQL!", "id" => $conexion->lastInsertId()]);
    } catch (\PDOException $e) {
        http_response_code(500);
        echo json_encode(["error" => "Fallo al escribir en la BD: " . $e->getMessage()]);
    }
}