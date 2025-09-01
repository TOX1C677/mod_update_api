package ru.jumpie.JumpieClient.api.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            // Декодируем URL-encoded путь
            String decodedFilePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8.toString());
            Path basePath = Paths.get("/home/tox1c/jumpie-files");
            Path path = basePath.resolve(decodedFilePath).normalize();

            // Security check: ensure the path is within the base directory
            if (!path.startsWith(basePath.normalize())) {
                return ResponseEntity.badRequest().build();
            }

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

            // Кодируем имя файла для поддержки русских символов
            String filename = resource.getFilename();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + encodedFilename + "\"; filename*=utf-8''" + encodedFilename)
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Error serving file: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}