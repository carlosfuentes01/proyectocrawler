<?php
require_once 'cabecera.php';

$id = $_GET['id'] ?? null;
if ($id === null || !ctype_digit((string)$id)) {
    http_response_code(400);
    echo json_encode(["error" => "id invalido."]);
    exit;
}

$host = 'base_datos';
$db   = 'taller1_db';
$user = 'root';
$pass = 'root_password';

try {
    $pdo = new PDO("mysql:host=$host;dbname=$db;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION
    ]);
    $stmt = $pdo->prepare("SELECT id, numeroa, numerob, resultado, estado FROM test WHERE id = ?");
    $stmt->execute([$id]);
    $fila = $stmt->fetch(PDO::FETCH_ASSOC);

    if (!$fila) {
        http_response_code(404);
        echo json_encode(["error" => "Registro no encontrado."]);
        exit;
    }

    header('Content-Type: application/json');
    echo json_encode($fila);
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(["error" => "Error BD: " . $e->getMessage()]);
}