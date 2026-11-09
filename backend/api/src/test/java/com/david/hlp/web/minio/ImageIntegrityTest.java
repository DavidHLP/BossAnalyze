package com.david.hlp.web.minio;

import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.minio.config.MinioConfig;
import com.david.hlp.web.minio.controller.ImageController;
import com.david.hlp.web.minio.service.MinioService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImageIntegrityTest {
    @Test
    void deletionRequiresExactOwnerPrefixAndControlledKey() throws Exception {
        MinioClient client = mock(MinioClient.class);
        MinioConfig config = mock(MinioConfig.class);
        when(config.getBucketName()).thenReturn("synthetic-images");
        MinioService service = new MinioService(client, config);
        for (String key : new String[]{"2_photo.png", "10_photo.png", "1_../2_photo.png", "1_\\2.png", "1_", "1_x\n.png"}) {
            BusinessException error = assertThrows(BusinessException.class, () -> service.deleteFile(key, 1L));
            assertEquals(403, error.getCode());
        }
        assertThrows(BusinessException.class, () -> service.deleteFile("1_photo.png", null));
        verifyNoInteractions(client);
        service.deleteFile("1_photo.png", 1L);
        verify(client).removeObject(argThat((RemoveObjectArgs args) -> "1_photo.png".equals(args.object())));
    }

    @Test
    void controllerUsesAuthenticatedOwnerAndPreservesDenial() {
        MinioClient client = mock(MinioClient.class);
        ImageController controller = new ImageController(new MinioService(client, mock(MinioConfig.class))) {
            @Override protected Long getCurrentUserId() { return 1L; }
        };
        BusinessException error = assertThrows(BusinessException.class, () -> controller.deleteImage("2_photo.png"));
        assertEquals(403, error.getCode());
        verifyNoInteractions(client);
    }

    @Test
    void legacySvgIsSandboxedDownloadAndRasterReadsStayAvailable() {
        MinioService service = mock(MinioService.class);
        when(service.getObject(anyString())).thenReturn(new ByteArrayInputStream(new byte[0]));
        ImageController controller = new ImageController(service);
        var svg = controller.viewImage("1_legacy.SVG");
        assertEquals("nosniff", svg.getHeaders().getFirst("X-Content-Type-Options"));
        assertTrue(svg.getHeaders().getFirst("Content-Security-Policy").contains("sandbox"));
        assertTrue(svg.getHeaders().getContentDisposition().isAttachment());
        assertEquals("image/png", controller.viewImage("1_photo.png").getHeaders().getContentType().toString());
    }

    @Test
    void imageHttpResponseStreamsBytesAndKeepsErrorsAsJson() throws Exception {
        MinioService service = mock(MinioService.class);
        byte[] png = new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        when(service.getObject("1_photo.png")).thenReturn(new ByteArrayInputStream(png));
        when(service.getObject("1_legacy.svg")).thenReturn(new ByteArrayInputStream(svg));
        when(service.getObject("1_missing.png")).thenThrow(new IllegalStateException("synthetic failure"));
        var mvc = MockMvcBuilders.standaloneSetup(new ImageController(service)).build();

        mvc.perform(get("/api/image/view/1_photo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(png))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
        var response = mvc.perform(get("/api/image/view/1_legacy.svg"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(svg))
                .andExpect(header().string("Content-Security-Policy", "sandbox; default-src 'none'"))
                .andReturn().getResponse();
        assertTrue(response.getHeader("Content-Disposition").startsWith("attachment;"));
        mvc.perform(get("/api/image/view/1_missing.png"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500));
    }
}
