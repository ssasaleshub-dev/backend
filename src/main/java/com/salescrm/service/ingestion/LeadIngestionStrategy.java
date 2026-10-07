package com.salescrm.service.ingestion;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.InputStream;
import java.util.List;

/**
 * Strategy pattern interface for lead ingestion.
 * Follows Open/Closed Principle: Open for extension (new ad platforms or file formats)
 * and Closed for modification.
 * Follows Liskov Substitution Principle: Any strategy can parse an input stream into standard rows.
 */
public interface LeadIngestionStrategy {

    /**
     * Determines whether this strategy can handle the given file and MIME type.
     *
     * @param fileName the uploaded file name
     * @param contentType the MIME type of the uploaded file
     * @return true if supported, false otherwise
     */
    boolean supports(String fileName, String contentType);

    /**
     * Parses the incoming stream into normalized row DTOs.
     *
     * @param inputStream the raw file input stream
     * @return list of parsed lead row DTOs
     * @throws Exception if parsing fails
     */
    List<ExcelLeadRowDTO> parse(InputStream inputStream) throws Exception;
}
