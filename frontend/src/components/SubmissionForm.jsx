import React, { useState } from 'react';
import { Send, Sparkles, FileCode, GitCompare, Code2, AlertCircle, Loader2 } from 'lucide-react';
import { CODE_SAMPLES } from '../utils/samples';

const SUPPORTED_LANGUAGES = [
  { id: 'java', label: 'Java' },
  { id: 'python', label: 'Python' },
  { id: 'typescript', label: 'TypeScript' },
  { id: 'javascript', label: 'JavaScript' },
  { id: 'go', label: 'Go' },
  { id: 'rust', label: 'Rust' },
  { id: 'cpp', label: 'C++' },
  { id: 'csharp', label: 'C#' },
  { id: 'sql', label: 'SQL' },
];

export default function SubmissionForm({ onSubmit = () => {}, isLoading = false }) {
  const [code, setCode] = useState('');
  const [language, setLanguage] = useState('java');
  const [submissionType, setSubmissionType] = useState('SNIPPET');
  const [title, setTitle] = useState('');
  const [validationError, setValidationError] = useState('');

  const charCount = code.length;
  const isOverLimit = charCount > 50000;

  const handleSampleLoad = (sample) => {
    setCode(sample.code);
    setLanguage(sample.language);
    setSubmissionType(sample.type);
    setTitle(sample.title);
    setValidationError('');
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!code.trim()) {
      setValidationError('Please provide code or a Git diff before submitting.');
      return;
    }
    if (isOverLimit) {
      setValidationError('Code exceeds the 50,000 character limit.');
      return;
    }
    setValidationError('');
    onSubmit({
      code,
      language,
      submissionType,
      title: title.trim() || undefined,
    });
  };

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 sm:px-6">
      
      {/* Intro Hero */}
      <div className="text-center mb-8">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-400 text-xs font-semibold uppercase tracking-wider mb-3">
          <Sparkles className="w-3.5 h-3.5" />
          Powered by Google Gemini 2.5 Flash
        </div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight sm:text-4xl">
          Automated Code Review with Human-in-the-Loop
        </h2>
        <p className="mt-3 text-sm sm:text-base text-slate-400 max-w-2xl mx-auto">
          Submit your source code or unified Git diff. Gemini analyzes bugs, security flaws, edge cases, and readability, returning actionable suggestions that you accept or reject.
        </p>
      </div>

      {/* Preset Quick Load Samples */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 mb-6">
        <span className="text-xs font-semibold text-slate-400 block mb-2 uppercase tracking-wider">
          Quick Load Test Samples:
        </span>
        <div className="flex items-center gap-2 flex-wrap">
          {CODE_SAMPLES.map((sample, idx) => (
            <button
              key={idx}
              type="button"
              onClick={() => handleSampleLoad(sample)}
              className="px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 transition-colors flex items-center gap-1.5"
            >
              <Code2 className="w-3.5 h-3.5 text-indigo-400" />
              <span>{sample.name}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Main Submission Form */}
      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-xl p-6 shadow-xl space-y-5">
        
        {/* Validation Error Banner */}
        {validationError && (
          <div className="bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs sm:text-sm p-3 rounded-lg flex items-center gap-2">
            <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-400" />
            <span>{validationError}</span>
          </div>
        )}

        {/* Top Controls: Type Switcher, Language & Title */}
        <div className="grid grid-cols-1 sm:grid-cols-12 gap-4">
          
          {/* Submission Type Switcher */}
          <div className="sm:col-span-4">
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Submission Mode
            </label>
            <div className="grid grid-cols-2 gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs font-medium">
              <button
                type="button"
                onClick={() => setSubmissionType('SNIPPET')}
                className={`flex items-center justify-center gap-1 py-1.5 rounded-md transition-colors ${
                  submissionType === 'SNIPPET'
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <FileCode className="w-3.5 h-3.5" />
                <span>Code Snippet</span>
              </button>

              <button
                type="button"
                onClick={() => setSubmissionType('DIFF')}
                className={`flex items-center justify-center gap-1 py-1.5 rounded-md transition-colors ${
                  submissionType === 'DIFF'
                    ? 'bg-indigo-600 text-white shadow-sm'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                <GitCompare className="w-3.5 h-3.5" />
                <span>Git Diff</span>
              </button>
            </div>
          </div>

          {/* Language Selector */}
          <div className="sm:col-span-3">
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Language
            </label>
            <select
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs sm:text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
            >
              {SUPPORTED_LANGUAGES.map((lang) => (
                <option key={lang.id} value={lang.id}>
                  {lang.label}
                </option>
              ))}
            </select>
          </div>

          {/* Optional Title Input */}
          <div className="sm:col-span-5">
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Session Title <span className="text-slate-500 font-normal">(optional)</span>
            </label>
            <input
              type="text"
              placeholder="e.g. Auth Token Controller Review"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-1.5 text-xs sm:text-sm text-slate-200 placeholder-slate-600 focus:outline-none focus:border-indigo-500"
            />
          </div>

        </div>

        {/* Code Input Area */}
        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="text-xs font-semibold text-slate-300">
              {submissionType === 'DIFF' ? 'Git Unified Diff Content' : 'Source Code Snippet'}
            </label>
            <span className={`text-[11px] font-mono ${isOverLimit ? 'text-rose-400 font-bold' : 'text-slate-500'}`}>
              {charCount.toLocaleString()} / 50,000 characters
            </span>
          </div>

          <textarea
            rows={14}
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              if (validationError) setValidationError('');
            }}
            placeholder={
              submissionType === 'DIFF'
                ? 'Paste unified diff content (e.g. git diff output)...\n--- a/file.js\n+++ b/file.js\n@@ -1,3 +1,3 @@\n-old code\n+new code'
                : 'Paste source code snippet here...'
            }
            className="w-full bg-slate-950 border border-slate-800 rounded-lg p-3 text-xs sm:text-sm font-mono text-slate-200 placeholder-slate-700 focus:outline-none focus:border-indigo-500 resize-y leading-relaxed"
            disabled={isLoading}
          />
        </div>

        {/* Submit Button */}
        <div className="flex items-center justify-between pt-2">
          <p className="text-xs text-slate-500">
            AI suggestions will be saved as draft review cards for human approval.
          </p>

          <button
            type="submit"
            disabled={isLoading || !code.trim() || isOverLimit}
            className="flex items-center gap-2 px-6 py-2.5 rounded-lg text-sm font-bold bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
          >
            {isLoading ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin text-white" />
                <span>Analyzing Code with Gemini...</span>
              </>
            ) : (
              <>
                <Send className="w-4 h-4" />
                <span>Submit for Review</span>
              </>
            )}
          </button>
        </div>

      </form>

    </div>
  );
}
