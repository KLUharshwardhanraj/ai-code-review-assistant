import React, { useState, useMemo } from 'react';
import FeedbackCard from './FeedbackCard';
import { Filter, Search, CheckCheck } from 'lucide-react';

export default function FeedbackList({ 
  feedbacks = [], 
  onDecision = () => {},
  selectedLine = null,
  onSelectLine = () => {}
}) {
  const [statusFilter, setStatusFilter] = useState('ALL'); // 'ALL' | 'PENDING' | 'ACCEPTED' | 'REJECTED'
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Extract unique categories from feedbacks
  const categories = useMemo(() => {
    const set = new Set();
    feedbacks.forEach(f => {
      if (f.category) set.add(f.category.toUpperCase());
    });
    return Array.from(set);
  }, [feedbacks]);

  // Filtered list
  const filteredFeedbacks = useMemo(() => {
    return feedbacks.filter(item => {
      const itemStatus = (item.status || 'PENDING').toUpperCase();
      const itemCat = (item.category || 'OTHER').toUpperCase();
      const text = `${item.suggestion || ''} ${item.description || ''} ${item.title || ''}`.toLowerCase();

      // Status filter
      if (statusFilter !== 'ALL' && itemStatus !== statusFilter) {
        return false;
      }

      // Category filter
      if (categoryFilter !== 'ALL' && itemCat !== categoryFilter) {
        return false;
      }

      // Search query
      if (searchQuery.trim() && !text.includes(searchQuery.toLowerCase())) {
        return false;
      }

      return true;
    });
  }, [feedbacks, statusFilter, categoryFilter, searchQuery]);

  return (
    <div className="flex flex-col h-full">
      
      {/* Filters & Search Header */}
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-3 mb-4 space-y-3">
        
        {/* Status Tabs & Search */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
          
          {/* Status Tabs */}
          <div className="flex items-center gap-1 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs font-medium">
            {['ALL', 'PENDING', 'ACCEPTED', 'REJECTED'].map((st) => (
              <button
                key={st}
                onClick={() => setStatusFilter(st)}
                className={`px-2.5 py-1 rounded-md transition-colors ${
                  statusFilter === st
                    ? 'bg-indigo-600 text-white font-semibold'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                {st}
              </button>
            ))}
          </div>

          {/* Search Input */}
          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-500 absolute left-2.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Search findings..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg pl-8 pr-3 py-1 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-indigo-500 w-full sm:w-44"
            />
          </div>

        </div>

        {/* Category Pills (if more than 1 category) */}
        {categories.length > 0 && (
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 text-xs">
            <span className="text-slate-500 flex items-center gap-1 text-[11px] font-medium mr-1">
              <Filter className="w-3 h-3" />
              Category:
            </span>
            <button
              onClick={() => setCategoryFilter('ALL')}
              className={`px-2 py-0.5 rounded-full border text-[11px] whitespace-nowrap transition-colors ${
                categoryFilter === 'ALL'
                  ? 'bg-slate-700 border-slate-500 text-white font-medium'
                  : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
              }`}
            >
              All
            </button>
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setCategoryFilter(cat)}
                className={`px-2 py-0.5 rounded-full border text-[11px] whitespace-nowrap transition-colors ${
                  categoryFilter === cat
                    ? 'bg-indigo-600 border-indigo-400 text-white font-medium'
                    : 'bg-slate-950 border-slate-800 text-slate-400 hover:text-slate-200'
                }`}
              >
                {cat}
              </button>
            ))}
          </div>
        )}

      </div>

      {/* Cards List or Empty View */}
      <div className="space-y-3 overflow-y-auto pr-1 flex-1">
        {filteredFeedbacks.length === 0 ? (
          <div className="text-center py-12 px-4 border border-dashed border-slate-800 rounded-xl bg-slate-900/30">
            <CheckCheck className="w-8 h-8 text-emerald-500/80 mx-auto mb-2" />
            <h3 className="text-sm font-semibold text-slate-300 mb-1">
              No review feedback matches the selected filters
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Try switching status tabs to 'ALL' or clear the category and search filters.
            </p>
          </div>
        ) : (
          filteredFeedbacks.map((item) => (
            <FeedbackCard
              key={item.id}
              feedback={item}
              onDecision={onDecision}
              onSelectLine={onSelectLine}
              isHighlighted={selectedLine != null && (item.line_number === selectedLine || item.lineNumber === selectedLine)}
            />
          ))
        )}
      </div>

    </div>
  );
}
