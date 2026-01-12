'use client';

import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';

interface ScoreRingProps {
  score: number;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
  className?: string;
}

export function ScoreRing({ score, size = 'md', showLabel = true, className }: ScoreRingProps) {
  const sizes = {
    sm: { ring: 36, stroke: 3, text: 'text-xs' },
    md: { ring: 48, stroke: 4, text: 'text-sm' },
    lg: { ring: 64, stroke: 5, text: 'text-base' },
  };

  const { ring, stroke, text } = sizes[size];
  const radius = (ring - stroke) / 2;
  const circumference = 2 * Math.PI * radius;
  const progress = ((100 - score) / 100) * circumference;

  const getScoreColor = (s: number) => {
    if (s >= 80) return 'text-success stroke-success';
    if (s >= 60) return 'text-primary stroke-primary';
    if (s >= 40) return 'text-warning stroke-warning';
    return 'text-destructive stroke-destructive';
  };

  return (
    <div className={cn('relative inline-flex items-center justify-center', className)}>
      <svg width={ring} height={ring} className="-rotate-90">
        {/* Background ring */}
        <circle
          cx={ring / 2}
          cy={ring / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          className="stroke-muted"
        />
        {/* Progress ring */}
        <motion.circle
          cx={ring / 2}
          cy={ring / 2}
          r={radius}
          fill="none"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: progress }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
          className={cn('transition-colors', getScoreColor(score))}
        />
      </svg>
      {showLabel && (
        <span className={cn('absolute font-semibold', text, getScoreColor(score).split(' ')[0])}>
          {score}
        </span>
      )}
    </div>
  );
}
