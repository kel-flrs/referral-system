'use client';

import { motion } from 'framer-motion';
import { Send, CheckCircle, XCircle, Clock, Users } from 'lucide-react';
import { cn } from '@/lib/utils';

interface Activity {
  id: string;
  type: 'referral_sent' | 'referral_placed' | 'referral_rejected' | 'match_created' | 'status_update';
  message: string;
  timestamp: string;
}

const mockActivities: Activity[] = [
  { id: '1', type: 'referral_placed', message: 'John Smith placed at TechCorp', timestamp: '2 hours ago' },
  { id: '2', type: 'referral_sent', message: 'Referral sent for Sarah Johnson', timestamp: '4 hours ago' },
  { id: '3', type: 'match_created', message: '12 new matches found', timestamp: '5 hours ago' },
  { id: '4', type: 'status_update', message: 'Mike Brown moved to interviewing', timestamp: '6 hours ago' },
  { id: '5', type: 'referral_rejected', message: 'Lisa Davis declined by client', timestamp: '1 day ago' },
];

const activityIcons = {
  referral_sent: { icon: Send, color: 'text-primary bg-primary/10' },
  referral_placed: { icon: CheckCircle, color: 'text-success bg-success/10' },
  referral_rejected: { icon: XCircle, color: 'text-destructive bg-destructive/10' },
  match_created: { icon: Users, color: 'text-accent bg-accent/10' },
  status_update: { icon: Clock, color: 'text-warning bg-warning/10' },
};

export function ActivityFeed() {
  return (
    <div className="space-y-1">
      {mockActivities.map((activity, index) => {
        const { icon: Icon, color } = activityIcons[activity.type];

        return (
          <motion.div
            key={activity.id}
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3, delay: index * 0.08 }}
            className="group flex items-start gap-3 rounded-lg p-3 transition-colors hover:bg-muted/50"
          >
            <div className={cn('flex h-8 w-8 shrink-0 items-center justify-center rounded-full', color)}>
              <Icon className="h-4 w-4" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm text-foreground">{activity.message}</p>
              <p className="text-xs text-muted-foreground">{activity.timestamp}</p>
            </div>
          </motion.div>
        );
      })}
    </div>
  );
}
