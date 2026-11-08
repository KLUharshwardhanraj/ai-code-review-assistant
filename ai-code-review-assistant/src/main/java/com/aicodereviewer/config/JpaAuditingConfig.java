package com.aicodereviewer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables JPA Auditing for automatic population of {@code @CreatedDate}
 * and {@code @LastModifiedDate} fields on entity persist and update operations.
 *
 * <p>This is extracted into its own configuration class (rather than placing
 * {@code @EnableJpaAuditing} on the main application class) so that it can
 * be excluded in test slices that don't need auditing (e.g., {@code @DataJpaTest}
 * with a custom configuration).</p>
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
