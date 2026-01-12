'use client';

import { motion } from 'framer-motion';
import { ArrowRight, Briefcase, User } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ScoreRing } from './score-ring';
import type { Match } from '@/types';
import Link from 'next/link';

interface RecentMatchesProps {
  matches: Match[];
}

export function RecentMatches({ matches }: RecentMatchesProps) {
  if (!matches?.length) {
    return (
      <div className="flex h-48 items-center justify-center rounded-xl border border-dashed border-border">
        <p className="text-sm text-muted-foreground">No recent matches</p>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {matches.slice(0, 5).map((match, index) => (
        <motion.div
          key={match.id}
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.3, delay: index * 0.1 }}
          className="group relative overflow-hidden rounded-lg border border-border bg-card p-4 transition-all hover:border-primary/30 hover:shadow-md"
        >
          <div className="flex items-center gap-4">
            {/* Score */}
            <ScoreRing score={match.overallScore} size="md" />

            {/* Match info */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-1">
                <User className="h-3.5 w-3.5 text-muted-foreground" />
                <span className="font-medium text-sm truncate">
                  {match.candidate.firstName} {match.candidate.lastName}
                </span>
              </div>
              <div className="flex items-center gap-2">
                <Briefcase className="h-3.5 w-3.5 text-muted-foreground" />
                <span className="text-xs text-muted-foreground truncate">
                  {match.position.title} @ {match.position.clientName}
                </span>
              </div>
            </div>

            {/* Skills preview */}
            <div className="hidden sm:flex items-center gap-1.5">
              {match.matchedSkills.slice(0, 2).map((skill) => (
                <Badge key={skill} variant="secondary" className="text-[10px]">
                  {skill}
                </Badge>
              ))}
              {match.matchedSkills.length > 2 && (
                <Badge variant="outline" className="text-[10px]">
                  +{match.matchedSkills.length - 2}
                </Badge>
              )}
            </div>

            {/* Action */}
            <Link href={`/matches?id=${match.id}`}>
              <Button
                size="icon"
                variant="ghost"
                className="h-8 w-8 opacity-0 transition-opacity group-hover:opacity-100"
              >
                <ArrowRight className="h-4 w-4" />
              </Button>
            </Link>
          </div>
        </motion.div>
      ))}
    </div>
  );
}
