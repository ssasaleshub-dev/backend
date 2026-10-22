package com.salescrm.service;

import java.nio.ByteBuffer;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class FileStorageService {

    private final S3AsyncClient s3Client;

    @Value("${cloudflare.r2.bucket-name}")
    private String bucketName;

    @Value("${cloudflare.r2.public-domain}")
    private String publicDomain;

    public FileStorageService(S3AsyncClient s3Client) {
        this.s3Client = s3Client;
    }

    public Mono<FileUploadResponse> uploadFile(FilePart filePart, String folder) {
        String originalFilename = filePart.filename();
        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx);
        }
        String targetFolder = (folder == null || folder.isBlank()) ? "uploads" : folder.trim();
        String key = targetFolder + "/" + UUID.randomUUID() + extension;
        String contentType = filePart.headers().getContentType() != null
            ? filePart.headers().getContentType().toString()
            : "application/octet-stream";

        return filePart
            .content()
            .map(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                return ByteBuffer.wrap(bytes);
            })
            .reduce((b1, b2) -> {
                ByteBuffer combined = ByteBuffer.allocate(b1.remaining() + b2.remaining());
                combined.put(b1).put(b2);
                combined.flip();
                return combined;
            })
            .flatMap(byteBuffer -> {
                long size = byteBuffer.remaining();
                PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .build();

                return Mono.fromFuture(s3Client.putObject(request, AsyncRequestBody.fromByteBuffer(byteBuffer)))
                    .map(response -> {
                        String publicUrl = publicDomain.replaceAll("/+$", "") + "/" + key;
                        String mediaType = "FILE";
                        if (contentType.startsWith("image/")) {
                            mediaType = "IMAGE";
                        } else if (contentType.startsWith("video/")) {
                            mediaType = "VIDEO";
                        }
                        return new FileUploadResponse(publicUrl, key, originalFilename, mediaType, size);
                    });
            });
    }

    public record FileUploadResponse(String url, String key, String fileName, String mediaType, long fileSize) {}
}
