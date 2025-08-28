package ru.jumpie.JumpieClient.api.controllers;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.base-dir:/uploads}")
    private String baseDir;

    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(@RequestParam String filePath) {
        try {
            Path path = Paths.get(baseDir).resolve(filePath).normalize();

            // Security check: ensure the path is within the base directory
            if (!path.startsWith(Paths.get(baseDir).normalize())) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}