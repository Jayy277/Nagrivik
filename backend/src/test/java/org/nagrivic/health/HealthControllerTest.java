package org.nagrivic.health;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<DataSource> dataSourceProvider = mock(ObjectProvider.class);
    private final HealthController controller = new HealthController(dataSourceProvider);

    @Test
    void healthEndpointReturnsUp() {
        when(dataSourceProvider.getIfAvailable()).thenReturn(null);
        ResponseEntity<HealthResponse> response = controller.checkHealth();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().status());
        assertEquals("nagrivic-backend", response.getBody().service());
    }
}
