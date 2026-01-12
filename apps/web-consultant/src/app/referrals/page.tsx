'use client';

import { useState, useMemo } from 'react';
import { motion } from 'framer-motion';
import { Send, Filter, Rocket } from 'lucide-react';
import { ReferralCard } from '@/components/referrals/referral-card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useReferrals, useUpdateReferralStatus, useSendReferral } from '@/hooks/use-api';
import { Skeleton } from '@/components/ui/skeleton';
import type { ReferralStatus } from '@/types';

const statusTabs: { value: ReferralStatus | 'ALL'; label: string }[] = [
  { value: 'ALL', label: 'All' },
  { value: 'PENDING', label: 'Pending' },
  { value: 'SENT', label: 'Sent' },
  { value: 'CONTACTED', label: 'Contacted' },
  { value: 'INTERVIEWING', label: 'Interviewing' },
  { value: 'PLACED', label: 'Placed' },
];

export default function ReferralsPage() {
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<ReferralStatus | 'ALL'>('ALL');

  const { data, isLoading, error } = useReferrals();
  const updateStatus = useUpdateReferralStatus();
  const sendReferral = useSendReferral();

  const referrals = data?.data ?? [];

  const filteredReferrals = useMemo(() => {
    return referrals.filter((ref) => {
      if (search) {
        const searchLower = search.toLowerCase();
        const matchesSearch =
          ref.candidate.firstName.toLowerCase().includes(searchLower) ||
          ref.candidate.lastName.toLowerCase().includes(searchLower) ||
          ref.position.title.toLowerCase().includes(searchLower);
        if (!matchesSearch) return false;
      }
      if (statusFilter !== 'ALL' && ref.status !== statusFilter) return false;
      return true;
    });
  }, [referrals, search, statusFilter]);

  const pendingCount = referrals.filter((r) => r.status === 'PENDING').length;

  const handleSend = (id: string) => {
    sendReferral.mutate(id);
  };

  const handleStatusChange = (id: string, status: ReferralStatus) => {
    updateStatus.mutate({ id, status });
  };

  if (error) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-medium text-destructive">Failed to load referrals</p>
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
        className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between"
      >
        <div className="space-y-1">
          <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight">
            <Send className="h-6 w-6 text-primary" />
            Referrals
          </h1>
          <p className="text-muted-foreground">
            Track and manage candidate referrals
          </p>
        </div>

        {pendingCount > 0 && (
          <Button className="gap-2 glow-primary">
            <Rocket className="h-4 w-4" />
            Send All Pending ({pendingCount})
          </Button>
        )}
      </motion.div>

      {/* Filters */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
        <Input
          placeholder="Search referrals..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs bg-card"
        />

        <Tabs
          value={statusFilter}
          onValueChange={(v) => setStatusFilter(v as ReferralStatus | 'ALL')}
          className="w-full sm:w-auto"
        >
          <TabsList className="bg-muted/50">
            {statusTabs.map((tab) => (
              <TabsTrigger key={tab.value} value={tab.value} className="text-xs">
                {tab.label}
              </TabsTrigger>
            ))}
          </TabsList>
        </Tabs>
      </div>

      {/* Referrals grid */}
      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48 w-full rounded-xl" />
          ))}
        </div>
      ) : filteredReferrals.length === 0 ? (
        <div className="flex h-64 items-center justify-center rounded-xl border border-dashed border-border">
          <div className="text-center">
            <Send className="mx-auto h-10 w-10 text-muted-foreground/50" />
            <p className="mt-2 text-sm text-muted-foreground">No referrals found</p>
          </div>
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredReferrals.map((referral, index) => (
            <ReferralCard
              key={referral.id}
              referral={referral}
              onSend={handleSend}
              onStatusChange={handleStatusChange}
              index={index}
            />
          ))}
        </div>
      )}
    </div>
  );
}
