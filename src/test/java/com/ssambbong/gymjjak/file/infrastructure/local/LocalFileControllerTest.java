package com.ssambbong.gymjjak.file.infrastructure.local;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalFileControllerTest {

    @TempDir
    Path tempDir;

    LocalFileStorageAdapter adapter;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalStorageProperties props = new LocalStorageProperties();
        props.setBaseDir(tempDir.toString());
        props.setBaseUrl("http://localhost:8080/");
        props.setMaxFileSize(1024);
        adapter = new LocalFileStorageAdapter(props);
        mockMvc = MockMvcBuilders.standaloneSetup(new LocalFileController(adapter)).build();
    }

    @Test
    void 업로드_후_조회_삭제() throws Exception {
        String key = "uploads/meals/1/abc";
        byte[] body = "hello-image".getBytes();

        assertThat(adapter.generatePresignedUploadUrl(key, "image/png"))
                .isEqualTo("http://localhost:8080/api/files/local/" + key);

        mockMvc.perform(put("/api/files/local/" + key).contentType("image/png").content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/files/local/" + key))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(body));

        assertThat(adapter.download(key)).isEqualTo(body);

        adapter.delete(key);
        mockMvc.perform(get("/api/files/local/" + key)).andExpect(status().isNotFound());
    }

    @Test
    void 영상_Range_요청은_206() throws Exception {
        String key = "uploads/feedbacks/videos/1/v";
        mockMvc.perform(put("/api/files/local/" + key).contentType("video/mp4").content("0123456789".getBytes()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/files/local/" + key).header("Range", "bytes=2-5"))
                .andExpect(status().isPartialContent())
                .andExpect(content().string("2345"));
    }

    @Test
    void 허용되지_않은_콘텐츠타입은_415() throws Exception {
        mockMvc.perform(put("/api/files/local/uploads/x/1/a").contentType("text/html").content("<script>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void 최대_크기_초과는_413() throws Exception {
        mockMvc.perform(put("/api/files/local/uploads/x/1/big").contentType("image/png").content(new byte[2048]))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    void 경로_탈출은_거부() throws Exception {
        mockMvc.perform(put("/api/files/local/uploads/../../evil").contentType("image/png").content("x".getBytes()))
                .andExpect(status().isBadRequest());
        assertThat(tempDir.getParent().resolve("evil")).doesNotExist();
    }
}
