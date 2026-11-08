package com.david.hlp.web.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.hadoop.fs.FileSystem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HadoopConfigTest {

    @TempDir
    Path directory;

    @Test
    void localLogsCanAppendAfterReopening() throws Exception {
        HadoopConfig config = new HadoopConfig();
        ReflectionTestUtils.setField(config, "hdfsUri", "file:///");
        ReflectionTestUtils.setField(config, "hdfsUser", System.getProperty("user.name"));
        Path log = directory.resolve("access.log");
        org.apache.hadoop.fs.Path file = new org.apache.hadoop.fs.Path(log.toUri());
        try (FileSystem fs = config.fileSystem(config.hadoopConfiguration())) {
            try (var output = fs.create(file)) {
                output.write("first\n".getBytes(StandardCharsets.UTF_8));
                output.hsync();
            }
            try (var output = fs.append(file)) {
                output.write("second\n".getBytes(StandardCharsets.UTF_8));
                output.hsync();
            }
        }
        assertEquals("first\nsecond\n", Files.readString(log));
    }
}
