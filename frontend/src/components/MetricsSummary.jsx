import React from 'react';
import { CheckCircle2, XCircle, Clock, BarChart3 } from 'lucide-react';

export default function MetricsSummary({ total = 0, accepted = 0, rejected = 0, pending = 0 }) {
  const decided = accepted + rejected;
  const acceptanceRate = decided > 0 ? Math.round((accepted / decided) * 100) : 0;
  const progressPercent = total > 0 ? Math.round((decided / total) * 100) : 0;

  return (
    <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-4 shadow-sm mb-6">
      
      {/* Cards Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-4">
        
        {/* Total Suggestions */}
        <div className="bg-slate-950/60 border border-slate-800/80 rounded-lg p-3">
          <div className="flex items-center justify-between text-xs font-medium text-slate-400 mb-1">
            <span>Total Findings</span>
            <BarChart3 className="w-3.5 h-3.5 text-slate-500" />
          </div>
          <div className="text-xl font-bold text-white">{total}</div>
        </div>

        {/* Accepted Decisions */}
        <div className="bg-emerald-950/20 border border-emerald-900/30 rounded-lg p-3">
          <div className="flex items-center justify-between text-xs font-medium text-emerald-400 mb-1">
            <span>Accepted</span>
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
          </div>
          <div className="text-xl font-bold text-emerald-300">{accepted}</div>
        </div>

        {/* Rejected Decisions */}
        <div className="bg-rose-950/20 border border-rose-900/30 rounded-lg p-3">
          <div className="flex items-center justify-between text-xs font-medium text-rose-400 mb-1">
            <span>Rejected</span>
            <XCircle className="w-3.5 h-3.5 text-rose-400" />
          </div>
          <div className="text-xl font-bold text-rose-300">{rejected}</div>
        </div>

        {/* Pending Decisions */}
        <div className="bg-amber-950/20 border border-amber-900/30 rounded-lg p-3">
          <div className="flex items-center justify-between text-xs font-medium text-amber-400 mb-1">
            <span>Pending Review</span>
            <Clock className="w-3.5 h-3.5 text-amber-400" />
          </div>
          <div className="text-xl font-bold text-amber-300">{pending}</div>
        </div>

      </div>

      {/* Progress & Acceptance Rate */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="text-slate-400">
            Review Progress: <strong className="text-slate-200">{decided} of {total}</strong> decided ({progressPercent}%)
          </span>
          <span className="text-slate-400">
            Acceptance Rate: <strong className="text-emerald-400">{acceptanceRate}%</strong>
          </span>
        </div>

        <div className="h-2 w-full bg-slate-800 rounded-full overflow-hidden flex">
          <div 
            className="bg-emerald-500 h-full transition-all duration-500"
            style={{ width: `${total > 0 ? (accepted / total) * 100 : 0}%` }}
            title={`Accepted: ${accepted}`}
          />
          <div 
            className="bg-rose-500 h-full transition-all duration-500"
            style={{ width: `${total > 0 ? (rejected / total) * 100 : 0}%` }}
            title={`Rejected: ${rejected}`}
          />
          <div 
            className="bg-amber-500/50 h-full transition-all duration-500"
            style={{ width: `${total > 0 ? (pending / total) * 100 : 0}%` }}
            title={`Pending: ${pending}`}
          />
        </div>
      </div>

    </div>
  );
}
