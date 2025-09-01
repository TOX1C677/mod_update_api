package ru.jumpie.JumpieClient.api.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class FileController {

//    @Value("${file.base-dir}")
//    private String baseDir;
    //я устал с ним бороться, потом поменяю
    private String baseDir = "/home/tox1c/jumpie-files";

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        try {
            // Используем правильный базовый путь
            Path basePath = Paths.get("/home/tox1c/jumpie-files");
            Path path = basePath.resolve(filePath).normalize();

            // Security check: ensure the path is within the base directory
            if (!path.startsWith(basePath.normalize())) {
                return ResponseEntity.badRequest().build();
            }

            // Проверяем существование файла
            if (!Files.exists(path)) {
                System.out.println("File not found: " + path);
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Для больших файлов используем потоковую передачу
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Error serving file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}