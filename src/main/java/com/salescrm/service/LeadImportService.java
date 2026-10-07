package com.salescrm.service;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.LeadImportResultDTO;
import java.io.InputStream;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Service interface for ingesting and processing lead data from spreadsheets and raw streams.
 * Dependency Inversion Principle: High-level consumers depend on this abstraction.
 */
public interface LeadImportService {

    /**
     * Ingests leads from an uploaded file stream using strategy resolution.
     *
     * @param fileName uploaded file name
     * @param contentType MIME content type
     * @param inputStream binary stream
     * @return Mono containing the import result telemetry
     */
    Mono<LeadImportResultDTO> importFile(String fileName, String contentType, InputStream inputStream);

    /**
     * Ingests leads from pre-parsed row DTOs.
     *
     * @param sourceName name of data source (e.g., file name or manual entry)
     * @param rows parsed lead rows
     * @return Mono containing import result
     */
    Mono<LeadImportResultDTO> importRows(String sourceName, List<ExcelLeadRowDTO> rows);

    /**
     * Ingests the bundled reference/sample Excel data.
     *
     * @return Mono containing import result
     */
    Mono<LeadImportResultDTO> importSampleData();
}
