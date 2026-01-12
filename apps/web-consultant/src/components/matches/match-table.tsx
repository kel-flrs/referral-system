'use client';

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ChevronDown,
  ChevronUp,
  MoreHorizontal,
  ArrowUpRight,
  CheckCircle,
  XCircle,
  Send,
} from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { ScoreRing } from '@/components/dashboard/score-ring';
import { cn } from '@/lib/utils';
import type { Match, MatchStatus } from '@/types';

interface MatchTableProps {
  matches: Match[];
  onStatusChange: (id: string, status: MatchStatus) => void;
  onCreateReferral: (match: Match) => void;
}

const statusConfig: Record<MatchStatus, { label: string; className: string }> = {
  PENDING: { label: 'Pending', className: 'status-pending' },
  REVIEWED: { label: 'Reviewed', className: 'status-reviewed' },
  REFERRED: { label: 'Referred', className: 'status-referred' },
  REJECTED: { label: 'Rejected', className: 'status-rejected' },
};

export function MatchTable({ matches, onStatusChange, onCreateReferral }: MatchTableProps) {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  return (
    <div className="rounded-xl border border-border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow className="bg-muted/30 hover:bg-muted/30">
            <TableHead className="w-12"></TableHead>
            <TableHead className="w-16">Score</TableHead>
            <TableHead>Candidate</TableHead>
            <TableHead>Position</TableHead>
            <TableHead className="hidden lg:table-cell">Skills</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="w-12"></TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {matches.map((match, index) => (
            <motion.tr
              key={match.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.2, delay: index * 0.03 }}
              className={cn(
                'group border-b border-border transition-colors hover:bg-muted/20',
                expandedId === match.id && 'bg-muted/10'
              )}
            >
              <TableCell>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  onClick={() => setExpandedId(expandedId === match.id ? null : match.id)}
                >
                  {expandedId === match.id ? (
                    <ChevronUp className="h-4 w-4" />
                  ) : (
                    <ChevronDown className="h-4 w-4" />
                  )}
                </Button>
              </TableCell>
              <TableCell>
                <ScoreRing score={match.overallScore} size="sm" />
              </TableCell>
              <TableCell>
                <div>
                  <p className="font-medium">
                    {match.candidate.firstName} {match.candidate.lastName}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {match.candidate.currentTitle || 'No title'}
                  </p>
                </div>
              </TableCell>
              <TableCell>
                <div>
                  <p className="font-medium">{match.position.title}</p>
                  <p className="text-xs text-muted-foreground">
                    {match.position.clientName}
                  </p>
                </div>
              </TableCell>
              <TableCell className="hidden lg:table-cell">
                <div className="flex flex-wrap gap-1">
                  {match.matchedSkills.slice(0, 3).map((skill) => (
                    <Badge key={skill} variant="secondary" className="text-[10px]">
                      {skill}
                    </Badge>
                  ))}
                  {match.matchedSkills.length > 3 && (
                    <Badge variant="outline" className="text-[10px]">
                      +{match.matchedSkills.length - 3}
                    </Badge>
                  )}
                </div>
              </TableCell>
              <TableCell>
                <Badge className={cn('border', statusConfig[match.status].className)}>
                  {statusConfig[match.status].label}
                </Badge>
              </TableCell>
              <TableCell>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 opacity-0 group-hover:opacity-100"
                    >
                      <MoreHorizontal className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem onClick={() => onCreateReferral(match)}>
                      <Send className="mr-2 h-4 w-4" />
                      Create Referral
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onStatusChange(match.id, 'REVIEWED')}>
                      <ArrowUpRight className="mr-2 h-4 w-4" />
                      Mark Reviewed
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onStatusChange(match.id, 'REJECTED')}>
                      <XCircle className="mr-2 h-4 w-4" />
                      Reject
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </TableCell>
            </motion.tr>
          ))}
        </TableBody>
      </Table>

      {/* Expanded details row - simplified implementation */}
      <AnimatePresence>
        {expandedId && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="border-t border-border bg-muted/5 p-4"
          >
            {(() => {
              const match = matches.find((m) => m.id === expandedId);
              if (!match) return null;
              return (
                <div className="grid gap-6 md:grid-cols-3">
                  <div>
                    <h4 className="text-sm font-medium mb-2">Score Breakdown</h4>
                    <div className="space-y-2 text-sm">
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Skill Match</span>
                        <span>{match.skillMatchScore}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Experience</span>
                        <span>{match.experienceScore}%</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-muted-foreground">Location</span>
                        <span>{match.locationScore ?? 0}%</span>
                      </div>
                    </div>
                  </div>
                  <div>
                    <h4 className="text-sm font-medium mb-2">Matched Skills</h4>
                    <div className="flex flex-wrap gap-1">
                      {match.matchedSkills.map((skill) => (
                        <Badge key={skill} className="bg-success/10 text-success text-[10px]">
                          {skill}
                        </Badge>
                      ))}
                    </div>
                  </div>
                  <div>
                    <h4 className="text-sm font-medium mb-2">Missing Skills</h4>
                    <div className="flex flex-wrap gap-1">
                      {match.missingSkills.length > 0 ? (
                        match.missingSkills.map((skill) => (
                          <Badge key={skill} className="bg-destructive/10 text-destructive text-[10px]">
                            {skill}
                          </Badge>
                        ))
                      ) : (
                        <span className="text-xs text-muted-foreground">None</span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })()}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
