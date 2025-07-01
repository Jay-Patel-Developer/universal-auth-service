package com.ecommerce.user.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SecurityAuditConfigTest {
    @Test
    void testConfigLoads() {
        SecurityAuditConfig config = new SecurityAuditConfig();
        assertNotNull(config);
    }
}
