package com.ecommerce.user.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebSecurityConfigTest {
    @Test
    void testConfigLoads() {
        WebSecurityConfig config = new WebSecurityConfig();
        assertNotNull(config);
    }
}
