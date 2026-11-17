package com.ssambbong.gymjjak.file.infrastructure.local;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * 로컬 디스크 파일 저장소 설정 (storage.type=local 일 때만 활성화).
 */
@Getter
@Setter
@Configuration
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
@ConfigurationProperties(prefix = "storage.local")
public class LocalStorageProperties {

    /** 파일이 저장될 루트 디렉터리 */
    private String baseDir = "./uploads";

    /** 클라이언트(브라우저, AI 서버)가 파일에 접근할 때 쓰는 백엔드 주소 */
    private String baseUrl = "http://localhost:8080";

    /** 업로드 1건 최대 크기(byte). 기본 200MB */
    private long maxFileSize = 200L * 1024 * 1024;
}
