package com.salescrm.service.ingestion;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Factory pattern: Resolves and provides the appropriate LeadIngestionStrategy
 * based on input file name and content type.
 */
@Component
public class IngestionStrategyFactory {

    private static final Logger LOG = LoggerFactory.getLogger(IngestionStrategyFactory.class);

    private final List<LeadIngestionStrategy> strategies;

    public IngestionStrategyFactory(List<LeadIngestionStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * Resolves the strategy that supports the specified file attributes.
     *
     * @param fileName file name (e.g. leads.xlsx, data.csv)
     * @param contentType MIME type (e.g. application/vnd.openxmlformats-officedocument.spreadsheetml.sheet)
     * @return supported LeadIngestionStrategy
     * @throws IllegalArgumentException if no strategy matches
     */
    public LeadIngestionStrategy getStrategy(String fileName, String contentType) {
        for (LeadIngestionStrategy strategy : strategies) {
            if (strategy.supports(fileName, contentType)) {
                LOG.debug("Selected ingestion strategy: {} for file '{}'", strategy.getClass().getSimpleName(), fileName);
                return strategy;
            }
        }

        throw new IllegalArgumentException(
            "Unsupported file format for '" + fileName + "'. Supported formats are Microsoft Excel (.xlsx, .xls) and CSV/TSV (.csv, .tsv)."
        );
    }
}
