package com.ecommerce.user.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {
    @Test
    void testConfigLoads() {
        SecurityConfig config = new SecurityConfig();
        assertNotNull(config);
    }
}
