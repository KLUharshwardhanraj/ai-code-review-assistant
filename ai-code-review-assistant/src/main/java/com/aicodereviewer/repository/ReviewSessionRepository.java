package com.aicodereviewer.repository;

import com.aicodereviewer.entity.ReviewSession;
import com.aicodereviewer.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReviewSession} entities.
 *
 * <p>Provides standard CRUD operations plus custom query methods for
 * filtering sessions by status, paginating history, and eagerly fetching feedbacks.</p>
 */
@Repository
public interface ReviewSessionRepository extends JpaRepository<ReviewSession, Long> {

    /**
     * Finds all sessions with the given status, ordered by most recent first.
     *
     * @param status the review status to filter by
     * @return list of matching sessions, newest first
     */
    List<ReviewSession> findAllByStatusOrderByCreatedAtDesc(ReviewStatus status);

    /**
     * Finds all sessions ordered by most recent first.
     *
     * @return list of all sessions, newest first
     */
    List<ReviewSession> findAllByOrderByCreatedAtDesc();

    /**
     * Finds sessions ordered by most recent first with pagination support.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @return a page of review sessions
     */
    Page<ReviewSession> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Retrieves a session by its ID with all associated feedback items eagerly loaded in a single query.
     * Prevents lazy initialization exceptions and N+1 queries.
     *
     * @param id session identifier
     * @return optional containing the session with feedback items, or empty
     */
    @Query("SELECT DISTINCT s FROM ReviewSession s LEFT JOIN FETCH s.feedbackItems WHERE s.id = :id")
    Optional<ReviewSession> findByIdWithFeedbacks(@Param("id") Long id);
}
