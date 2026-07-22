package com.project.assignment.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class AttachmentStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp",
            ".pdf", ".txt", ".doc", ".docx", ".xls", ".xlsx",
            ".ppt", ".pptx", ".zip", ".csv");

    private final Path root;

    public AttachmentStorageService(@Value("${app.submissions.dir:./data/submissions}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(root);
    }

    public StoredFile store(UUID submissionId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String original = sanitizeOriginalName(file.getOriginalFilename());
        String ext = extensionOf(original);
        if (!ALLOWED_EXT.contains(ext.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Use an image (jpg, png, gif, webp) or common document (pdf, doc, zip, …)");
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            String ct = contentType.toLowerCase(Locale.ROOT);
            if (ct.contains("javascript") || ct.contains("html") || ct.equals("application/x-msdownload")) {
                throw new IllegalArgumentException("This file type is not allowed");
            }
        }
        String storedName = submissionId + ext;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Invalid attachment path");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        String resolvedType = contentType != null && !contentType.isBlank()
                ? contentType
                : guessContentType(ext);
        return new StoredFile(storedName, original, resolvedType, file.getSize());
    }

    public Resource load(String storedName) {
        Path path = resolveSafe(storedName);
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Attachment file not found");
        }
        return new FileSystemResource(path);
    }

    public void deleteIfPresent(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafe(storedName));
        } catch (IOException | IllegalArgumentException ignored) {
            // best-effort
        }
    }

    private Path resolveSafe(String storedName) {
        if (storedName == null || storedName.isBlank() || storedName.contains("..")
                || storedName.contains("/") || storedName.contains("\\")) {
            throw new IllegalArgumentException("Invalid attachment name");
        }
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid attachment path");
        }
        return path;
    }

    private static String sanitizeOriginalName(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        String base = name.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        if (slash >= 0) {
            base = base.substring(slash + 1);
        }
        base = base.replaceAll("[^a-zA-Z0-9._\\- ]", "_").trim();
        return base.isBlank() ? "upload.bin" : base.substring(0, Math.min(base.length(), 180));
    }

    private static String extensionOf(String original) {
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return ".bin";
        }
        String ext = original.substring(dot);
        return ext.length() > 10 ? ".bin" : ext;
    }

    private static String guessContentType(String ext) {
        return switch (ext.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".pdf" -> "application/pdf";
            case ".txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    public record StoredFile(String storedName, String originalName, String contentType, long bytes) {
    }
}
