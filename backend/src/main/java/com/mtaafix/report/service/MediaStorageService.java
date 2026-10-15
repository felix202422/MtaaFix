package com.mtaafix.report.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local-disk media storage. This is an explicit Phase 7 placeholder: real
 * object storage, CDN delivery and content scanning arrive with Phase 13.
 * Files live under {@code media.storage-dir} (default {@code ./uploads}) and
 * are served through the authenticated {@code GET /media/{id}/content} route
 * (FR-9.3) — never as unrestricted public links (FR-9.5).
 */
@Service
public class MediaStorageService {

    private final Path root;

    public MediaStorageService(@Value("${media.storage-dir:./uploads}") String storageDir)
            throws IOException {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public StoredObject store(MultipartFile file, String contentType) throws IOException {
        String extension = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String storedName = UUID.randomUUID() + extension;
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Resolved storage path escapes the media root.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredObject(storedName, target, file.getSize());
    }

    public Path resolve(String storedName) throws IOException {
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root)) {
            throw new IOException("Invalid media file name.");
        }
        return path;
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException ignored) {
            // Best-effort cleanup; the metadata row is already removed.
        }
    }

    public record StoredObject(String storedName, Path path, long sizeBytes) {
    }
}
