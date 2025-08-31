package ru.jumpie.JumpieClient.api.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.jumpie.JumpieClient.api.models.DownloadRequest;
import ru.jumpie.JumpieClient.api.models.DownloadResponse;
import ru.jumpie.JumpieClient.api.models.Resource;
import ru.jumpie.JumpieClient.api.services.FileSystemService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.IOException;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final FileSystemService fileSystemService;

    @Value("${file.base-dir}")
    private String baseDir;

    @GetMapping("/resources")
    public List<Resource> getAvailableResources() {
        List<Resource> resources = new ArrayList<>();

        try {
            System.out.println("BASE DIR: " + baseDir);
            System.out.println("Absolute path: " + Paths.get(baseDir).toAbsolutePath());

            // Проверяем существование директории
            Path basePath = Paths.get(baseDir);
            if (!Files.exists(basePath)) {
                System.out.println("Directory does not exist: " + basePath);
                Files.createDirectories(basePath);
                System.out.println("Created directory: " + basePath);
                return getTestResources(); // Возвращаем тестовые данные если директория пустая
            }

            if (!Files.isDirectory(basePath)) {
                System.out.println("Path is not a directory: " + basePath);
                return getTestResources();
            }

            // Сканируем директорию
            List<Path> items = fileSystemService.listFilesInDirectory(baseDir);
            System.out.println("Found " + items.size() + " items in directory");

            for (Path item : items) {
                Resource resource = new Resource();
                resource.setId("item_" + UUID.randomUUID().toString());
                resource.setName(item.getFileName().toString());
                resource.setPath(item.toAbsolutePath().toString());

                if (Files.isDirectory(item)) {
                    resource.setType("folder");
                    resource.setSize(0);
                    System.out.println("Folder: " + resource.getName());
                } else {
                    resource.setType("file");
                    try {
                        resource.setSize(Files.size(item));
                        System.out.println("File: " + resource.getName() + " (" + resource.getSize() + " bytes)");
                    } catch (IOException e) {
                        resource.setSize(0);
                        System.out.println("File: " + resource.getName() + " (error getting size)");
                    }
                }

                resources.add(resource);
            }

            // Если директория пустая, возвращаем тестовые данные
            if (resources.isEmpty()) {
                System.out.println("Directory is empty, returning test resources");
                return getTestResources();
            }

        } catch (Exception e) {
            System.err.println("Error scanning directory: " + e.getMessage());
            e.printStackTrace();
            return getTestResources();
        }

        return resources;
    }

    private List<Resource> getTestResources() {
        List<Resource> testResources = new ArrayList<>();

        // Тестовый файл
        Resource file1 = new Resource();
        file1.setId("test_file_1");
        file1.setName("TestFile.txt");
        file1.setType("file");
        file1.setPath(baseDir + "/TestFile.txt");
        file1.setSize(1024);
        testResources.add(file1);

        // Тестовая папка
        Resource folder1 = new Resource();
        folder1.setId("test_folder_1");
        folder1.setName("TestFolder");
        folder1.setType("folder");
        folder1.setPath(baseDir + "/TestFolder");
        folder1.setSize(0);
        testResources.add(folder1);

        System.out.println("Returning test resources");
        return testResources;
    }

    @PostMapping("/download")
    public ResponseEntity<DownloadResponse> prepareDownload(@RequestBody DownloadRequest request) {
        try {
            // Для простоты всегда возвращаем тестовый ответ
            DownloadResponse response = new DownloadResponse();
            response.setResourceId(request.getResourceId());
            response.setFilesToDownload(new ArrayList<>());

            // Добавляем тестовый файл для скачивания
            DownloadResponse.FileToDownload fileToDownload = new DownloadResponse.FileToDownload();
            fileToDownload.setServerPath(baseDir + "/test-file.txt");
            fileToDownload.setRelativePath("test-file.txt");
            fileToDownload.setSize(1024);
            fileToDownload.setHash("test-hash-123");
            response.getFilesToDownload().add(fileToDownload);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug")
    public String debug() {
        Path basePath = Paths.get(baseDir);
        Path currentPath = Paths.get(".");

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("Base dir from properties: ").append(baseDir).append("\n");
        debugInfo.append("Absolute base path: ").append(basePath.toAbsolutePath()).append("\n");
        debugInfo.append("Current working dir: ").append(currentPath.toAbsolutePath()).append("\n");
        debugInfo.append("Base path exists: ").append(Files.exists(basePath)).append("\n");
        debugInfo.append("Is directory: ").append(Files.isDirectory(basePath)).append("\n");

        // Попробуем создать директорию
        try {
            Files.createDirectories(basePath);
            debugInfo.append("Directory created successfully\n");
        } catch (IOException e) {
            debugInfo.append("Error creating directory: ").append(e.getMessage()).append("\n");
        }

        // Посмотрим что в директории
        try {
            if (Files.exists(basePath)) {
                debugInfo.append("Contents of ").append(basePath).append(":\n");
                try (Stream<Path> paths = Files.list(basePath)) {
                    paths.forEach(path -> debugInfo.append("  - ").append(path.getFileName()).append("\n"));
                }
            }
        } catch (IOException e) {
            debugInfo.append("Error listing directory: ").append(e.getMessage()).append("\n");
        }

        return debugInfo.toString();
    }
}