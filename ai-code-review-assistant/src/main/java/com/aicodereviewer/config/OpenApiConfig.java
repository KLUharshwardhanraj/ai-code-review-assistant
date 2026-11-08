package com.aicodereviewer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / Swagger UI metadata configuration.
 *
 * <p>Once the application is running, access the interactive API docs at:
 * <ul>
 *   <li>Swagger UI: <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a></li>
 *   <li>Raw spec:   <a href="http://localhost:8080/api-docs">/api-docs</a></li>
 * </ul>
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AI Code Review Assistant API",
                version = "1.0.0",
                description = """
                        Production-grade REST API for AI-powered code review.
                        
                        **Workflow:**
                        1. Submit source code or a Git diff via `POST /api/v1/reviews/submit`.
                        2. The backend sends the code to Google Gemini for structured analysis.
                        3. Review each feedback item and accept or reject via `PATCH /api/v1/reviews/feedback/{id}/decision`.
                        4. Retrieve session history and details via the GET endpoints.
                        
                        All feedback decisions follow a human-in-the-loop model — no AI suggestion is applied automatically.
                        """,
                contact = @Contact(
                        name = "AI Code Review Assistant",
                        url = "https://github.com/ai-code-review-assistant"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development server")
        }
)
public class OpenApiConfig {
}
