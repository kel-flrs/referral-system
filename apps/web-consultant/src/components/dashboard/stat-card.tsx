'use client';

import { motion } from 'framer-motion';
import { cn } from '@/lib/utils';
import { LucideIcon } from 'lucide-react';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: { value: number; label: string };
  variant?: 'default' | 'primary' | 'accent';
  delay?: number;
}

export function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  variant = 'default',
  delay = 0,
}: StatCardProps) {
  const variants = {
    default: 'bg-card border-border',
    primary: 'bg-primary/5 border-primary/20',
    accent: 'bg-accent/5 border-accent/20',
  };

  const iconVariants = {
    default: 'bg-muted text-muted-foreground',
    primary: 'bg-primary/15 text-primary',
    accent: 'bg-accent/15 text-accent',
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay }}
      className={cn(
        'group relative overflow-hidden rounded-xl border p-5 transition-all hover:shadow-lg',
        variants[variant]
      )}
    >
      {/* Subtle gradient overlay on hover */}
      <div className="absolute inset-0 bg-gradient-to-br from-white/[0.02] to-transparent opacity-0 transition-opacity group-hover:opacity-100" />

      <div className="relative flex items-start justify-between">
        <div className="space-y-3">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <div className="flex items-baseline gap-2">
            <span className="text-3xl font-bold tracking-tight text-foreground">
              {typeof value === 'number' ? value.toLocaleString() : value}
            </span>
            {trend && (
              <span
                className={cn(
                  'text-xs font-medium',
                  trend.value >= 0 ? 'text-success' : 'text-destructive'
                )}
              >
                {trend.value >= 0 ? '+' : ''}
                {trend.value}% {trend.label}
              </span>
            )}
          </div>
          {subtitle && (
            <p className="text-xs text-muted-foreground">{subtitle}</p>
          )}
        </div>

        <div
          className={cn(
            'flex h-10 w-10 items-center justify-center rounded-lg transition-transform group-hover:scale-110',
            iconVariants[variant]
          )}
        >
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </motion.div>
  );
}
