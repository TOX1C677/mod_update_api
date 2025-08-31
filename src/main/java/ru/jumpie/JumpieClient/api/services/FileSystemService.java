package ru.jumpie.JumpieClient.api.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileSystemService {

    public List<Path> listFilesInDirectory(String directoryPath) throws IOException {
        List<Path> items = new ArrayList<>();
        Path dir = Paths.get(directoryPath);

        System.out.println("Listing directory: " + dir.toAbsolutePath());

        if (!Files.exists(dir)) {
            System.out.println("Directory does not exist: " + dir);
            return items;
        }

        if (!Files.isDirectory(dir)) {
            System.out.println("Path is not a directory: " + dir);
            return items;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                items.add(path);
                System.out.println("Found: " + path.getFileName());
            }
        } catch (IOException e) {
            System.err.println("Error listing directory: " + e.getMessage());
            throw e;
        }

        return items;
    }
}