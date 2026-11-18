package com.elemental.tech.fileprocessing.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileProcessingController {

    @PostMapping(
            value = "/process",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<Map<String, Object>> process(
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "File is empty")
            );
        }


        return ResponseEntity.ok(Map.of(
                "message", "File accepted",
                "fileName", file.getOriginalFilename() == null
                        ? "" : file.getOriginalFilename(),
                "size", file.getSize()
        ));
    }
}
