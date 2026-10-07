package com.salescrm.web.rest;

import com.salescrm.service.LeadImportService;
import com.salescrm.service.dto.LeadImportResultDTO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for spreadsheet and raw data ingestion.
 */
@RestController
@RequestMapping("/api/leads/import")
public class LeadImportResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadImportResource.class);

    private final LeadImportService leadImportService;

    public LeadImportResource(LeadImportService leadImportService) {
        this.leadImportService = leadImportService;
    }

    /**
     * {@code POST /api/leads/import/file} : Upload and ingest an Excel (.xlsx/.xls) or CSV file.
     *
     * @param filePartMono the multipart file upload
     * @return the execution results and telemetry
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<LeadImportResultDTO>> uploadFile(@RequestPart("file") Mono<FilePart> filePartMono) {
        LOG.info("REST request to upload and ingest lead spreadsheet");
        return filePartMono.flatMap(filePart ->
            DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);
                    ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

                    String contentType = filePart.headers().getContentType() != null
                        ? filePart.headers().getContentType().toString()
                        : "application/octet-stream";

                    return leadImportService.importFile(filePart.filename(), contentType, bais);
                })
        ).map(ResponseEntity::ok);
    }

    /**
     * {@code POST /api/leads/import/raw} : Ingest raw TSV or CSV string payload.
     *
     * @param rawData raw text data
     * @return the execution results
     */
    @PostMapping(value = "/raw", consumes = { MediaType.TEXT_PLAIN_VALUE, "text/csv" })
    public Mono<ResponseEntity<LeadImportResultDTO>> importRaw(@RequestBody String rawData) {
        LOG.info("REST request to ingest raw text data");
        ByteArrayInputStream bais = new ByteArrayInputStream(rawData.getBytes(StandardCharsets.UTF_8));
        return leadImportService.importFile("raw_input.csv", "text/csv", bais)
            .map(ResponseEntity::ok);
    }

    /**
     * {@code POST /api/leads/import/sample} : Ingest the bundled reference Excel/CSV sample data.
     *
     * @return the execution results
     */
    @PostMapping("/sample")
    public Mono<ResponseEntity<LeadImportResultDTO>> importSample() {
        LOG.info("REST request to trigger seed sample data ingestion");
        return leadImportService.importSampleData()
            .map(ResponseEntity::ok);
    }
}
