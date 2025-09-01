package ru.jumpie.JumpieClient.api.controllers;

import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final FileSystemService fileSystemService;

    // Жестко заданный путь
    private static final String BASE_DIR = "/home/tox1c/jumpie-files";
    private Map<String, Resource> resourcesMap = new HashMap<>();

    @GetMapping("/resources")
    public List<Resource> getAvailableResources() {
        initializeResources();
        return new ArrayList<>(resourcesMap.values());
    }

    @PostMapping("/download")
    public ResponseEntity<DownloadResponse> prepareDownload(@RequestBody DownloadRequest request) {
        try {
            Resource requestedResource = resourcesMap.get(request.getResourceId());
            if (requestedResource == null) {
                return ResponseEntity.notFound().build();
            }

            DownloadResponse response = new DownloadResponse();
            response.setResourceId(request.getResourceId());
            response.setFilesToDownload(new ArrayList<>());

            if ("file".equals(requestedResource.getType())) {
                processFileDownload(request, requestedResource, response);
            } else if ("folder".equals(requestedResource.getType())) {
                processFolderDownload(request, requestedResource, response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void initializeResources() {
        resourcesMap.clear();

        try {
            System.out.println("Scanning directory: " + BASE_DIR);

            // Сканируем базовую директорию
            List<Path> items = fileSystemService.listFilesInDirectory(BASE_DIR);

            for (Path item : items) {
                Resource resource = new Resource();
                resource.setId("item_" + UUID.randomUUID().toString());
                resource.setName(item.getFileName().toString());
                resource.setPath(item.toString());

                if (Files.isDirectory(item)) {
                    resource.setType("folder");
                    resource.setSize(0);
                } else {
                    resource.setType("file");
                    resource.setSize(fileSystemService.getFileSize(item));
                }

                resourcesMap.put(resource.getId(), resource);
                System.out.println("Found: " + resource.getName() + " (" + resource.getType() + ")");
            }

        } catch (IOException e) {
            System.err.println("Error scanning directory: " + e.getMessage());
            createTestResources();
        }
    }

    private void createTestResources() {
        System.out.println("Using test resources");

        Resource file1 = new Resource();
        file1.setId("file_1");
        file1.setName("Test Document.pdf");
        file1.setType("file");
        file1.setPath(BASE_DIR + "/test.pdf");
        file1.setSize(1024 * 1024);
        resourcesMap.put(file1.getId(), file1);

        Resource folder1 = new Resource();
        folder1.setId("folder_1");
        folder1.setName("Test Project");
        folder1.setType("folder");
        folder1.setPath(BASE_DIR + "/test-project");
        folder1.setSize(0);
        resourcesMap.put(folder1.getId(), folder1);
    }

    private void processFileDownload(DownloadRequest request, Resource resource, DownloadResponse response) throws IOException {
        Path filePath = Paths.get(resource.getPath());

        if (!Files.exists(filePath)) {
            System.out.println("File not found: " + filePath);
            return;
        }

        boolean needToDownload = true;
        if (request.getClientFiles() != null && !request.getClientFiles().isEmpty()) {
            String currentHash = fileSystemService.calculateFileHash(filePath);
            for (DownloadRequest.ClientFileInfo clientFile : request.getClientFiles()) {
                if (clientFile.getHash().equals(currentHash)) {
                    needToDownload = false;
                    break;
                }
            }
        }

        if (needToDownload) {
            DownloadResponse.FileToDownload fileToDownload = new DownloadResponse.FileToDownload();
            // Отправляем только имя файла, а не полный путь
            fileToDownload.setServerPath(Paths.get(resource.getPath()).getFileName().toString());
            fileToDownload.setRelativePath("");
            fileToDownload.setSize(fileSystemService.getFileSize(filePath));
            fileToDownload.setHash(fileSystemService.calculateFileHash(filePath));
            response.getFilesToDownload().add(fileToDownload);

            System.out.println("File to download: " + fileToDownload.getServerPath());
        }
    }

    private void processFolderDownload(DownloadRequest request, Resource resource, DownloadResponse response) throws IOException {
        Path basePath = Paths.get(resource.getPath());

        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            System.out.println("Folder not found: " + basePath);
            return;
        }

        // Рекурсивно получаем все файлы в папке
        List<Path> serverFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(basePath)) {
            serverFiles = walk.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }

        for (Path serverFile : serverFiles) {
            String relativePath = basePath.relativize(serverFile).toString();
            String serverFileHash = fileSystemService.calculateFileHash(serverFile);

            boolean needToDownload = true;
            if (request.getClientFiles() != null) {
                for (DownloadRequest.ClientFileInfo clientFile : request.getClientFiles()) {
                    if (clientFile.getRelativePath().equals(relativePath) &&
                            clientFile.getHash().equals(serverFileHash)) {
                        needToDownload = false;
                        break;
                    }
                }
            }

            if (needToDownload) {
                DownloadResponse.FileToDownload fileToDownload = new DownloadResponse.FileToDownload();
                // Отправляем полный путь от базовой директории сервера
                Path serverBasePath = Paths.get(BASE_DIR);
                String serverRelativePath = serverBasePath.relativize(serverFile).toString();

                fileToDownload.setServerPath(serverRelativePath);
                fileToDownload.setRelativePath(relativePath);
                fileToDownload.setSize(fileSystemService.getFileSize(serverFile));
                fileToDownload.setHash(serverFileHash);
                response.getFilesToDownload().add(fileToDownload);

                System.out.println("Folder file to download: " + fileToDownload.getServerPath());
            }
        }
    }

    @GetMapping("/debug")
    public String debug() {
        Path basePath = Paths.get(BASE_DIR);
        Path currentPath = Paths.get(".");

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("Base dir: ").append(BASE_DIR).append("\n");
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
                try (var paths = Files.list(basePath)) {
                    paths.forEach(path -> debugInfo.append("  - ").append(path.getFileName()).append("\n"));
                }
            }
        } catch (IOException e) {
            debugInfo.append("Error listing directory: ").append(e.getMessage()).append("\n");
        }

        return debugInfo.toString();
    }
}