import React from 'react';
import { Cpu, Clock, Calendar, CheckCircle2, AlertCircle } from 'lucide-react';

export default function SessionHeader({ session }) {
  if (!session) return null;

  const getStatusBadge = (status) => {
    switch (status) {
      case 'COMPLETED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
            <CheckCircle2 className="w-3.5 h-3.5" />
            COMPLETED
          </span>
        );
      case 'IN_REVIEW':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-indigo-500/15 text-indigo-300 border border-indigo-500/30 animate-pulse">
            <span className="w-2 h-2 rounded-full bg-indigo-400"></span>
            IN REVIEW
          </span>
        );
      case 'FAILED':
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-500/15 text-rose-300 border border-rose-500/30">
            <AlertCircle className="w-3.5 h-3.5" />
            FAILED
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-700/50 text-slate-300 border border-slate-600/50">
            PENDING
          </span>
        );
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
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 mb-6 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        
        {/* Title and Metadata */}
        <div>
          <div className="flex items-center gap-2.5 flex-wrap mb-2">
            <h2 className="text-xl font-bold text-white tracking-tight">
              {session.title || 'Code Review Session'}
            </h2>
            {getStatusBadge(session.status)}
          </div>

          <div className="flex items-center gap-3 text-xs text-slate-400 flex-wrap">
            {/* Language */}
            <span className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono text-indigo-300 uppercase">
              {session.language}
            </span>

            {/* Type */}
            <span className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700 font-medium text-slate-300">
              {session.submission_type || session.submissionType || 'SNIPPET'}
            </span>

            {/* AI Model */}
            {(session.gemini_model || session.geminiModel) && (
              <span className="flex items-center gap-1 text-slate-400">
                <Cpu className="w-3.5 h-3.5 text-indigo-400" />
                <span>{session.gemini_model || session.geminiModel}</span>
              </span>
            )}

            {/* Turnaround Time */}
            {(session.gemini_request_duration_ms != null || session.geminiRequestDurationMs != null) && (
              <span className="flex items-center gap-1 text-slate-400">
                <Clock className="w-3.5 h-3.5 text-emerald-400" />
                <span>{session.gemini_request_duration_ms ?? session.geminiRequestDurationMs} ms</span>
              </span>
            )}

            {/* Date */}
            {(session.created_at || session.createdAt) && (
              <span className="flex items-center gap-1 text-slate-500">
                <Calendar className="w-3.5 h-3.5" />
                <span>{formatDate(session.created_at || session.createdAt)}</span>
              </span>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
