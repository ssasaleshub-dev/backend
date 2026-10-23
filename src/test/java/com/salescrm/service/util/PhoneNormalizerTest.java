package com.salescrm.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PhoneNormalizerTest {

    private PhoneNormalizer phoneNormalizer;

    @BeforeEach
    void setUp() {
        phoneNormalizer = new PhoneNormalizer();
    }

    @Test
    void shouldStripMetaAdsPColonPrefix() {
        assertThat(phoneNormalizer.normalize("p:+916385888585")).isEqualTo("+916385888585");
        assertThat(phoneNormalizer.normalize("P:+916385888585")).isEqualTo("+916385888585");
    }

    @Test
    void shouldStripTelPrefix() {
        assertThat(phoneNormalizer.normalize("tel:+916385888585")).isEqualTo("+916385888585");
    }

    @Test
    void shouldEnsurePlus91FormatForIndianNumbers() {
        assertThat(phoneNormalizer.normalize("9025099944")).isEqualTo("+919025099944");
        assertThat(phoneNormalizer.normalize("09025099944")).isEqualTo("+919025099944");
        assertThat(phoneNormalizer.normalize("919025099944")).isEqualTo("+919025099944");
        assertThat(phoneNormalizer.normalize("p:9025099944")).isEqualTo("+919025099944");
        assertThat(phoneNormalizer.normalize(" +91 (638) 588-8585 ")).isEqualTo("+916385888585");
        assertThat(phoneNormalizer.normalize("91.6385.888585")).isEqualTo("+916385888585");
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertThat(phoneNormalizer.normalize(null)).isNull();
    }
}
