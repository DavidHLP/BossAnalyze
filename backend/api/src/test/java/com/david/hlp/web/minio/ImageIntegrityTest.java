package com.david.hlp.web.minio;

import com.david.hlp.web.common.exception.BusinessException;
import com.david.hlp.web.minio.config.MinioConfig;
import com.david.hlp.web.minio.controller.ImageController;
import com.david.hlp.web.minio.service.MinioService;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
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
}
