package com.ly.springquickstart.controller;

import com.ly.springquickstart.pojo.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${app.upload-dir:#{null}}")
    private String uploadDir;

    private Path resolveUploadDir() {
        if (uploadDir != null && !uploadDir.isBlank()) {
            return Paths.get(uploadDir).toAbsolutePath();
        }
        return Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
    }

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) return Result.error("文件不能为空");

        String originalName = file.getOriginalFilename();
        String ext = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : "";
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;

        Path dir = resolveUploadDir();
        Files.createDirectories(dir);

        Path dest = dir.resolve(newName);
        file.transferTo(dest.toFile());

        String url = "/uploads/" + newName;
        return Result.success(url);
    }
}
