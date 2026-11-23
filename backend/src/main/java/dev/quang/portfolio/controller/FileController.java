package dev.quang.portfolio.controller;

import dev.quang.portfolio.entity.UploadedFileEntity;
import dev.quang.portfolio.repository.UploadedFileRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class FileController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif",
            "image/svg+xml",
            "application/pdf"
    );

    private final UploadedFileRepository uploadedFileRepository;

    public FileController(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @PostMapping("/admin/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + contentType);
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        UploadedFileEntity entity = new UploadedFileEntity();
        entity.setId(id);
        entity.setFilename(safeName);
        entity.setContentType(contentType);
        entity.setData(file.getBytes());
        entity.setCreatedAt(Instant.now());
        uploadedFileRepository.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", id,
                "url", "/api/files/" + id,
                "filename", safeName,
                "contentType", contentType
        ));
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        UploadedFileEntity entity = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(entity.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entity.getFilename() + "\"")
                .body(entity.getData());
    }
}
