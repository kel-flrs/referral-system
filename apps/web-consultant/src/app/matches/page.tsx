'use client';

import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { GitCompare } from 'lucide-react';
import { MatchTable } from '@/components/matches/match-table';
import { MatchFilters } from '@/components/matches/match-filters';
import { useMatches, useUpdateMatchStatus, useCreateReferral } from '@/hooks/use-api';
import { Skeleton } from '@/components/ui/skeleton';
import type { Match, MatchStatus } from '@/types';

export default function MatchesPage() {
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<MatchStatus | 'ALL'>('ALL');
  const [minScore, setMinScore] = useState('0');

  const { data, isLoading, error } = useMatches();
  const updateStatus = useUpdateMatchStatus();
  const createReferral = useCreateReferral();

  const matches = data?.data ?? [];

  const filteredMatches = useMemo(() => {
    return matches.filter((match) => {
      // Search filter
      if (search) {
        const searchLower = search.toLowerCase();
        const matchesSearch =
          match.candidate.firstName.toLowerCase().includes(searchLower) ||
          match.candidate.lastName.toLowerCase().includes(searchLower) ||
          match.position.title.toLowerCase().includes(searchLower) ||
          (match.position.clientName ?? '').toLowerCase().includes(searchLower);
        if (!matchesSearch) return false;
      }

      // Status filter
      if (status !== 'ALL' && match.status !== status) return false;

      // Score filter
      if (match.overallScore < parseInt(minScore)) return false;

      return true;
    });
  }, [matches, search, status, minScore]);

  const activeFilters = [
    search ? 1 : 0,
    status !== 'ALL' ? 1 : 0,
    minScore !== '0' ? 1 : 0,
  ].reduce((a, b) => a + b, 0);

  const handleStatusChange = (id: string, newStatus: MatchStatus) => {
    updateStatus.mutate({ id, status: newStatus });
  };

  const handleCreateReferral = (match: Match) => {
    createReferral.mutate({
      matchId: match.id,
    });
  };

  const clearFilters = () => {
    setSearch('');
    setStatus('ALL');
    setMinScore('0');
  };

  if (error) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-medium text-destructive">Failed to load matches</p>
          <p className="text-sm text-muted-foreground mt-1">Please try again later</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex items-center justify-between"
      >
        <div className="space-y-1">
          <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight">
            <GitCompare className="h-6 w-6 text-accent" />
            Matches
          </h1>
          <p className="text-muted-foreground">
            Review and manage candidate-position matches
          </p>
        </div>
        <div className="text-sm text-muted-foreground">
          {filteredMatches.length} of {matches.length} matches
        </div>
      </motion.div>

      {/* Filters */}
      <MatchFilters
        search={search}
        onSearchChange={setSearch}
        status={status}
        onStatusChange={setStatus}
        minScore={minScore}
        onMinScoreChange={setMinScore}
        onClear={clearFilters}
        activeFilters={activeFilters}
      />

      {/* Table */}
      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-16 w-full" />
          ))}
        </div>
      ) : filteredMatches.length === 0 ? (
        <div className="flex h-64 items-center justify-center rounded-xl border border-dashed border-border">
          <div className="text-center">
            <GitCompare className="mx-auto h-10 w-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">No matches found</p>
            {activeFilters > 0 && (
              <button
                onClick={clearFilters}
                className="mt-2 text-sm text-primary hover:underline"
              >
                Clear filters
              </button>
            )}
          </div>
        </div>
      ) : (
        <MatchTable
          matches={filteredMatches}
          onStatusChange={handleStatusChange}
          onCreateReferral={handleCreateReferral}
        />
      )}
    </div>
  );
}
