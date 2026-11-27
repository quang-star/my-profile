package dev.quang.portfolio.controller;

import dev.quang.portfolio.dto.MultiTenantDtos.MyFilesResponse;
import dev.quang.portfolio.dto.MultiTenantDtos.UploadedFileDto;
import dev.quang.portfolio.entity.ContentNodeEntity;
import dev.quang.portfolio.entity.PageEntity;
import dev.quang.portfolio.entity.UploadedFileEntity;
import dev.quang.portfolio.entity.UserEntity;
import dev.quang.portfolio.repository.ContentNodeRepository;
import dev.quang.portfolio.repository.PageRepository;
import dev.quang.portfolio.repository.UploadedFileRepository;
import dev.quang.portfolio.repository.UserRepository;
import dev.quang.portfolio.security.AuthenticatedUser;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
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
            "application/pdf"
    );

    private final UploadedFileRepository uploadedFileRepository;
    private final UserRepository userRepository;
    private final PageRepository pageRepository;
    private final ContentNodeRepository contentNodeRepository;

    public FileController(
            UploadedFileRepository uploadedFileRepository,
            UserRepository userRepository,
            PageRepository pageRepository,
            ContentNodeRepository contentNodeRepository
    ) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.userRepository = userRepository;
        this.pageRepository = pageRepository;
        this.contentNodeRepository = contentNodeRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadUserFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return handleUpload(AuthenticatedUser.require(authentication), file);
    }

    @PostMapping("/admin/upload")
    public ResponseEntity<Map<String, Object>> uploadAdminFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        return handleUpload(AuthenticatedUser.require(authentication), file);
    }

    private ResponseEntity<Map<String, Object>> handleUpload(
            AuthenticatedUser currentUser,
            MultipartFile file
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File size exceeds 10MB limit");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        if (originalName.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SVG files are not allowed");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + contentType);
        }

        validateMagicBytes(file, contentType.toLowerCase(Locale.ROOT));

        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole()) && user.getMaxStorageBytes() >= 0) {
            long usedBytes = uploadedFileRepository.sumSizeBytesByOwnerId(user.getId());
            if (usedBytes + file.getSize() > user.getMaxStorageBytes()) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Storage quota exceeded");
            }
        }

        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        UploadedFileEntity entity = new UploadedFileEntity();
        entity.setId(id);
        entity.setOwnerId(user.getId());
        entity.setFilename(safeName);
        entity.setContentType(contentType);
        entity.setSizeBytes(file.getSize());
        entity.setData(file.getBytes());
        entity.setCreatedAt(Instant.now());
        uploadedFileRepository.save(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", id,
                "url", "/api/files/" + id,
                "filename", safeName,
                "contentType", contentType,
                "sizeBytes", file.getSize()
        ));
    }

    private void validateMagicBytes(MultipartFile file, String normalizedContentType) throws IOException {
        byte[] header;
        try (InputStream is = file.getInputStream()) {
            header = is.readNBytes(12);
        }
        boolean matches = switch (normalizedContentType) {
            case "image/png" -> isPng(header);
            case "image/jpeg", "image/jpg" -> isJpeg(header);
            case "image/gif" -> isGif(header);
            case "image/webp" -> isWebp(header);
            case "application/pdf" -> isPdf(header);
            default -> false;
        };
        if (!matches) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File content does not match declared type: " + normalizedContentType
            );
        }
    }

    private static boolean isPng(byte[] b) {
        return b.length >= 4
                && (b[0] & 0xFF) == 0x89
                && b[1] == 0x50
                && b[2] == 0x4E
                && b[3] == 0x47;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isGif(byte[] b) {
        return b.length >= 4
                && b[0] == 'G'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == '8';
    }

    private static boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P';
    }

    private static boolean isPdf(byte[] b) {
        return b.length >= 5
                && b[0] == '%'
                && b[1] == 'P'
                && b[2] == 'D'
                && b[3] == 'F'
                && b[4] == '-';
    }

    @GetMapping("/my/files")
    @Transactional(readOnly = true)
    public MyFilesResponse listMyFiles(Authentication authentication) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UserEntity user = userRepository.findById(current.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        List<String> referenceBlobs = collectOwnerReferenceBlobs(user);

        List<UploadedFileDto> files = uploadedFileRepository
                .findSummariesByOwnerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(f -> {
                    String url = "/api/files/" + f.getId();
                    int usageCount = countUrlReferences(referenceBlobs, url);
                    return new UploadedFileDto(
                            f.getId(),
                            url,
                            f.getFilename(),
                            f.getContentType(),
                            f.getSizeBytes(),
                            usageCount,
                            f.getCreatedAt()
                    );
                })
                .toList();
        long used = uploadedFileRepository.sumSizeBytesByOwnerId(user.getId());
        return new MyFilesResponse(files, used, user.getMaxStorageBytes());
    }

    private List<String> collectOwnerReferenceBlobs(UserEntity user) {
        List<String> blobs = new ArrayList<>();
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            blobs.add(user.getAvatarUrl());
        }
        List<PageEntity> pages = pageRepository.findByOwnerIdOrderByUpdatedAtDesc(user.getId());
        for (PageEntity page : pages) {
            if (page.getCvPdfUrl() != null && !page.getCvPdfUrl().isBlank()) {
                blobs.add(page.getCvPdfUrl());
            }
            if (page.getThemeJson() != null && !page.getThemeJson().isBlank()) {
                blobs.add(page.getThemeJson());
            }
            List<ContentNodeEntity> nodes = contentNodeRepository.findByPageIdOrderBySortOrderAscIdAsc(page.getId());
            for (ContentNodeEntity node : nodes) {
                if (node.getAttrsJson() != null && !node.getAttrsJson().isBlank()) {
                    blobs.add(node.getAttrsJson());
                }
                if (node.getContentVi() != null && !node.getContentVi().isBlank()) {
                    blobs.add(node.getContentVi());
                }
                if (node.getContentEn() != null && !node.getContentEn().isBlank()) {
                    blobs.add(node.getContentEn());
                }
            }
        }
        return blobs;
    }

    private static int countUrlReferences(List<String> blobs, String url) {
        int count = 0;
        for (String blob : blobs) {
            if (blob.contains(url)) {
                count++;
            }
        }
        return count;
    }

    @DeleteMapping("/files/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(Authentication authentication, @PathVariable String id) {
        AuthenticatedUser current = AuthenticatedUser.require(authentication);
        UploadedFileEntity entity = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("File not found"));
        if (!current.isSuperAdmin() && !current.id().equals(entity.getOwnerId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this file");
        }
        uploadedFileRepository.delete(entity);
    }

    @GetMapping("/files/{id}")
    public ResponseEntity<byte[]> download(
            @PathVariable String id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        UploadedFileEntity entity = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

        String eTag = "\"file-" + id + "\"";
        CacheControl cacheControl = CacheControl.maxAge(java.time.Duration.ofDays(365)).cachePublic();
        if (matchesETag(ifNoneMatch, eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(eTag)
                    .cacheControl(cacheControl)
                    .build();
        }

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(entity.getContentType());
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .eTag(eTag)
                .contentType(mediaType)
                .cacheControl(cacheControl)
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entity.getFilename() + "\"")
                .body(entity.getData());
    }

    private static boolean matchesETag(String ifNoneMatch, String eTag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        for (String candidate : ifNoneMatch.split(",")) {
            String trimmed = candidate.trim();
            if ("*".equals(trimmed) || eTag.equals(trimmed) || ("W/" + eTag).equals(trimmed)) {
                return true;
            }
        }
        return false;
    }
}
