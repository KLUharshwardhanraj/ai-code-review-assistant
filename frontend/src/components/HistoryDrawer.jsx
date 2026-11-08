import React, { useEffect, useState } from 'react';
import { X, Clock, RefreshCw, Calendar, ChevronRight, FileCode, GitCompare, CheckCircle2, AlertCircle } from 'lucide-react';
import { listRecentSessions } from '../api/reviewApi';

export default function HistoryDrawer({ 
  isOpen = false, 
  onClose = () => {}, 
  onSelectSession = () => {} 
}) {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchHistory = async (pageNumber = 0) => {
    setLoading(true);
    setError('');
    try {
      const data = await listRecentSessions(pageNumber, 10);
      setSessions(data.content || []);
      setTotalPages(data.totalPages || 0);
      setPage(pageNumber);
    } catch (err) {
      setError(err.message || 'Failed to load session history');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      fetchHistory(0);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return <span className="text-[10px] font-bold text-emerald-400 bg-emerald-950/60 border border-emerald-500/30 px-2 py-0.5 rounded">COMPLETED</span>;
      case 'IN_REVIEW':
        return <span className="text-[10px] font-bold text-indigo-400 bg-indigo-950/60 border border-indigo-500/30 px-2 py-0.5 rounded">IN REVIEW</span>;
      case 'FAILED':
        return <span className="text-[10px] font-bold text-rose-400 bg-rose-950/60 border border-rose-500/30 px-2 py-0.5 rounded">FAILED</span>;
      default:
        return <span className="text-[10px] font-bold text-slate-400 bg-slate-800 px-2 py-0.5 rounded">PENDING</span>;
    }
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div 
        className="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />

      {/* Drawer Panel */}
      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-slate-900 border-l border-slate-800 flex flex-col shadow-2xl">
          
          {/* Header */}
          <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/80">
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-400" />
              <h3 className="text-base font-bold text-white">Review History</h3>
            </div>
            
            <div className="flex items-center gap-2">
              <button
                onClick={() => fetchHistory(page)}
                disabled={loading}
                className="p-1.5 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition-colors"
                title="Refresh history"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
              <button
                onClick={onClose}
                className="p-1.5 text-slate-400 hover:text-white rounded hover:bg-slate-800 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {error && (
              <div className="bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs p-3 rounded-lg flex items-center gap-2">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {loading && sessions.length === 0 ? (
              <div className="py-12 text-center text-xs text-slate-400">
                <RefreshCw className="w-6 h-6 animate-spin mx-auto text-indigo-400 mb-2" />
                Loading sessions...
              </div>
            ) : sessions.length === 0 ? (
              <div className="py-12 text-center text-xs text-slate-400">
                No review sessions found. Submit a review to get started!
              </div>
            ) : (
              sessions.map((item) => {
                const id = item.session_id || item.sessionId;
                const total = item.total_feedback_count != null ? item.total_feedback_count : (item.totalFeedbackCount || 0);
                const accepted = item.accepted_count != null ? item.accepted_count : (item.acceptedCount || 0);
                const rejected = item.rejected_count != null ? item.rejected_count : (item.rejectedCount || 0);

                return (
                  <div
                    key={id}
                    onClick={() => {
                      onSelectSession(id);
                      onClose();
                    }}
                    className="bg-slate-950/70 border border-slate-800/80 hover:border-indigo-500/60 rounded-xl p-3.5 cursor-pointer transition-all hover:bg-slate-800/40 group"
                  >
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <h4 className="text-sm font-semibold text-white group-hover:text-indigo-300 truncate">
                        {item.title || `Session #${id}`}
                      </h4>
                      {getStatusBadge(item.status)}
                    </div>

                    <div className="flex items-center justify-between text-xs text-slate-400">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-indigo-400 uppercase text-[11px] bg-indigo-950/60 px-1.5 py-0.5 rounded border border-indigo-500/20">
                          {item.language}
                        </span>
                        <span className="text-slate-500">•</span>
                        <span>{total} findings</span>
                        {total > 0 && (
                          <span className="text-[11px] text-emerald-400">
                            ({accepted} accepted)
                          </span>
                        )}
                      </div>

                      <div className="flex items-center gap-1 text-[11px] text-slate-500">
                        <Calendar className="w-3 h-3" />
                        <span>{formatDate(item.created_at || item.createdAt)}</span>
                        <ChevronRight className="w-3.5 h-3.5 text-slate-600 group-hover:text-indigo-400 ml-1 transition-transform group-hover:translate-x-0.5" />
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Footer Pagination */}
          {totalPages > 1 && (
            <div className="p-3 border-t border-slate-800 bg-slate-950/80 flex items-center justify-between text-xs text-slate-400">
              <button
                disabled={page <= 0}
                onClick={() => fetchHistory(page - 1)}
                className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40"
              >
                Previous
              </button>
              <span>Page {page + 1} of {totalPages}</span>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => fetchHistory(page + 1)}
                className="px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 disabled:opacity-40"
              >
                Next
              </button>
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
