package com.museum.reservation.module.system.controller;

import com.museum.reservation.common.ApiResponse;
import com.museum.reservation.common.BusinessException;
import com.museum.reservation.common.UploadVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 8L * 1024 * 1024;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${app.upload.url-prefix:/uploads}")
    private String urlPrefix;

    @PostMapping("/upload")
    public ApiResponse<UploadVO> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("图片大小不能超过 8MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持上传图片文件");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new BusinessException("仅支持 JPG、PNG、WebP、GIF 格式的图片");
        }

        LocalDate today = LocalDate.now();
        String year = String.valueOf(today.getYear());
        String month = String.format("%02d", today.getMonthValue());
        String storedName = UUID.randomUUID().toString().replace("-", "")
                + "." + extension.toLowerCase(Locale.ROOT);

        Path directory = Paths.get(uploadDir, year, month).toAbsolutePath().normalize();
        Path destination = directory.resolve(storedName);
        try {
            Files.createDirectories(directory);
            try (var input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BusinessException(500, "图片保存失败，请稍后重试");
        }

        String prefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        String url = prefix + "/" + year + "/" + month + "/" + storedName;
        return ApiResponse.ok(new UploadVO(url, originalName, file.getSize()));
    }
}
