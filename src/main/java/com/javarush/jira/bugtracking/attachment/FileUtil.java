package com.javarush.jira.bugtracking.attachment;

import com.javarush.jira.common.error.IllegalRequestDataException;
import com.javarush.jira.common.error.NotFoundException;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class FileUtil {
    private static final String ATTACHMENT_PATH = "./attachments/%s/";

    public void upload(MultipartFile multipartFile, String dir, String fileName) {
        if (multipartFile.isEmpty()) {
            throw new IllegalRequestDataException("Select a file to upload.");
        }

        Path targetDir  = Paths.get(dir).normalize();
        Path targetFile = targetDir.resolve(fileName).normalize();

        try { Files.createDirectories(targetDir);
            multipartFile.transferTo(targetFile);
        } catch (IOException ex) {
            throw new IllegalRequestDataException(
                    "Failed to upload file " + multipartFile.getOriginalFilename() + ex);
        }
    }

    /* ---------- unchanged helper methods ---------- */
    public Resource download(String fileLink) {
        try {
            Path      path      = Paths.get(fileLink);
            Resource  resource  = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalRequestDataException("Failed to download file " + resource.getFilename());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("File " + fileLink + " not found" + e);
        }
    }

    public void delete(String fileLink) {
        try {
            Files.deleteIfExists(Paths.get(fileLink));   // idempotent
        } catch (IOException ex) {
            throw new IllegalRequestDataException("File " + fileLink + " deletion failed." + ex);
        }
    }

    public static String getPath(String titleType) {
        return String.format(ATTACHMENT_PATH, titleType.toLowerCase());
    }
}
