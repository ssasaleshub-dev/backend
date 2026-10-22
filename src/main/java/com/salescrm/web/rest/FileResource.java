package com.salescrm.web.rest;

import com.salescrm.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for file upload operations using Cloudflare R2 / S3.
 */
@RestController
@RequestMapping("/api/files")
public class FileResource {

    private static final Logger LOG = LoggerFactory.getLogger(FileResource.class);

    private final FileStorageService fileStorageService;

    public FileResource(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * {@code POST  /api/files/upload} : Upload a file to Cloudflare R2 object storage.
     *
     * @param filePartMono the multipart file part
     * @param folder the destination subfolder (defaults to "chat")
     * @return the {@link ResponseEntity} with the uploaded file metadata and public URL.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<FileStorageService.FileUploadResponse>> upload(
        @RequestPart("file") Mono<FilePart> filePartMono,
        @RequestParam(name = "folder", required = false, defaultValue = "chat") String folder
    ) {
        LOG.debug("REST request to upload file to folder: {}", folder);
        return filePartMono
            .flatMap(filePart -> fileStorageService.uploadFile(filePart, folder))
            .map(ResponseEntity::ok);
    }
}
