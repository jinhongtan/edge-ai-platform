package com.edgeaiplatform.backend.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CatalogServiceTest {
    private final CatalogService service = new CatalogService();

    @Test void initialCatalogsAreEmpty() {
        assertTrue(service.hardware().isEmpty());
        assertTrue(service.deployments().isEmpty());
        assertTrue(service.compatibilityReports().isEmpty());
    }
}
