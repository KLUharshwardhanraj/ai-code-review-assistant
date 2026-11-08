import axios from 'axios';

// Default to backend base URL
const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1/reviews';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 35000, // 35 seconds to accommodate Gemini LLM inference
});

/**
 * Submits code snippet or Git unified diff for AI code review.
 * @param {Object} data { code, language, submissionType, title }
 * @returns {Promise<Object>} ReviewSessionResponse
 */
export async function submitCodeForReview(data) {
  const response = await apiClient.post('/submit', {
    code: data.code,
    language: data.language,
    submission_type: data.submissionType || 'SNIPPET',
    title: data.title || undefined,
  });
  return response.data;
}

/**
 * Updates human review decision (ACCEPTED or REJECTED) for a feedback item.
 * @param {number|string} feedbackId
 * @param {'ACCEPTED'|'REJECTED'} status
 * @returns {Promise<Object>} ReviewFeedbackResponse
 */
export async function updateFeedbackDecision(feedbackId, status) {
  const response = await apiClient.patch(`/feedback/${feedbackId}/decision`, {
    status: status,
  });
  return response.data;
}

/**
 * Retrieves a full review session with all feedback items.
 * @param {number|string} sessionId
 * @returns {Promise<Object>} ReviewSessionResponse
 */
export async function getSessionById(sessionId) {
  const response = await apiClient.get(`/sessions/${sessionId}`);
  return response.data;
}

/**
 * Fetches paginated history of previous review sessions.
 * @param {number} page
 * @param {number} size
 * @returns {Promise<Object>} Page of ReviewSessionSummaryResponse
 */
export async function listRecentSessions(page = 0, size = 10) {
  const response = await apiClient.get('/sessions', {
    params: { page, size },
  });
  return response.data;
}

/**
 * Checks system health via Spring Boot Actuator.
 * @returns {Promise<Object>}
 */
export async function checkBackendHealth() {
  try {
    const response = await axios.get('/actuator/health', {
      timeout: 4000,
    });
    return response.data;
  } catch (error) {
    try {
      const directResponse = await axios.get('http://localhost:8080/actuator/health', {
        timeout: 4000,
      });
      return directResponse.data;
    } catch (e) {
      return { status: 'DOWN', error: error.message };
    }
  }
}
