package com.salescrm.service.dto.facebook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing an individual form field entry inside Meta Graph API's "field_data" array.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacebookFieldDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("name")
    private String name;

    @JsonProperty("values")
    private List<String> values = new ArrayList<>();

    public FacebookFieldDataDTO() {}

    public FacebookFieldDataDTO(String name, List<String> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    /**
     * Convenience helper to retrieve the first value if present.
     *
     * @return the primary field value, or null if list is empty
     */
    public String getFirstValue() {
        if (values != null && !values.isEmpty()) {
            return values.get(0);
        }
        return null;
    }

    @Override
    public String toString() {
        return "FacebookFieldDataDTO{" +
            "name='" + name + '\'' +
            ", values=" + values +
            '}';
    }
}
