package ru.jumpie.JumpieClient.api.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resource {
    private String id;
    private String name; // "Файл 1", "Папка с проектом"
    private String type; // "file", "folder"
    private String path; // Путь на сервере к ресурсу
    private long size; // Размер в байтах (для прогресса)
    // + getters/setters
}