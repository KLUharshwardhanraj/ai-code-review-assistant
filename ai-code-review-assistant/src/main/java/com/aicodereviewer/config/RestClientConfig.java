package com.aicodereviewer.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Configures the {@link RestClient} bean used exclusively for Gemini API calls.
 *
 * <h3>Why a dedicated ObjectMapper?</h3>
 * <p>The application-level Jackson config uses {@code SNAKE_CASE} property naming
 * (for our own REST API responses). However, the Gemini API expects and returns
 * camelCase JSON. This config creates a separate {@link ObjectMapper} with default
 * (camelCase) naming and wires it into the RestClient's message converters.</p>
 *
 * <h3>Timeout strategy</h3>
 * <ul>
 *   <li><strong>Connect timeout (5s)</strong> — Fail fast if Google's servers are unreachable.</li>
 *   <li><strong>Read timeout (25s)</strong> — LLM inference is slow; allow generous time
 *       but don't wait indefinitely.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class RestClientConfig {

    /**
     * ObjectMapper configured for Gemini API communication.
     *
     * <p>Uses default camelCase naming (matching Gemini's JSON contract) and
     * excludes null fields to keep payloads lean. Exposed as a named bean so
     * {@code GeminiService} can reuse it for parsing the nested JSON text
     * from Gemini's response.</p>
     */
    @Bean(name = "geminiObjectMapper")
    public ObjectMapper geminiObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * RestClient pre-configured for the Gemini API with:
     * <ul>
     *   <li>Base URL from {@link GeminiProperties#getUrl()}</li>
     *   <li>API key sent via the {@code x-goog-api-key} header (not URL query — avoids key leaking into logs)</li>
     *   <li>JDK HttpClient request factory with explicit connect/read timeouts</li>
     *   <li>Dedicated camelCase ObjectMapper for serialization/deserialization</li>
     * </ul>
     */
    @Bean(name = "geminiRestClient")
    public RestClient geminiRestClient(GeminiProperties properties, ObjectMapper geminiObjectMapper) {

        // Use standard SimpleClientHttpRequestFactory (avoids OS loopback socket restrictions on Windows)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeout()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeout()));

        // Build the RestClient with Gemini-specific configuration
        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .requestFactory(requestFactory)
                .defaultHeader("x-goog-api-key", properties.getKey() != null ? properties.getKey() : "")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(new MappingJackson2HttpMessageConverter(geminiObjectMapper));
                })
                .build();
    }
}
