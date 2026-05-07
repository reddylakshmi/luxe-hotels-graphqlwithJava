package com.luxe.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Per-subgraph backend API config bound from {@code luxe.backend.*} in
 * {@code application.yml}. {@code base-url} is empty by default — when blank,
 * each subgraph keeps using its in-memory mock data source. When a future
 * REST data source is added, it reads {@link #getBaseUrl()} and routes calls
 * to the configured backend.
 */
@Component
@ConfigurationProperties(prefix = "luxe.backend")
public class LuxeBackendProperties {

    private String baseUrl = "";
    private int connectTimeoutMs = 2000;
    private int readTimeoutMs = 5000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl == null ? "" : baseUrl; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }

    public boolean isConfigured() { return !baseUrl.isBlank(); }
}
