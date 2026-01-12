'use client';

import { Search, SlidersHorizontal, X } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import type { MatchStatus } from '@/types';

interface MatchFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
  status: MatchStatus | 'ALL';
  onStatusChange: (value: MatchStatus | 'ALL') => void;
  minScore: string;
  onMinScoreChange: (value: string) => void;
  onClear: () => void;
  activeFilters: number;
}

export function MatchFilters({
  search,
  onSearchChange,
  status,
  onStatusChange,
  minScore,
  onMinScoreChange,
  onClear,
  activeFilters,
}: MatchFiltersProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="relative flex-1 max-w-md">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          placeholder="Search candidates, positions..."
          value={search}
          onChange={(e) => onSearchChange(e.target.value)}
          className="pl-9 bg-card"
        />
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Select value={status} onValueChange={(v) => onStatusChange(v as MatchStatus | 'ALL')}>
          <SelectTrigger className="w-[140px] bg-card">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All Status</SelectItem>
            <SelectItem value="PENDING">Pending</SelectItem>
            <SelectItem value="REVIEWED">Reviewed</SelectItem>
            <SelectItem value="REFERRED">Referred</SelectItem>
            <SelectItem value="REJECTED">Rejected</SelectItem>
          </SelectContent>
        </Select>

        <Select value={minScore} onValueChange={onMinScoreChange}>
          <SelectTrigger className="w-[130px] bg-card">
            <SelectValue placeholder="Min Score" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="0">Any Score</SelectItem>
            <SelectItem value="50">50+</SelectItem>
            <SelectItem value="60">60+</SelectItem>
            <SelectItem value="70">70+</SelectItem>
            <SelectItem value="80">80+</SelectItem>
            <SelectItem value="90">90+</SelectItem>
          </SelectContent>
        </Select>

        {activeFilters > 0 && (
          <Button
            variant="ghost"
            size="sm"
            onClick={onClear}
            className="h-9 gap-1.5 text-muted-foreground hover:text-foreground"
          >
            <X className="h-3.5 w-3.5" />
            Clear
            <Badge variant="secondary" className="ml-1 h-5 w-5 rounded-full p-0 text-[10px]">
              {activeFilters}
            </Badge>
          </Button>
        )}
      </div>
    </div>
  );
}
