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

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ResourceController {

    private final FileSystemService fileSystemService;

    @Value("${file.base-dir:/uploads}")
    private String baseDir;

    private Map<String, Resource> resourcesMap = new HashMap<>();

    @GetMapping("/resources")
    public List<Resource> getAvailableResources() {
        // Если resourcesMap пустой, инициализируем его
        if (resourcesMap.isEmpty()) {
            initializeResources();
        }
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
        try {
            // Сканируем базовую директорию и создаем ресурсы
            List<Path> files = fileSystemService.listFilesInDirectory(baseDir);

            for (Path file : files) {
                Resource resource = new Resource();
                resource.setId("file_" + UUID.randomUUID());
                resource.setName(file.getFileName().toString());
                resource.setType("file");
                resource.setPath(file.toString());
                resource.setSize(fileSystemService.getFileSize(file));
                resourcesMap.put(resource.getId(), resource);
            }

            // Можно добавить логику для сканирования папок
            Resource testFolder = new Resource();
            testFolder.setId("folder_test");
            testFolder.setName("Test Folder");
            testFolder.setType("folder");
            testFolder.setPath(baseDir + "/test-folder");
            testFolder.setSize(0);
            resourcesMap.put(testFolder.getId(), testFolder);

        } catch (IOException e) {
            // Fallback to test data if directory scanning fails
            createTestResources();
        }
    }

    private void createTestResources() {
        Resource file1 = new Resource();
        file1.setId("file_1");
        file1.setName("Test Document.pdf");
        file1.setType("file");
        file1.setPath(baseDir + "/test.pdf");
        file1.setSize(1024 * 1024);
        resourcesMap.put(file1.getId(), file1);

        Resource folder1 = new Resource();
        folder1.setId("folder_1");
        folder1.setName("Test Project");
        folder1.setType("folder");
        folder1.setPath(baseDir + "/test-project");
        folder1.setSize(0);
        resourcesMap.put(folder1.getId(), folder1);
    }

    private void processFileDownload(DownloadRequest request, Resource resource, DownloadResponse response) throws IOException {
        Path filePath = Paths.get(resource.getPath());

        if (!Files.exists(filePath)) {
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
            fileToDownload.setServerPath(resource.getPath());
            fileToDownload.setRelativePath("");
            fileToDownload.setSize(fileSystemService.getFileSize(filePath));
            fileToDownload.setHash(fileSystemService.calculateFileHash(filePath));
            response.getFilesToDownload().add(fileToDownload);
        }
    }

    private void processFolderDownload(DownloadRequest request, Resource resource, DownloadResponse response) throws IOException {
        Path basePath = Paths.get(resource.getPath());

        if (!Files.exists(basePath) || !Files.isDirectory(basePath)) {
            return;
        }

        List<Path> serverFiles = fileSystemService.listFilesInDirectory(resource.getPath());

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
                fileToDownload.setServerPath(serverFile.toString());
                fileToDownload.setRelativePath(relativePath);
                fileToDownload.setSize(fileSystemService.getFileSize(serverFile));
                fileToDownload.setHash(serverFileHash);
                response.getFilesToDownload().add(fileToDownload);
            }
        }
    }
}