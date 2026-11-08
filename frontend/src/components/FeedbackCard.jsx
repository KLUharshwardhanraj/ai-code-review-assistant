import React, { useState } from 'react';
import { 
  CheckCircle2, 
  XCircle, 
  AlertTriangle, 
  ShieldAlert, 
  Bug, 
  Eye, 
  Zap, 
  Code, 
  Copy, 
  Check, 
  ArrowRight,
  HelpCircle,
  Loader2
} from 'lucide-react';

export default function FeedbackCard({ 
  feedback, 
  onDecision = () => {}, 
  onSelectLine = () => {},
  isHighlighted = false 
}) {
  const [copied, setCopied] = useState(false);
  const [loadingDecision, setLoadingDecision] = useState(null); // 'ACCEPTED' | 'REJECTED' | null

  const id = feedback.id;
  const lineNumber = feedback.line_number || feedback.lineNumber;
  const category = (feedback.category || 'OTHER').toUpperCase();
  const severity = (feedback.severity || 'INFO').toUpperCase();
  const status = (feedback.status || 'PENDING').toUpperCase();
  const suggestion = feedback.suggestion || feedback.description || '';
  const replacementCode = feedback.replacement_code || feedback.replacementCode || feedback.suggested_code;

  const handleDecisionClick = async (decision) => {
    if (loadingDecision) return;
    setLoadingDecision(decision);
    try {
      await onDecision(id, decision);
    } finally {
      setLoadingDecision(null);
    }
  };

  const handleCopyCode = () => {
    if (!replacementCode) return;
    navigator.clipboard.writeText(replacementCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  // Category Icon & Styling
  const getCategoryMeta = () => {
    switch (category) {
      case 'BUG':
        return {
          icon: <Bug className="w-3.5 h-3.5" />,
          color: 'text-rose-400 bg-rose-500/10 border-rose-500/30'
        };
      case 'SECURITY':
        return {
          icon: <ShieldAlert className="w-3.5 h-3.5" />,
          color: 'text-purple-400 bg-purple-500/10 border-purple-500/30'
        };
      case 'EDGE_CASE':
        return {
          icon: <AlertTriangle className="w-3.5 h-3.5" />,
          color: 'text-amber-400 bg-amber-500/10 border-amber-500/30'
        };
      case 'READABILITY':
        return {
          icon: <Eye className="w-3.5 h-3.5" />,
          color: 'text-sky-400 bg-sky-500/10 border-sky-500/30'
        };
      case 'PERFORMANCE':
        return {
          icon: <Zap className="w-3.5 h-3.5" />,
          color: 'text-emerald-400 bg-emerald-500/10 border-emerald-500/30'
        };
      case 'SYSTEM':
        return {
          icon: <AlertTriangle className="w-3.5 h-3.5" />,
          color: 'text-orange-400 bg-orange-500/10 border-orange-500/30'
        };
      default:
        return {
          icon: <HelpCircle className="w-3.5 h-3.5" />,
          color: 'text-slate-400 bg-slate-500/10 border-slate-500/30'
        };
    }
  };

  // Severity Badge Styling
  const getSeverityBadge = () => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-red-500/20 text-red-300 border-red-500/40 font-bold';
      case 'HIGH':
      case 'WARNING':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/40 font-semibold';
      case 'MEDIUM':
        return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
      case 'LOW':
      case 'INFO':
      default:
        return 'bg-slate-700/40 text-slate-300 border-slate-600/40';
    }
  };

  const catMeta = getCategoryMeta();

  return (
    <div 
      className={`border rounded-xl p-4 transition-all duration-200 ${
        isHighlighted
          ? 'bg-slate-900 border-indigo-500 ring-2 ring-indigo-500/30 shadow-lg'
          : status === 'ACCEPTED'
            ? 'bg-slate-900/60 border-emerald-900/40'
            : status === 'REJECTED'
              ? 'bg-slate-950/40 border-slate-800 opacity-70'
              : 'bg-slate-900/80 border-slate-800 hover:border-slate-700'
      }`}
    >
      {/* Header Row: Category, Severity, Line, Status */}
      <div className="flex items-center justify-between gap-2 flex-wrap mb-3">
        <div className="flex items-center gap-1.5 flex-wrap">
          
          {/* Category Pill */}
          <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-semibold border ${catMeta.color}`}>
            {catMeta.icon}
            <span>{category}</span>
          </span>

          {/* Severity Pill */}
          <span className={`px-2 py-0.5 rounded-md text-[11px] uppercase tracking-wide border ${getSeverityBadge()}`}>
            {severity}
          </span>

          {/* Line reference pill */}
          {lineNumber != null && (
            <button
              onClick={() => onSelectLine(lineNumber)}
              className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-mono font-medium bg-slate-800 hover:bg-slate-700 text-indigo-300 border border-slate-700 transition-colors"
              title="Click to view line in code viewer"
            >
              <Code className="w-3 h-3 text-indigo-400" />
              <span>Line {lineNumber}</span>
            </button>
          )}

        </div>

        {/* Current Decision Pill */}
        <div>
          {status === 'ACCEPTED' && (
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-emerald-400 bg-emerald-950/60 border border-emerald-500/30 px-2 py-0.5 rounded">
              <CheckCircle2 className="w-3 h-3" />
              Accepted
            </span>
          )}
          {status === 'REJECTED' && (
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-rose-400 bg-rose-950/60 border border-rose-500/30 px-2 py-0.5 rounded">
              <XCircle className="w-3 h-3" />
              Rejected
            </span>
          )}
          {status === 'PENDING' && (
            <span className="text-[11px] font-medium text-amber-400/80 bg-amber-950/30 border border-amber-500/20 px-2 py-0.5 rounded">
              Awaiting Review
            </span>
          )}
        </div>
      </div>

      {/* Suggestion Body */}
      <div className="text-sm text-slate-200 leading-relaxed mb-3 whitespace-pre-line">
        {suggestion}
      </div>

      {/* Suggested Code Replacement Box (if available) */}
      {replacementCode && (
        <div className="mb-4 bg-slate-950 border border-slate-800 rounded-lg overflow-hidden">
          <div className="bg-slate-900/90 px-3 py-1.5 border-b border-slate-800/80 flex items-center justify-between text-xs text-slate-400 font-mono">
            <span className="flex items-center gap-1 text-indigo-300">
              <ArrowRight className="w-3 h-3" />
              Suggested Replacement:
            </span>
            <button
              onClick={handleCopyCode}
              className="flex items-center gap-1 hover:text-white transition-colors"
              title="Copy snippet"
            >
              {copied ? (
                <>
                  <Check className="w-3 h-3 text-emerald-400" />
                  <span className="text-emerald-400">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-3 h-3" />
                  <span>Copy</span>
                </>
              )}
            </button>
          </div>
          <pre className="p-3 text-xs font-mono text-emerald-300 overflow-x-auto bg-slate-950/80">
            <code>{replacementCode}</code>
          </pre>
        </div>
      )}

      {/* Human-in-the-Loop Action Buttons */}
      <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800/80">
        
        {/* Reject Button */}
        <button
          onClick={() => handleDecisionClick('REJECTED')}
          disabled={loadingDecision != null}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition-all ${
            status === 'REJECTED'
              ? 'bg-rose-900/60 text-rose-200 border border-rose-600/50 cursor-default'
              : 'bg-slate-800 text-slate-300 hover:text-rose-300 hover:bg-rose-950/40 border border-slate-700/80 hover:border-rose-500/40'
          }`}
        >
          {loadingDecision === 'REJECTED' ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <XCircle className="w-3.5 h-3.5 text-rose-400" />
          )}
          <span>{status === 'REJECTED' ? 'Rejected' : 'Reject'}</span>
        </button>

        {/* Accept Button */}
        <button
          onClick={() => handleDecisionClick('ACCEPTED')}
          disabled={loadingDecision != null}
          className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-lg text-xs font-semibold transition-all shadow-sm ${
            status === 'ACCEPTED'
              ? 'bg-emerald-600 text-white border border-emerald-500 cursor-default shadow-emerald-900/30'
              : 'bg-indigo-600 hover:bg-emerald-600 text-white hover:shadow-emerald-600/20'
          }`}
        >
          {loadingDecision === 'ACCEPTED' ? (
            <Loader2 className="w-3.5 h-3.5 animate-spin" />
          ) : (
            <CheckCircle2 className="w-3.5 h-3.5" />
          )}
          <span>{status === 'ACCEPTED' ? 'Accepted' : 'Accept Suggestion'}</span>
        </button>

      </div>

    </div>
  );
}
