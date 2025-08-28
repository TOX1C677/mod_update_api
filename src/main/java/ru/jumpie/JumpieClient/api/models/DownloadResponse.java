package ru.jumpie.JumpieClient.api.models;

import lombok.Data;
import java.util.List;

@Data
public class DownloadResponse {
    private String resourceId;
    private List<FileToDownload> filesToDownload;

    @Data
    public static class FileToDownload {
        private String serverPath;
        private String relativePath;
        private long size;
        private String hash;
    }
}