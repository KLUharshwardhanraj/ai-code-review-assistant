package com.aicodereviewer.service;

import com.aicodereviewer.config.GeminiProperties;
import com.aicodereviewer.dto.gemini.FeedbackItemDto;
import com.aicodereviewer.dto.gemini.GeminiRequestDto;
import com.aicodereviewer.dto.gemini.GeminiRequestDto.Content;
import com.aicodereviewer.dto.gemini.GeminiRequestDto.GenerationConfig;
import com.aicodereviewer.dto.gemini.GeminiResponseDto;
import com.aicodereviewer.exception.GeminiApiException;
import com.aicodereviewer.exception.GeminiParsingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for interacting with the Google Gemini API to produce
 * structured code review feedback.
 *
 * <h3>Request flow</h3>
 * <ol>
 *   <li>Build a Gemini request with a system instruction (senior reviewer persona),
 *       the user's code as the content, and a {@code responseSchema} that forces
 *       Gemini to output a JSON array of feedback items.</li>
 *   <li>POST the request to {@code /{model}:generateContent} via RestClient.</li>
 *   <li>Validate the response structure (non-null candidates, STOP finish reason).</li>
 *   <li>Extract the JSON text from the first candidate's content and deserialize it
 *       into {@code List<FeedbackItemDto>}.</li>
 * </ol>
 *
 * <h3>Resilience strategy</h3>
 * <ul>
 *   <li><strong>Retried</strong> (transient): 5xx server errors, 429 rate limits,
 *       network/socket timeouts — up to 3 attempts with exponential backoff (1.5s base, 2x multiplier, jitter).</li>
 *   <li><strong>Not retried</strong> (deterministic): 400/401/403 client errors,
 *       JSON parsing failures, invalid response structures — these propagate immediately.</li>
 *   <li><strong>Fallback</strong> ({@code @Recover}): After all retries are exhausted, returns
 *       a synthetic SYSTEM/WARNING feedback item instead of crashing with a 500.</li>
 * </ul>
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String SYSTEM_PROMPT = """
            You are a strict, meticulous senior code reviewer with deep expertise across \
            multiple programming languages and frameworks. Analyze the provided source code \
            for the following categories of issues:
            
            1. **BUG** — Logical errors, null pointer risks, off-by-one errors, incorrect API usage.
            2. **EDGE_CASE** — Unhandled boundary conditions, missing input validation, empty/null guards.
            3. **READABILITY** — Poor naming, complex nesting, missing documentation, unclear intent.
            4. **SECURITY** — Injection vulnerabilities, hardcoded secrets, insecure patterns, data exposure.
            
            For each issue found:
            - Identify the specific line number where the issue occurs.
            - Classify it into exactly one category and assign a severity level.
            - Provide a clear, actionable suggestion explaining the problem and how to fix it.
            - If applicable, provide a concrete replacement code snippet.
            
            If the code is clean and has no issues, return an empty JSON array [].
            Be precise. Do not invent issues that do not exist. Do not repeat yourself.""";

    private final RestClient geminiRestClient;
    private final ObjectMapper geminiObjectMapper;
    private final GeminiProperties geminiProperties;
    private final com.aicodereviewer.metrics.ReviewMetrics reviewMetrics;

    /**
     * Constructs the GeminiService with its required dependencies.
     *
     * @param geminiRestClient   RestClient pre-configured for Gemini (base URL, API key header, timeouts)
     * @param geminiObjectMapper ObjectMapper using camelCase naming for Gemini JSON contract
     * @param geminiProperties   externalized Gemini API configuration properties
     * @param reviewMetrics      telemetry metrics recorder
     */
    public GeminiService(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            @Qualifier("geminiObjectMapper") ObjectMapper geminiObjectMapper,
            GeminiProperties geminiProperties,
            com.aicodereviewer.metrics.ReviewMetrics reviewMetrics
    ) {
        this.geminiRestClient = geminiRestClient;
        this.geminiObjectMapper = geminiObjectMapper;
        this.geminiProperties = geminiProperties;
        this.reviewMetrics = reviewMetrics;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PUBLIC API — Retryable entry point
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Submits source code to the Gemini API for structured code review.
     *
     * <p>This method is the primary entry point for AI-powered code analysis.
     * It constructs a tightly-constrained prompt, calls Gemini with strict JSON
     * schema enforcement, and returns a parsed list of feedback items.</p>
     *
     * <p>Transient failures (5xx, 429, timeouts) are retried automatically.
     * Deterministic failures (parsing errors, invalid responses) propagate immediately.</p>
     *
     * @param sourceCode the raw source code or Git unified diff to review
     * @param language   the programming language (e.g., "java", "python")
     * @return list of structured feedback items; empty list if the code is clean
     * @throws GeminiApiException     if the response structure is invalid (not retried)
     * @throws GeminiParsingException if the response JSON cannot be deserialized (not retried)
     */
    @Retryable(
            retryFor = {
                    HttpServerErrorException.class,
                    ResourceAccessException.class,
                    HttpClientErrorException.TooManyRequests.class
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1500, multiplier = 2.0, random = true)
    )
    public List<FeedbackItemDto> reviewCode(String sourceCode, String language) {
        log.info("Initiating Gemini code review | model={} | language={} | codeLength={}",
                geminiProperties.getModel(), language, sourceCode.length());

        // Zero-configuration intelligent simulation fallback if no API key is provided
        if (isUnconfiguredKey(geminiProperties.getKey())) {
            log.info("Gemini API key is unconfigured or default. Running intelligent local analysis for language: {}", language);
            io.micrometer.core.instrument.Timer.Sample sample = reviewMetrics.startTimer();
            List<FeedbackItemDto> offlineItems = generateOfflineReview(sourceCode, language);
            reviewMetrics.recordGeminiLatency(sample, "local-simulation", "success");
            return offlineItems;
        }

        // 1. Build the structured request
        GeminiRequestDto request = buildRequest(sourceCode, language);

        // 2. Measure latency with Micrometer Timer.Sample
        io.micrometer.core.instrument.Timer.Sample sample = reviewMetrics.startTimer();

        try {
            // POST to Gemini API
            log.debug("Sending request to Gemini API endpoint: /{model}:generateContent",
                    geminiProperties.getModel());

            GeminiResponseDto response = geminiRestClient.post()
                    .uri("/{model}:generateContent", geminiProperties.getModel())
                    .body(request)
                    .retrieve()
                    .body(GeminiResponseDto.class);

            // 3. Validate the response envelope
            validateResponse(response);

            // 4. Extract the JSON feedback text from the first candidate
            String feedbackJson = extractFeedbackJson(response);
            log.debug("Raw Gemini feedback JSON (first 500 chars): {}",
                    feedbackJson.substring(0, Math.min(feedbackJson.length(), 500)));

            // 5. Parse into typed feedback items
            List<FeedbackItemDto> items = parseFeedbackItems(feedbackJson);

            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "success");
            log.info("Gemini review complete | language={} | feedbackItems={}", language, items.size());
            return items;

        } catch (HttpClientErrorException.TooManyRequests ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("rate_limit_429");
            throw ex;
        } catch (HttpServerErrorException ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("server_error_5xx");
            throw ex;
        } catch (ResourceAccessException ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("timeout");
            throw ex;
        } catch (GeminiParsingException ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("json_parse_error");
            throw ex;
        } catch (GeminiApiException ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("api_error");
            throw ex;
        } catch (HttpClientErrorException ex) {
            // Upstream client errors (e.g. 400 Bad Request, 401 Unauthorized from invalid API key)
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("client_error_" + ex.getStatusCode().value());
            log.error("Gemini API rejected request with HTTP status {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return recoverReviewCode(ex, sourceCode, language);
        } catch (Exception ex) {
            reviewMetrics.recordGeminiLatency(sample, geminiProperties.getModel(), "error");
            reviewMetrics.incrementGeminiFailure("unexpected_error");
            log.error("Unexpected error during Gemini API invocation: {}", ex.getMessage(), ex);
            return recoverReviewCode(ex, sourceCode, language);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RECOVERY — Fallback after all retries exhausted
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Fallback invoked when all retry attempts for {@link #reviewCode} are exhausted.
     *
     * <p>Returns a single synthetic feedback item with category {@code SYSTEM} to
     * signal to the caller (and ultimately the user) that the AI service is
     * temporarily unavailable. This prevents a raw 500 error from reaching the frontend.</p>
     *
     * @param ex         the exception from the final failed attempt
     * @param sourceCode the original source code (unused but required for signature matching)
     * @param language   the original language (logged for diagnostics)
     * @return a single-element list containing a SYSTEM/WARNING fallback item
     */
    @Recover
    public List<FeedbackItemDto> recoverReviewCode(Exception ex, String sourceCode, String language) {
        log.error("Gemini API exhausted all retry attempts | language={} | error={}",
                language, ex.getMessage(), ex);

        reviewMetrics.incrementGeminiFailure("retry_exhausted_fallback");

        FeedbackItemDto fallback = new FeedbackItemDto(
                null,
                "SYSTEM",
                "WARNING",
                "AI code review service is currently unavailable or rate-limited. Please retry shortly.",
                null
        );

        return List.of(fallback);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE — Request construction
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Assembles the complete Gemini API request with system instruction,
     * user code content, and the strict JSON response schema.
     */
    private GeminiRequestDto buildRequest(String sourceCode, String language) {
        // System instruction: establishes the reviewer persona
        Content systemInstruction = Content.of(SYSTEM_PROMPT);

        // User content: the code to review, wrapped in a fenced code block
        String userPrompt = String.format(
                "Review the following %s code and provide your analysis as a JSON array:%n```%s%n%s%n```",
                language, language, sourceCode
        );
        List<Content> contents = List.of(Content.of(userPrompt));

        // Generation config: enforce structured JSON output with schema
        GenerationConfig generationConfig = new GenerationConfig(
                "application/json",
                buildResponseSchema()
        );

        return new GeminiRequestDto(systemInstruction, contents, generationConfig);
    }

    /**
     * Builds the JSON schema that constrains Gemini's output to a strict
     * feedback array structure.
     *
     * <p>Uses {@link Map#ofEntries} for type-safe, readable schema construction
     * instead of fragile string concatenation. The schema maps directly to
     * {@link FeedbackItemDto}'s fields.</p>
     *
     * @return the response schema as a nested Map structure
     */
    private Map<String, Object> buildResponseSchema() {
        return Map.of(
                "type", "ARRAY",
                "items", Map.ofEntries(
                        Map.entry("type", "OBJECT"),
                        Map.entry("properties", Map.ofEntries(
                                Map.entry("lineNumber", Map.of(
                                        "type", "INTEGER",
                                        "nullable", true
                                )),
                                Map.entry("category", Map.of(
                                        "type", "STRING",
                                        "enum", List.of("BUG", "EDGE_CASE", "READABILITY", "SECURITY")
                                )),
                                Map.entry("severity", Map.of(
                                        "type", "STRING",
                                        "enum", List.of("CRITICAL", "WARNING", "INFO")
                                )),
                                Map.entry("suggestion", Map.of(
                                        "type", "STRING"
                                )),
                                Map.entry("replacementCode", Map.of(
                                        "type", "STRING",
                                        "nullable", true
                                ))
                        )),
                        Map.entry("required", List.of("category", "severity", "suggestion"))
                )
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //  PRIVATE — Response validation & parsing
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Validates the Gemini response envelope for structural completeness.
     *
     * @param response the raw deserialized Gemini response
     * @throws GeminiApiException if candidates are missing or the finish reason is not STOP
     */
    private void validateResponse(GeminiResponseDto response) {
        if (response == null) {
            throw new GeminiApiException("Gemini API returned a null response body.");
        }

        if (response.candidates() == null || response.candidates().isEmpty()) {
            throw new GeminiApiException("Gemini API returned an empty candidates list. "
                    + "The request may have been blocked by safety filters.");
        }

        GeminiResponseDto.Candidate candidate = response.candidates().get(0);

        // Check finish reason — anything other than STOP indicates incomplete output
        String finishReason = candidate.finishReason();
        if (finishReason != null && !"STOP".equalsIgnoreCase(finishReason)) {
            throw new GeminiApiException(String.format(
                    "Gemini generation did not complete normally. finishReason='%s'. "
                            + "Output may be truncated, filtered, or blocked.", finishReason));
        }

        if (candidate.content() == null
                || candidate.content().parts() == null
                || candidate.content().parts().isEmpty()) {
            throw new GeminiApiException("Gemini candidate contains no content parts.");
        }

        String text = candidate.content().parts().get(0).text();
        if (text == null || text.isBlank()) {
            throw new GeminiApiException("Gemini candidate text is null or blank.");
        }
    }

    /**
     * Extracts the raw JSON feedback string from the validated response.
     * Called only after {@link #validateResponse} has confirmed structural integrity.
     */
    private String extractFeedbackJson(GeminiResponseDto response) {
        return response.candidates().get(0).content().parts().get(0).text().trim();
    }

    /**
     * Deserializes the JSON feedback string into a typed list of feedback items.
     *
     * @param json the raw JSON array string from Gemini's response text
     * @return list of parsed feedback items (may be empty if code is clean)
     * @throws GeminiParsingException if the JSON is malformed or doesn't match the expected structure
     */
    private List<FeedbackItemDto> parseFeedbackItems(String json) {
        try {
            List<FeedbackItemDto> items = geminiObjectMapper.readValue(
                    json, new TypeReference<List<FeedbackItemDto>>() {}
            );

            if (items == null) {
                log.warn("Gemini returned null after JSON parsing; treating as empty feedback list.");
                return List.of();
            }

            // Validate each item — category and severity must not be null
            for (int i = 0; i < items.size(); i++) {
                FeedbackItemDto item = items.get(i);
                if (item.category() == null || item.category().isBlank()) {
                    throw new GeminiParsingException(String.format(
                            "Feedback item at index %d has a null or blank category.", i));
                }
                if (item.severity() == null || item.severity().isBlank()) {
                    throw new GeminiParsingException(String.format(
                            "Feedback item at index %d has a null or blank severity.", i));
                }
                if (item.suggestion() == null || item.suggestion().isBlank()) {
                    throw new GeminiParsingException(String.format(
                            "Feedback item at index %d has a null or blank suggestion.", i));
                }
            }

            return items;

        } catch (JsonProcessingException ex) {
            throw new GeminiParsingException(
                    "Failed to parse Gemini feedback JSON. Raw content (first 300 chars): "
                            + json.substring(0, Math.min(json.length(), 300)),
                    ex
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  OFFLINE SIMULATION & DIAGNOSTICS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Checks if the Gemini API key is missing or set to the default placeholder.
     */
    private boolean isUnconfiguredKey(String key) {
        return key == null || key.isBlank() || "your-api-key-here".equalsIgnoreCase(key.trim());
    }

    /**
     * Generates intelligent, structured code review feedback locally when the Gemini
     * API key is unconfigured. Uses multi-pattern static analysis for Java, Python, and
     * Git diffs. Enables full out-of-the-box demo execution without an API key.
     */
    private List<FeedbackItemDto> generateOfflineReview(String sourceCode, String language) {
        List<FeedbackItemDto> items = new ArrayList<>();
        String lang = language != null ? language.toLowerCase().trim() : "java";
        String[] lines = sourceCode.split("\\r?\\n");

        // ── JAVA ──────────────────────────────────────────────────────────────
        if (lang.contains("java")) {
            // Track variable names assigned from repository calls that may return null
            java.util.Set<String> nullableVars = new java.util.HashSet<>();

            for (int i = 0; i < lines.length; i++) {
                String raw = lines[i];
                String line = raw.trim();
                int lineNum = i + 1;

                // Pattern: SomeType varName = something.findBy...(...) or findOne/findFirst
                Matcher repoAssign = Pattern.compile(
                        "(?:var|\\w+)\\s+(\\w+)\\s*=\\s*\\w+\\.find(?:By\\w+|One|First|Any|All)\\s*\\("
                ).matcher(raw);
                while (repoAssign.find()) {
                    nullableVars.add(repoAssign.group(1));
                }

                // Check if any tracked nullable var is dereferenced without null safety
                for (String varName : nullableVars) {
                    if (raw.contains(varName + ".") && !raw.contains("== null")
                            && !raw.contains("!= null") && !raw.contains("Optional")
                            && !raw.contains("Objects.requireNonNull")
                            && !raw.contains(varName + " =") && !raw.contains(varName + "=")) {
                        items.add(new FeedbackItemDto(
                                lineNum, "BUG", "CRITICAL",
                                String.format(
                                    "NullPointerException risk: `%s` is obtained from a repository call " +
                                    "that may return null, but is accessed on line %d without a null-check. " +
                                    "Use Optional or add an explicit guard.",
                                    varName, lineNum),
                                String.format(
                                    "// Option A — Optional:\n" +
                                    "Optional.ofNullable(%s)\n" +
                                    "    .orElseThrow(() -> new EntityNotFoundException(\"%s not found\"));\n\n" +
                                    "// Option B — Explicit guard:\n" +
                                    "if (%s == null) throw new EntityNotFoundException(\"%s not found\");",
                                    varName, varName, varName, varName)
                        ));
                    }
                }

                // Unclosed I/O resources outside try-with-resources
                if ((line.contains("new FileInputStream") || line.contains("new BufferedReader")
                        || line.contains("new FileReader") || line.contains("new FileOutputStream")
                        || line.contains("new FileWriter"))
                        && !line.contains("try (") && !raw.contains("try (")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "BUG", "WARNING",
                            "Resource leak: I/O stream opened outside try-with-resources. " +
                            "The stream will not be closed if an exception is thrown.",
                            "try (var stream = new FileInputStream(path)) {\n    // safe — auto-closed\n}"));
                }

                // Floating-point for monetary values
                if (line.contains("double ") && (line.contains("price") || line.contains("total")
                        || line.contains("amount") || line.contains("balance") || line.contains("cost"))) {
                    items.add(new FeedbackItemDto(
                            lineNum, "BUG", "WARNING",
                            "IEEE 754 precision hazard: `double` is unsuitable for monetary values. " +
                            "Use `BigDecimal` to avoid rounding errors in financial calculations.",
                            "BigDecimal amount = BigDecimal.ZERO; // or new BigDecimal(\"0.00\")"));
                }

                // Hardcoded credentials
                if ((line.contains("password") || line.contains("secret") || line.contains("apiKey")
                        || line.contains("api_key") || line.contains("token"))
                        && line.contains("=") && line.contains("\"") && !line.startsWith("//")
                        && !line.startsWith("*")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "CRITICAL",
                            "Hardcoded credential in source code: Sensitive value must not be committed. " +
                            "Use environment variables, Spring @Value, or a secrets manager.",
                            "// Use env var:\nString secret = System.getenv(\"SECRET_KEY\");\n" +
                            "// Or Spring:\n@Value(\"${app.secret}\")\nprivate String secret;"));
                }

                // System.out.println in non-test code
                if (line.contains("System.out.print") && !line.startsWith("//")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "READABILITY", "INFO",
                            "Debug `System.out.println` found in production code path. " +
                            "Replace with SLF4J logger to enable structured log routing and level filtering.",
                            "private static final Logger log = LoggerFactory.getLogger(getClass());\nlog.debug(\"value: {}\", value);"));
                }

                // Overly broad exception catch
                if ((line.startsWith("catch (Exception") || line.startsWith("} catch (Exception")
                        || line.startsWith("catch (Throwable") || line.startsWith("} catch (Throwable"))
                        && !line.contains("// intentional")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "EDGE_CASE", "WARNING",
                            "Overly broad exception handling: Catching `Exception` or `Throwable` " +
                            "swallows unexpected failures and makes debugging very difficult. " +
                            "Catch the most specific exception type possible.",
                            "catch (IOException ex) {\n    log.error(\"I/O error\", ex);\n    throw new ServiceException(\"Operation failed\", ex);\n}"));
                }

                // e.printStackTrace()
                if (line.contains(".printStackTrace()")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "READABILITY", "WARNING",
                            "`e.printStackTrace()` outputs to stderr without log routing. " +
                            "Use a structured logger to preserve stack traces in log pipelines.",
                            "log.error(\"Unexpected error: {}\", ex.getMessage(), ex);"));
                }

                // Unused variable assigned but value never read (simple heuristic)
                Matcher unusedMatcher = Pattern.compile("(?:String|int|long|boolean|var)\\s+(\\w+)\\s*=").matcher(raw);
                if (unusedMatcher.find()) {
                    String varName = unusedMatcher.group(1);
                    // Check if that variable name appears anywhere else in the entire source
                    long usageCount = java.util.Arrays.stream(lines)
                            .filter(l -> l.contains(varName))
                            .count();
                    // Only flag if appears on exactly 1 line (the assignment itself) and name is not trivial
                    if (usageCount == 1 && varName.length() > 2 && !varName.equals("log")) {
                        items.add(new FeedbackItemDto(
                                lineNum, "READABILITY", "INFO",
                                String.format("Unused variable `%s` is assigned but never referenced. " +
                                        "Remove dead code to improve readability and avoid confusion.", varName),
                                "// Remove: " + raw.trim()));
                    }
                }
            }

        // ── PYTHON ────────────────────────────────────────────────────────────
        } else if (lang.contains("python")) {
            for (int i = 0; i < lines.length; i++) {
                String raw = lines[i];
                String line = raw.trim();
                int lineNum = i + 1;

                // SQL Injection via string interpolation
                if ((line.contains("f\"SELECT") || line.contains("f\"INSERT") || line.contains("f\"UPDATE")
                        || line.contains("f'SELECT") || line.contains("f'INSERT") || line.contains("f'UPDATE")
                        || (line.contains(".format(") && (line.contains("SELECT") || line.contains("WHERE")))
                        || (line.contains("% ") && (line.contains("SELECT") || line.contains("WHERE"))))) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "CRITICAL",
                            "SQL Injection vulnerability: User-controlled data is interpolated directly " +
                            "into a SQL query string. An attacker can inject arbitrary SQL commands.",
                            "# Use parameterized queries:\ncursor.execute(\"SELECT * FROM users WHERE id = %s\", (user_id,))"));
                }

                // Mutable default argument
                if (line.startsWith("def ") && (line.contains("=[]") || line.contains("= []")
                        || line.contains("={}") || line.contains("= {}"))) {
                    items.add(new FeedbackItemDto(
                            lineNum, "BUG", "WARNING",
                            "Mutable default argument: Python evaluates default values once at function " +
                            "definition time. Mutations to this argument will persist across all subsequent calls.",
                            "def process(user_id, tags=None):\n    if tags is None:\n        tags = []  # fresh list each call"));
                }

                // Bare except
                if (line.equals("except:") || line.startsWith("except BaseException:")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "EDGE_CASE", "WARNING",
                            "Bare `except:` silently swallows `KeyboardInterrupt`, `SystemExit`, and " +
                            "`GeneratorExit`. Always catch the most specific exception type.",
                            "except (ValueError, IOError) as ex:\n    logger.error(\"Operation failed\", exc_info=ex)"));
                }

                // print() in production code (not in test file)
                if (line.startsWith("print(") && !sourceCode.contains("def test_")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "READABILITY", "INFO",
                            "`print()` used for diagnostics. In production code, use the `logging` module " +
                            "for structured output with configurable log levels.",
                            "import logging\nlogger = logging.getLogger(__name__)\nlogger.debug(\"value: %s\", value)"));
                }

                // Hardcoded credentials
                if ((line.contains("password") || line.contains("secret") || line.contains("api_key"))
                        && line.contains("=") && line.contains("\"") && !line.startsWith("#")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "CRITICAL",
                            "Hardcoded credential in source: Sensitive values must be loaded from " +
                            "environment variables or a secrets vault — never committed to source control.",
                            "import os\npassword = os.environ['DB_PASSWORD']  # or use python-dotenv"));
                }

                // File opened without context manager
                if (line.contains("open(") && !line.contains("with ") && !line.contains("# noqa")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "BUG", "WARNING",
                            "File handle leak: File is opened outside a `with` block. " +
                            "If an exception occurs before `.close()`, the file descriptor is never released.",
                            "with open(path, 'r', encoding='utf-8') as fh:\n    data = fh.read()"));
                }
            }

        // ── GIT DIFF ──────────────────────────────────────────────────────────
        } else if (lang.contains("diff") || sourceCode.contains("@@") || sourceCode.contains("diff --git")) {
            for (int i = 0; i < lines.length; i++) {
                String raw = lines[i];
                String line = raw.trim();
                int lineNum = i + 1;

                // Secret added to codebase
                if (raw.startsWith("+") && !raw.startsWith("+++")
                        && (line.contains("password") || line.contains("secret") || line.contains("token")
                            || line.contains("api_key") || line.contains("apiKey"))
                        && line.contains("\"")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "CRITICAL",
                            "Credential committed to repository: This diff introduces a hardcoded secret. " +
                            "Remove it immediately, rotate the credential, and add the file to .gitignore.",
                            "// Use env var or @Value:\nString secret = System.getenv(\"SECRET_KEY\");"));
                }

                // Auth token logged
                if (raw.startsWith("+") && !raw.startsWith("+++")
                        && (line.contains("token") || line.contains("auth") || line.contains("jwt"))
                        && (line.contains("println") || line.contains("console.log") || line.contains("log.info")
                            || line.contains("print("))) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "CRITICAL",
                            "Authentication token written to logs: Sensitive bearer token or auth credential " +
                            "is being printed to the logging stream, exposing it in log aggregators.",
                            "log.debug(\"Auth validated for subject: {}\", sanitize(subject));"));
                }

                // JWT claims without signature verification
                if (raw.startsWith("+") && !raw.startsWith("+++")
                        && (line.contains("getClaims") || line.contains("parseJwt") || line.contains("decodeJwt"))
                        && !line.contains("verifySignature") && !line.contains("verify(")) {
                    items.add(new FeedbackItemDto(
                            lineNum, "SECURITY", "WARNING",
                            "JWT decoded without signature verification: Claims are extracted from the token " +
                            "without validating the cryptographic signature — forged tokens would be accepted.",
                            "Jwts.parserBuilder()\n    .setSigningKey(signingKey)\n    .build()\n    .parseClaimsJws(token)\n    .getBody();"));
                }

                // TODO/FIXME in added lines
                if (raw.startsWith("+") && !raw.startsWith("+++")
                        && (line.contains("TODO") || line.contains("FIXME") || line.contains("HACK"))) {
                    items.add(new FeedbackItemDto(
                            lineNum, "READABILITY", "INFO",
                            "Unresolved TODO/FIXME marker committed: Technical debt note is being merged " +
                            "into the branch. Create a tracked issue instead of leaving inline markers.",
                            "// Replace with: // See: PROJECT-1234 or link to issue tracker"));
                }

                // Large hunk (>50 added lines)
                if (raw.startsWith("@@")) {
                    Matcher hunkMatcher = Pattern.compile("\\+(\\d+),(\\d+)").matcher(raw);
                    if (hunkMatcher.find()) {
                        int addedLines = Integer.parseInt(hunkMatcher.group(2));
                        if (addedLines > 50) {
                            items.add(new FeedbackItemDto(
                                    lineNum, "READABILITY", "WARNING",
                                    String.format("Large change block: This diff hunk adds %d lines in a single " +
                                            "commit. Consider splitting into smaller, focused commits for easier " +
                                            "review, targeted rollback, and bisect-friendly history.", addedLines),
                                    null));
                        }
                    }
                }
            }
        }

        // De-duplicate findings by (lineNumber, category) key to prevent repeated warnings on same line
        Map<String, FeedbackItemDto> deduped = new LinkedHashMap<>();
        for (FeedbackItemDto item : items) {
            String key = item.lineNumber() + "|" + item.category();
            deduped.putIfAbsent(key, item);
        }
        List<FeedbackItemDto> result = new ArrayList<>(deduped.values());

        // General fallback when no specific patterns matched
        if (result.isEmpty()) {
            result.add(new FeedbackItemDto(
                    1,
                    "READABILITY",
                    "INFO",
                    "No critical issues detected by static pattern analysis. " +
                    "Ensure comprehensive unit test coverage, document all public API contracts " +
                    "with Javadoc/docstrings, and validate all external inputs at service boundaries.",
                    null
            ));
        }

        log.info("Offline simulation produced {} finding(s) for language={}", result.size(), lang);
        return result;
    }
}
