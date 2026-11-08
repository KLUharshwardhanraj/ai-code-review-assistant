import React from 'react';
import { Code2, Sparkles, History, FileText, CheckCircle2, AlertTriangle, XCircle } from 'lucide-react';

export default function Header({ 
  onNewReview, 
  onOpenHistory, 
  currentSession, 
  healthStatus 
}) {
  const getHealthBadge = () => {
    if (!healthStatus) {
      return (
        <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-slate-800/60 border-slate-700/50 text-slate-400">
          <span className="w-1.5 h-1.5 rounded-full bg-slate-500 animate-pulse"></span>
          <span>Connecting...</span>
        </div>
      );
    }
    const isUp = healthStatus.status === 'UP';
    const isLive = healthStatus.components?.geminiApi?.details?.apiKeyConfigured
      ?? healthStatus.details?.apiKeyConfigured
      ?? healthStatus.apiKeyConfigured
      ?? false;

    if (!isUp) {
      return (
        <div 
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-rose-950/60 border-rose-500/30 text-rose-300"
          title={`Backend status: ${healthStatus.error || healthStatus.status}`}
        >
          <span className="w-1.5 h-1.5 rounded-full bg-rose-400"></span>
          <span>Backend Offline</span>
        </div>
      );
    }

    if (isLive) {
      return (
        <div 
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-emerald-950/60 border-emerald-500/30 text-emerald-300"
          title="Connected to Google Gemini 2.5 Flash API"
        >
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
          <span>Gemini 2.5 Flash Live</span>
        </div>
      );
    }

    return (
      <div 
        className="flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-sky-950/60 border-sky-500/30 text-sky-300"
        title="Backend online in intelligent simulation mode. Set GEMINI_API_KEY environment variable for live Gemini LLM calls."
      >
        <span className="w-1.5 h-1.5 rounded-full bg-sky-400 animate-pulse"></span>
        <span>Backend Online (Simulation)</span>
      </div>
    );
  };

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        
        {/* Brand */}
        <div className="flex items-center gap-3 cursor-pointer" onClick={onNewReview}>
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center shadow-lg shadow-indigo-500/20">
            <Code2 className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
                AI Code Review Assistant
              </h1>
              <span className="text-[10px] font-semibold tracking-wider uppercase px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                PROD
              </span>
            </div>
            <p className="text-xs text-slate-400">Human-in-the-Loop Gemini Code Review</p>
          </div>
        </div>

        {/* Center / Health Status */}
        <div className="hidden md:flex items-center gap-2">
          {getHealthBadge()}
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2 sm:gap-3">
          <button
            onClick={onNewReview}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all ${
              !currentSession
                ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                : 'text-slate-300 hover:text-white hover:bg-slate-800'
            }`}
          >
            <Sparkles className="w-4 h-4 text-indigo-300" />
            <span>New Review</span>
          </button>

          <button
            onClick={onOpenHistory}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-700/60 transition-all"
            title="View Previous Review Sessions"
          >
            <History className="w-4 h-4 text-slate-400" />
            <span className="hidden sm:inline">History</span>
          </button>

          <a
            href="http://localhost:8080/swagger-ui.html"
            target="_blank"
            rel="noopener noreferrer"
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium text-slate-300 hover:text-white hover:bg-slate-800 border border-slate-700/60 transition-all"
            title="OpenAPI / Swagger 3 UI"
          >
            <FileText className="w-4 h-4 text-slate-400" />
            <span className="hidden sm:inline">Swagger UI</span>
          </a>
        </div>

      </div>
    </header>
  );
}
