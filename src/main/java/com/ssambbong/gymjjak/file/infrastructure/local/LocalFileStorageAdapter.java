package com.ssambbong.gymjjak.file.infrastructure.local;

import com.ssambbong.gymjjak.file.domain.repository.FileStoragePort;
import com.ssambbong.gymjjak.file.exception.FileUploadException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * S3 대신 서버 로컬 디스크에 파일을 저장하는 어댑터.
 * 업로드/조회 URL은 {@link LocalFileController} 의 엔드포인트를 가리킨다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class LocalFileStorageAdapter implements FileStoragePort {

    public static final String URL_PREFIX = "/api/files/local/";
    private static final String CONTENT_TYPE_SUFFIX = ".contenttype";

    private final LocalStorageProperties properties;
    private final Path root;

    public LocalFileStorageAdapter(LocalStorageProperties properties) {
        this.properties = properties;
        this.root = Path.of(properties.getBaseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("로컬 저장소 디렉터리를 만들 수 없습니다: " + root, e);
        }
        log.info("로컬 파일 저장소 사용 - dir: {}, baseUrl: {}", root, properties.getBaseUrl());
    }

    // ---- FileStoragePort ----

    @Override
    public String generatePresignedUploadUrl(String key, String contentType) {
        return url(key);
    }

    @Override
    public String getPublicUrl(String key) {
        return url(key);
    }

    @Override
    public String getPresignedUrl(String key) {
        return url(key);
    }

    @Override
    public byte[] download(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (IOException e) {
            log.error("로컬 파일 읽기 실패 - key: {}", key, e);
            throw new FileUploadException(e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path path = resolve(key);
            Files.deleteIfExists(path);
            Files.deleteIfExists(contentTypePath(path));
            log.info("로컬 파일 삭제 완료 - key: {}", key);
        } catch (IOException e) {
            log.error("로컬 파일 삭제 실패 - key: {}", key, e);
            throw new FileUploadException(e);
        }
    }

    // ---- LocalFileController 용 ----

    /** 업로드 스트림을 디스크에 저장한다. 최대 크기를 넘으면 중단하고 삭제한다. */
    public void save(String key, InputStream in, String contentType) throws IOException {
        Path target = resolve(key);
        Files.createDirectories(target.getParent());
        Path tmp = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
        try {
            long max = properties.getMaxFileSize();
            long written = 0;
            byte[] buf = new byte[8192];
            try (OutputStream out = Files.newOutputStream(tmp)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    written += n;
                    if (written > max) {
                        throw new IOException("최대 업로드 크기를 초과했습니다: " + max);
                    }
                    out.write(buf, 0, n);
                }
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(contentTypePath(target), contentType, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public boolean exists(String key) {
        return Files.isRegularFile(resolve(key));
    }

    public Path path(String key) {
        return resolve(key);
    }

    public String readContentType(String key) {
        try {
            return Files.readString(contentTypePath(resolve(key)), StandardCharsets.UTF_8).trim();
        } catch (NoSuchFileException e) {
            return "application/octet-stream";
        } catch (IOException e) {
            return "application/octet-stream";
        }
    }

    // ---- 내부 ----

    private String url(String key) {
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        return base + URL_PREFIX + key;
    }

    private Path contentTypePath(Path file) {
        return file.resolveSibling(file.getFileName() + CONTENT_TYPE_SUFFIX);
    }

    /** key 를 저장소 루트 아래 경로로 변환한다. 루트 밖으로 나가는 key 는 거부한다(path traversal 방지). */
    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.contains("\\") || key.contains("%") || key.contains("\0")) {
            throw new IllegalArgumentException("유효하지 않은 파일 key");
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root) || resolved.equals(root)) {
            throw new IllegalArgumentException("유효하지 않은 파일 key");
        }
        return resolved;
    }
}
