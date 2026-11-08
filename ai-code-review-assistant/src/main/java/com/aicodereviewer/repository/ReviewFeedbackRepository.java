package com.aicodereviewer.repository;

import com.aicodereviewer.entity.ReviewFeedback;
import com.aicodereviewer.entity.enums.FeedbackResolution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for {@link ReviewFeedback} entities.
 *
 * <p>Provides standard CRUD operations plus custom query methods for
 * retrieving feedback by session and counting by resolution status.</p>
 */
@Repository
public interface ReviewFeedbackRepository extends JpaRepository<ReviewFeedback, Long> {

    /**
     * Finds all feedback items belonging to a specific session, ordered chronologically.
     * Used to populate the review card list in the session detail view.
     *
     * @param sessionId the ID of the parent review session
     * @return list of feedback items, oldest first (in the order Gemini produced them)
     */
    List<ReviewFeedback> findAllBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * Counts feedback items for a session filtered by resolution status.
     * Used to update the denormalized counters on {@link com.aicodereviewer.entity.ReviewSession}
     * and to determine if all items have been resolved (for session completion logic).
     *
     * @param sessionId  the ID of the parent review session
     * @param resolution the resolution status to count
     * @return the count of matching feedback items
     */
    long countBySessionIdAndResolution(Long sessionId, FeedbackResolution resolution);
}
