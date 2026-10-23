package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.salescrm.service.LeadImportService;
import com.salescrm.service.dto.LeadImportResultDTO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LeadImportResourceTest {

    @Mock
    private LeadImportService leadImportService;

    @Mock
    private FilePart filePart;

    private LeadImportResource resource;

    @BeforeEach
    void setUp() {
        resource = new LeadImportResource(leadImportService);
    }

    @Test
    void shouldReturnErrorWhenFilePartMonoIsNull() {
        ResponseEntity<LeadImportResultDTO> response = resource.uploadFile(null).block();

        assertThat(response).isNotNull();
        LeadImportResultDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).contains("No file provided in request part 'file'");
    }

    @Test
    void shouldReturnErrorWhenFilePartMonoIsEmpty() {
        ResponseEntity<LeadImportResultDTO> response = resource.uploadFile(Mono.empty()).block();

        assertThat(response).isNotNull();
        LeadImportResultDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).contains("No file provided in request part 'file'");
    }

    @Test
    void shouldReturnErrorWhenFilePartHasEmptyFilename() {
        when(filePart.filename()).thenReturn("");

        ResponseEntity<LeadImportResultDTO> response = resource.uploadFile(Mono.just(filePart)).block();

        assertThat(response).isNotNull();
        LeadImportResultDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).contains("Uploaded file has an empty filename");
    }

    @Test
    void shouldReturnErrorWhenFileContentIsEmpty() {
        when(filePart.filename()).thenReturn("empty.csv");
        when(filePart.content()).thenReturn(Flux.empty());

        ResponseEntity<LeadImportResultDTO> response = resource.uploadFile(Mono.just(filePart)).block();

        assertThat(response).isNotNull();
        LeadImportResultDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors().get(0)).contains("empty (0 bytes)");
    }

    @Test
    void shouldUploadAndIngestValidFile() {
        String content = "id,phone_number\nl:123,+916385888585\n";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(bytes);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));

        when(filePart.filename()).thenReturn("leads.csv");
        when(filePart.headers()).thenReturn(headers);
        when(filePart.content()).thenReturn(Flux.just(buffer));

        LeadImportResultDTO mockResult = LeadImportResultDTO.create("leads.csv");
        mockResult.setTotalRowsProcessed(1);
        mockResult.addSuccess("l:123");

        when(leadImportService.importFile(eq("leads.csv"), eq("text/csv"), any(InputStream.class)))
            .thenReturn(Mono.just(mockResult));

        ResponseEntity<LeadImportResultDTO> response = resource.uploadFile(Mono.just(filePart)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        LeadImportResultDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getFileName()).isEqualTo("leads.csv");
        assertThat(body.getSuccessfulRows()).isEqualTo(1);
        assertThat(body.getImportedLeadExternalIds()).containsExactly("l:123");
    }

    @Test
    void shouldIngestRawData() {
        String rawData = "id,phone\nl:999,+919999999999";
        LeadImportResultDTO mockResult = LeadImportResultDTO.create("raw_input.csv");
        mockResult.setTotalRowsProcessed(1);
        mockResult.addSuccess("l:999");

        when(leadImportService.importFile(eq("raw_input.csv"), eq("text/csv"), any(InputStream.class)))
            .thenReturn(Mono.just(mockResult));

        ResponseEntity<LeadImportResultDTO> response = resource.importRaw(rawData).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessfulRows()).isEqualTo(1);
    }

    @Test
    void shouldIngestSampleData() {
        LeadImportResultDTO mockResult = LeadImportResultDTO.create("sample_leads.xlsx");
        mockResult.setTotalRowsProcessed(5);
        mockResult.setSuccessfulRows(5);

        when(leadImportService.importSampleData()).thenReturn(Mono.just(mockResult));

        ResponseEntity<LeadImportResultDTO> response = resource.importSample().block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccessfulRows()).isEqualTo(5);
    }
}
