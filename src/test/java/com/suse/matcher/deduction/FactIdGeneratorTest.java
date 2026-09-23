package com.suse.matcher.deduction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FactIdGeneratorTest {

    private FactIdGenerator generator;

    @BeforeEach
    void setup() {
        generator = new FactIdGenerator();
    }

    @DisplayName("Reuses ids for equal tuples")
    @Test
    void reusesIdsForEqualTuples() {
        int firstId = generator.generate("match", 1L, 2L, 3L);
        int secondId = generator.generate("match", 1L, 2L, 3L);

        assertEquals(firstId, secondId);
    }

    @DisplayName("Generates distinct ids for different tuples")
    @Test
    void generatesDistinctIdsForDifferentTuples() {
        int firstId = generator.generate("match", 1L, 2L, 3L);
        int secondId = generator.generate("match", 1L, 2L, 4L);

        assertAll(
            () -> assertEquals(0, firstId),
            () -> assertEquals(1, secondId),
            () -> assertNotEquals(firstId, secondId)
        );
    }

    @DisplayName("Separates ids by namespace")
    @Test
    void separatesIdsByNamespace() {
        int firstId = generator.generate("match", 1L, 2L, 3L);
        int secondId = generator.generate("penalty", 1L, 2L, 3L);

        assertNotEquals(firstId, secondId);
    }

    @DisplayName("Uses root as default namespace")
    @Test
    void usesRootAsDefaultNamespace() {
        int firstId = generator.generate("root", 1L, 2L, 3L);
        int secondId = generator.generate(1L, 2L, 3L);

        assertEquals(firstId, secondId);
    }

    @DisplayName("Uses class simple name as namespace")
    @Test
    void usesClassSimpleNameAsNamespace() {
        int firstId = generator.generate(FactIdGeneratorTest.class, 1L, 2L, 3L);
        int secondId = generator.generate("FactIdGeneratorTest", 1L, 2L, 3L);

        assertEquals(firstId, secondId);
    }

    @DisplayName("Rejects null namespace")
    @Test
    void rejectsNullNamespaceNamespace() {
        assertAll(
            () -> assertThrows(NullPointerException.class, () -> generator.generate((String) null, 1L)),
            () -> assertThrows(NullPointerException.class, () -> generator.generate((Class<?>) null, 1L))
        );
    }

    @DisplayName("Accepts null values")
    @Test
    void acceptsNullVales() {
        assertAll(
            () -> assertDoesNotThrow(() -> generator.generate(1L, null)),
            () -> assertDoesNotThrow(() -> generator.generate(FactIdGeneratorTest.class, 1L, null)),
            () -> assertDoesNotThrow(() -> generator.generate("match", 1L, null))
        );
    }


}
