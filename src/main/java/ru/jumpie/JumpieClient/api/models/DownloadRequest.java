package ru.jumpie.JumpieClient.api.models;

import lombok.Data;
import java.util.List;

@Data
public class DownloadRequest {
    private String resourceId;
    private List<ClientFileInfo> clientFiles;

    @Data
    public static class ClientFileInfo {
        private String relativePath;
        private String hash;
        private long lastModified;
    }
}