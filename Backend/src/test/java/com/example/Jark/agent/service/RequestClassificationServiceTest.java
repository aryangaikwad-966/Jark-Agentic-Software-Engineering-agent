package com.example.Jark.agent.service;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.Jark.agent.dto.RequestClassification;

/**
 * Test request classification service structure and schemas.
 */
class RequestClassificationServiceTest {

    @Test
    void requestClassificationEnum_hasAllRequiredValues() {
        // Assert that all required classification values exist
        assertEquals(4, RequestClassification.values().length);
        
        assertTrue(Arrays.stream(RequestClassification.values())
                .anyMatch(c -> c == RequestClassification.INVESTIGATE));
        assertTrue(Arrays.stream(RequestClassification.values())
                .anyMatch(c -> c == RequestClassification.REMEDIATE));
        assertTrue(Arrays.stream(RequestClassification.values())
                .anyMatch(c -> c == RequestClassification.INVESTIGATE_AND_REMEDIATE));
        assertTrue(Arrays.stream(RequestClassification.values())
                .anyMatch(c -> c == RequestClassification.OUT_OF_SCOPE));
    }

    @Test
    void requestClassificationEnum_valuesAreCorrect() {
        // Assert enum values match expected strings
        assertEquals("INVESTIGATE", RequestClassification.INVESTIGATE.name());
        assertEquals("REMEDIATE", RequestClassification.REMEDIATE.name());
        assertEquals("INVESTIGATE_AND_REMEDIATE", RequestClassification.INVESTIGATE_AND_REMEDIATE.name());
        assertEquals("OUT_OF_SCOPE", RequestClassification.OUT_OF_SCOPE.name());
    }

    @Test
    void requestClassificationEnum_canBeParsedFromString() {
        // Test that enum values can be parsed from strings
        assertEquals(RequestClassification.INVESTIGATE, RequestClassification.valueOf("INVESTIGATE"));
        assertEquals(RequestClassification.REMEDIATE, RequestClassification.valueOf("REMEDIATE"));
        assertEquals(RequestClassification.INVESTIGATE_AND_REMEDIATE, RequestClassification.valueOf("INVESTIGATE_AND_REMEDIATE"));
        assertEquals(RequestClassification.OUT_OF_SCOPE, RequestClassification.valueOf("OUT_OF_SCOPE"));
    }

    @Test
    void requestClassificationEnum_isCaseSensitive() {
        // Test that enum parsing is case-sensitive
        assertThrows(IllegalArgumentException.class, () -> {
            RequestClassification.valueOf("investigate");
        });
    }
}