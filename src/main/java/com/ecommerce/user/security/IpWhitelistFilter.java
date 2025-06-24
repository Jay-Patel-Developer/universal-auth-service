package com.ecommerce.user.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class IpWhitelistFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(IpWhitelistFilter.class);

    @Value("${security.ip-whitelist.enabled:false}")
    private boolean ipWhitelistEnabled;

    @Value("${security.ip-whitelist.admin-ips:127.0.0.1,::1}")
    private String adminIpsConfig;

    @Value("${security.ip-whitelist.monitoring-ips:127.0.0.1,::1}")
    private String monitoringIpsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        if (!ipWhitelistEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIpAddress(request);
        String requestUri = request.getRequestURI();

        // Check admin endpoints
        if (requestUri.startsWith("/api/admin/") || requestUri.startsWith("/actuator/")) {
            List<String> allowedIps = Arrays.asList(adminIpsConfig.split(","));
            if (!isIpAllowed(clientIp, allowedIps)) {
                logger.warn("Unauthorized access attempt to admin endpoint {} from IP: {}", requestUri, clientIp);
                sendForbiddenResponse(response, "Access denied from this IP address");
                return;
            }
        }

        // Check monitoring endpoints
        if (requestUri.startsWith("/actuator/metrics") || requestUri.startsWith("/actuator/prometheus")) {
            List<String> allowedIps = Arrays.asList(monitoringIpsConfig.split(","));
            if (!isIpAllowed(clientIp, allowedIps)) {
                logger.warn("Unauthorized access attempt to monitoring endpoint {} from IP: {}", requestUri, clientIp);
                sendForbiddenResponse(response, "Monitoring access denied from this IP address");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isIpAllowed(String clientIp, List<String> allowedIps) {
        for (String allowedIp : allowedIps) {
            if (allowedIp.trim().equals(clientIp) || 
                allowedIp.trim().equals("0.0.0.0") || 
                matchesCidr(clientIp, allowedIp.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesCidr(String ip, String cidr) {
        try {
            if (!cidr.contains("/")) {
                return ip.equals(cidr);
            }
            
            String[] parts = cidr.split("/");
            String network = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            // Simple CIDR matching for IPv4
            if (ip.contains(".") && network.contains(".")) {
                return isIpInCidrRange(ip, network, prefixLength);
            }
        } catch (Exception e) {
            logger.warn("Error parsing CIDR notation: {}", cidr, e);
        }
        return false;
    }

    private boolean isIpInCidrRange(String ip, String network, int prefixLength) {
        try {
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(network);
            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        return (Long.parseLong(parts[0]) << 24) +
               (Long.parseLong(parts[1]) << 16) +
               (Long.parseLong(parts[2]) << 8) +
               Long.parseLong(parts[3]);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(
            "{\"error\":\"Access Forbidden\",\"message\":\"" + message + "\"}"
        );
    }
}