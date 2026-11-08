import React, { useRef, useEffect, useState } from 'react';
import { Copy, Check, FileCode } from 'lucide-react';

export default function CodeViewer({ 
  code = '', 
  language = 'text', 
  submissionType = 'SNIPPET',
  highlightedLine = null,
  feedbacks = [],
  onLineClick = () => {}
}) {
  const [copied, setCopied] = useState(false);
  const lineRefs = useRef({});

  const lines = code ? code.split('\n') : [];
  const isDiff = submissionType === 'DIFF' || language === 'diff';

  // Map of lineNumber -> array of feedback items targeting that line
  const feedbackByLine = React.useMemo(() => {
    const map = {};
    feedbacks.forEach(f => {
      const line = f.line_number || f.lineNumber;
      if (line != null) {
        if (!map[line]) map[line] = [];
        map[line].push(f);
      }
    });
    return map;
  }, [feedbacks]);

  // Scroll to highlighted line when selected from feedback card
  useEffect(() => {
    if (highlightedLine && lineRefs.current[highlightedLine]) {
      lineRefs.current[highlightedLine].scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    }
  }, [highlightedLine]);

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="bg-slate-950 border border-slate-800 rounded-xl overflow-hidden shadow-sm flex flex-col h-full">
      
      {/* Code Header Bar */}
      <div className="bg-slate-900/90 px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <FileCode className="w-4 h-4 text-indigo-400" />
          <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider">
            {language} {isDiff ? '(Diff View)' : 'Source'}
          </span>
          <span className="text-xs text-slate-500">
            • {lines.length} lines
          </span>
        </div>

        <button
          onClick={handleCopy}
          className="flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium text-slate-400 hover:text-white bg-slate-800/80 hover:bg-slate-700/80 rounded border border-slate-700/50 transition-colors"
          title="Copy raw code"
        >
          {copied ? (
            <>
              <Check className="w-3.5 h-3.5 text-emerald-400" />
              <span className="text-emerald-300">Copied</span>
            </>
          ) : (
            <>
              <Copy className="w-3.5 h-3.5" />
              <span>Copy</span>
            </>
          )}
        </button>
      </div>

      {/* Code Body with Line Numbers */}
      <div className="overflow-auto flex-1 font-mono text-xs sm:text-[13px] leading-relaxed select-text p-2">
        <table className="w-full border-collapse">
          <tbody>
            {lines.map((lineContent, index) => {
              const lineNumber = index + 1;
              const isHighlighted = highlightedLine === lineNumber;
              const lineFeedbacks = feedbackByLine[lineNumber] || [];
              const hasFeedback = lineFeedbacks.length > 0;

              let diffClass = '';
              if (isDiff) {
                if (lineContent.startsWith('+') && !lineContent.startsWith('+++')) {
                  diffClass = 'diff-add';
                } else if (lineContent.startsWith('-') && !lineContent.startsWith('---')) {
                  diffClass = 'diff-remove';
                }
              }

              return (
                <tr
                  key={lineNumber}
                  ref={el => (lineRefs.current[lineNumber] = el)}
                  onClick={() => onLineClick(lineNumber)}
                  className={`transition-colors duration-150 cursor-pointer group ${
                    isHighlighted 
                      ? 'highlight-line' 
                      : hasFeedback 
                        ? 'bg-indigo-950/20 hover:bg-indigo-950/30' 
                        : 'hover:bg-slate-900/60'
                  } ${diffClass}`}
                >
                  {/* Line Number Column */}
                  <td className="w-12 text-right pr-3 select-none text-slate-600 group-hover:text-slate-400 font-mono text-xs border-r border-slate-800/80 align-top py-0.5">
                    {lineNumber}
                  </td>

                  {/* Feedback Indicator Pill */}
                  <td className="w-6 px-1 text-center select-none align-middle py-0.5">
                    {hasFeedback && (
                      <span 
                        className="inline-block w-2 h-2 rounded-full bg-amber-400 ring-2 ring-amber-400/30 animate-pulse" 
                        title={`${lineFeedbacks.length} AI suggestion(s) on line ${lineNumber}`}
                      />
                    )}
                  </td>

                  {/* Code Line Content */}
                  <td className="pl-3 pr-4 whitespace-pre font-mono py-0.5 text-slate-200">
                    {lineContent || ' '}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

    </div>
  );
}
