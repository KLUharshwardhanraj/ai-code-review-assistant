import React, { useState, useEffect } from 'react';
import Header from './components/Header';
import SubmissionForm from './components/SubmissionForm';
import SessionHeader from './components/SessionHeader';
import MetricsSummary from './components/MetricsSummary';
import CodeViewer from './components/CodeViewer';
import FeedbackList from './components/FeedbackList';
import HistoryDrawer from './components/HistoryDrawer';
import { 
  submitCodeForReview, 
  updateFeedbackDecision, 
  getSessionById, 
  checkBackendHealth 
} from './api/reviewApi';
import { AlertCircle, ArrowLeft } from 'lucide-react';

export default function App() {
  const [currentSession, setCurrentSession] = useState(null);
  const [selectedLine, setSelectedLine] = useState(null);
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [healthStatus, setHealthStatus] = useState(null);

  // Check backend health on initial load
  useEffect(() => {
    checkBackendHealth().then((status) => setHealthStatus(status));
    const interval = setInterval(() => {
      checkBackendHealth().then((status) => setHealthStatus(status));
    }, 30000); // Poll every 30 seconds
    return () => clearInterval(interval);
  }, []);

  // Handle Code Submission
  const handleSubmitCode = async (formData) => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const session = await submitCodeForReview(formData);
      setCurrentSession(session);
      setSelectedLine(null);
    } catch (err) {
      const msg = err.response?.data?.message || err.message || 'Failed to submit review request.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Accept / Reject Decision
  const handleDecision = async (feedbackId, status) => {
    try {
      const updatedFeedback = await updateFeedbackDecision(feedbackId, status);
      
      // Update session state locally with the new decision
      setCurrentSession(prev => {
        if (!prev) return prev;
        const updatedFeedbacks = prev.feedbacks.map(f => {
          if (f.id === feedbackId) {
            return {
              ...f,
              status: updatedFeedback.status,
            };
          }
          return f;
        });

        // Recalculate summary metrics
        const total = updatedFeedbacks.length;
        const accepted = updatedFeedbacks.filter(f => f.status === 'ACCEPTED').length;
        const rejected = updatedFeedbacks.filter(f => f.status === 'REJECTED').length;
        const pending = total - (accepted + rejected);
        const sessionStatus = pending === 0 && total > 0 ? 'COMPLETED' : prev.status;

        return {
          ...prev,
          status: sessionStatus,
          feedbacks: updatedFeedbacks,
          total_count: total,
          accepted_count: accepted,
          rejected_count: rejected,
          pending_count: pending,
          totalCount: total,
          acceptedCount: accepted,
          rejectedCount: rejected,
          pendingCount: pending,
        };
      });
    } catch (err) {
      alert(`Failed to update decision: ${err.response?.data?.message || err.message}`);
    }
  };

  // Load a historical review session
  const handleSelectSession = async (sessionId) => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const session = await getSessionById(sessionId);
      setCurrentSession(session);
      setSelectedLine(null);
    } catch (err) {
      setErrorMessage(`Failed to load session #${sessionId}: ${err.message}`);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-indigo-500 selection:text-white">
      
      {/* Top Navbar */}
      <Header
        onNewReview={() => {
          setCurrentSession(null);
          setSelectedLine(null);
          setErrorMessage('');
        }}
        onOpenHistory={() => setIsHistoryOpen(true)}
        currentSession={currentSession}
        healthStatus={healthStatus}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        
        {/* Global Error Banner */}
        {errorMessage && (
          <div className="bg-rose-950/80 border border-rose-500/40 text-rose-200 text-sm p-4 rounded-xl mb-6 flex items-center justify-between shadow-lg">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-5 h-5 text-rose-400 flex-shrink-0" />
              <span>{errorMessage}</span>
            </div>
            <button 
              onClick={() => setErrorMessage('')}
              className="text-xs uppercase tracking-wider font-semibold text-rose-300 hover:text-white ml-4"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* View Switcher: Submission Form vs Session Review */}
        {!currentSession ? (
          <SubmissionForm
            onSubmit={handleSubmitCode}
            isLoading={isLoading}
          />
        ) : (
          <div>
            {/* Back Button */}
            <button
              onClick={() => {
                setCurrentSession(null);
                setSelectedLine(null);
              }}
              className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-400 hover:text-indigo-400 mb-4 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Submit Another Code Review</span>
            </button>

            {/* Session Metadata Header */}
            <SessionHeader session={currentSession} />

            {/* Decision Metrics Summary Bar */}
            <MetricsSummary
              total={currentSession.total_count ?? currentSession.totalCount ?? currentSession.feedbacks?.length ?? 0}
              accepted={currentSession.accepted_count ?? currentSession.acceptedCount ?? 0}
              rejected={currentSession.rejected_count ?? currentSession.rejectedCount ?? 0}
              pending={currentSession.pending_count ?? currentSession.pendingCount ?? 0}
            />

            {/* Split Review Workspace: Left Code / Right Findings */}
            <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
              
              {/* Left Panel: Code Viewer (7 cols on desktop) */}
              <div className="lg:col-span-7 h-[680px] sticky top-20">
                <CodeViewer
                  code={currentSession.code_content || currentSession.codeContent || currentSession.source_code || ''}
                  language={currentSession.language || 'text'}
                  submissionType={currentSession.submission_type || currentSession.submissionType || 'SNIPPET'}
                  highlightedLine={selectedLine}
                  feedbacks={currentSession.feedbacks || []}
                  onLineClick={(line) => setSelectedLine(line)}
                />
              </div>

              {/* Right Panel: Feedback Review Cards (5 cols on desktop) */}
              <div className="lg:col-span-5 h-[680px] flex flex-col">
                <FeedbackList
                  feedbacks={currentSession.feedbacks || []}
                  onDecision={handleDecision}
                  selectedLine={selectedLine}
                  onSelectLine={(line) => setSelectedLine(line)}
                />
              </div>

            </div>

          </div>
        )}

      </main>

      {/* History Drawer */}
      <HistoryDrawer
        isOpen={isHistoryOpen}
        onClose={() => setIsHistoryOpen(false)}
        onSelectSession={handleSelectSession}
      />

    </div>
  );
}
