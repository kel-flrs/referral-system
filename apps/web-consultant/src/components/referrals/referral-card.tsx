'use client';

import { motion } from 'framer-motion';
import { format } from 'date-fns';
import {
  Send,
  Clock,
  CheckCircle,
  Phone,
  Calendar,
  Award,
  XCircle,
  MoreVertical,
  ExternalLink,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
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
import type { Referral, ReferralStatus } from '@/types';

interface ReferralCardProps {
  referral: Referral;
  onSend: (id: string) => void;
  onStatusChange: (id: string, status: ReferralStatus) => void;
  index?: number;
}

const statusConfig: Record<ReferralStatus, { icon: typeof Clock; label: string; className: string }> = {
  PENDING: { icon: Clock, label: 'Pending', className: 'status-pending' },
  SENT: { icon: Send, label: 'Sent', className: 'status-referred' },
  CONTACTED: { icon: Phone, label: 'Contacted', className: 'status-reviewed' },
  INTERVIEWING: { icon: Calendar, label: 'Interviewing', className: 'bg-accent/15 text-accent border-accent/30' },
  PLACED: { icon: Award, label: 'Placed', className: 'status-placed' },
  REJECTED: { icon: XCircle, label: 'Rejected', className: 'status-rejected' },
};

export function ReferralCard({ referral, onSend, onStatusChange, index = 0 }: ReferralCardProps) {
  const config = statusConfig[referral.status];
  const StatusIcon = config.icon;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay: index * 0.05 }}
    >
      <Card className="group overflow-hidden transition-all hover:shadow-lg hover:border-primary/20">
        <CardContent className="p-0">
          {/* Status bar */}
          <div className={cn('h-1', {
            'bg-warning': referral.status === 'PENDING',
            'bg-primary': referral.status === 'SENT',
            'bg-accent': referral.status === 'CONTACTED' || referral.status === 'INTERVIEWING',
            'bg-success': referral.status === 'PLACED',
            'bg-destructive': referral.status === 'REJECTED',
          })} />

          <div className="p-5">
            <div className="flex items-start justify-between gap-4">
              {/* Left section */}
              <div className="flex gap-4 min-w-0">
                {referral.match && (
                  <ScoreRing score={referral.match.overallScore} size="md" />
                )}
                <div className="min-w-0">
                  <h3 className="font-semibold truncate">
                    {referral.candidate.firstName} {referral.candidate.lastName}
                  </h3>
                  <p className="text-sm text-muted-foreground truncate">
                    {referral.position.title}
                  </p>
                  <p className="text-xs text-muted-foreground truncate">
                    @ {referral.position.clientName}
                  </p>
                </div>
              </div>

              {/* Right section */}
              <div className="flex items-start gap-2">
                <Badge className={cn('border shrink-0', config.className)}>
                  <StatusIcon className="mr-1 h-3 w-3" />
                  {config.label}
                </Badge>
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 opacity-0 group-hover:opacity-100"
                    >
                      <MoreVertical className="h-4 w-4" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    {referral.status === 'PENDING' && (
                      <DropdownMenuItem onClick={() => onSend(referral.id)}>
                        <Send className="mr-2 h-4 w-4" />
                        Send to Bullhorn
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuSeparator />
                    <DropdownMenuItem onClick={() => onStatusChange(referral.id, 'CONTACTED')}>
                      <Phone className="mr-2 h-4 w-4" />
                      Mark Contacted
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onStatusChange(referral.id, 'INTERVIEWING')}>
                      <Calendar className="mr-2 h-4 w-4" />
                      Mark Interviewing
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => onStatusChange(referral.id, 'PLACED')}>
                      <Award className="mr-2 h-4 w-4" />
                      Mark Placed
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    <DropdownMenuItem
                      onClick={() => onStatusChange(referral.id, 'REJECTED')}
                      className="text-destructive"
                    >
                      <XCircle className="mr-2 h-4 w-4" />
                      Mark Rejected
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            {/* Footer */}
            <div className="mt-4 flex items-center justify-between text-xs text-muted-foreground">
              <span>
                Created {format(new Date(referral.createdAt), 'MMM d, yyyy')}
              </span>
              {referral.sentToBullhornAt && (
                <span className="flex items-center gap-1">
                  <Send className="h-3 w-3" />
                  Sent {format(new Date(referral.sentToBullhornAt), 'MMM d')}
                </span>
              )}
            </div>

            {referral.notes && (
              <p className="mt-3 text-sm text-muted-foreground line-clamp-2 border-t border-border pt-3">
                {referral.notes}
              </p>
            )}
          </div>
        </CardContent>
      </Card>
    </motion.div>
  );
}
