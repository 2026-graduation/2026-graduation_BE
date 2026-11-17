package com.ssambbong.gymjjak.file.infrastructure.local;

import com.ssambbong.gymjjak.file.domain.policy.FilePolicy;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 로컬 저장소 모드에서 S3 presigned URL 역할을 대신하는 업로드/조회 엔드포인트.
 * - PUT: 프론트가 presigned URL 로 보내던 요청을 그대로 받아 디스크에 저장
 * - GET: 저장된 파일 반환 (Range 요청 지원 → 영상 탐색 가능)
 *
 * 인증 없이 열려 있으므로 로컬 개발 전용이다. (key 에 UUID 가 포함되어 추측이 어렵다)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Arrays.stream(FilePolicy.values())
            .flatMap(p -> p.getAllowedMimeTypes().stream())
            .collect(Collectors.toUnmodifiableSet());

    private final LocalFileStorageAdapter storage;

    @PutMapping(LocalFileStorageAdapter.URL_PREFIX + "**")
    public ResponseEntity<Void> upload(HttpServletRequest request) throws IOException {
        String key = extractKey(request);
        String contentType = request.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(415).build();
        }
        try {
            storage.save(key, request.getInputStream(), contentType);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.warn("로컬 업로드 실패 - key: {}, reason: {}", key, e.getMessage());
            return ResponseEntity.status(413).build();
        }
        log.info("로컬 업로드 완료 - key: {}", key);
        return ResponseEntity.ok().build();
    }

    @GetMapping(LocalFileStorageAdapter.URL_PREFIX + "**")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String key = extractKey(request);
        try {
            if (!storage.exists(key)) {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(storage.readContentType(key)))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(new FileSystemResource(storage.path(key)));
    }

    private String extractKey(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        String path = uri.substring(context.length());
        return path.substring(LocalFileStorageAdapter.URL_PREFIX.length());
    }
}
