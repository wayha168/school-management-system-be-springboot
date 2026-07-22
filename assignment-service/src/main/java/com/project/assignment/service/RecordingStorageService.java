package com.project.assignment.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class RecordingStorageService {

    private final Path root;

    public RecordingStorageService(@Value("${app.recordings.dir:./data/recordings}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public String store(UUID meetingId, MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = ".webm";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
            if (ext.length() > 10) {
                ext = ".webm";
            }
        }
        String storedName = meetingId + ext;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid recording path");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    public Resource load(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("..") || storedName.contains("/")
                || storedName.contains("\\")) {
            throw new IllegalArgumentException("Invalid recording name");
        }
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Recording file not found");
        }
        return new FileSystemResource(path);
    }

    public void deleteIfPresent(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Path path = root.resolve(storedName).normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
