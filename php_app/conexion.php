<?php
$host = 'base_datos';
$db   = 'taller1_db';
$user = 'root';
$pass = 'root_password';

try {
    $conexion = new PDO("mysql:host=$host;dbname=$db;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);


} catch (\PDOException $e) {
    http_response_code(500);
    echo json_encode(["error" => "Error de conexión BD: " . $e->getMessage()]);
    exit;
}
?>