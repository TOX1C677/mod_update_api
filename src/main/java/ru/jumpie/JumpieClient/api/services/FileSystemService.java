package ru.jumpie.JumpieClient.api.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class FileSystemService {

    public List<Path> listFilesInDirectory(String directoryPath) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile).forEach(files::add);
        }
        return files;
    }

    public String calculateFileHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IOException("Error calculating hash for file: " + filePath, e);
        }
    }

    public long getFileSize(Path filePath) throws IOException {
        return Files.size(filePath);
    }
}