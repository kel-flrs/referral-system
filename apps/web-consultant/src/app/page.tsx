'use client';

import { motion } from 'framer-motion';
import { Users, Briefcase, GitCompare, Send, TrendingUp, Trophy } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { StatCard } from '@/components/dashboard/stat-card';
import { RecentMatches } from '@/components/dashboard/recent-matches';
import { ActivityFeed } from '@/components/dashboard/activity-feed';
import { useDashboardStats } from '@/hooks/use-api';
import { Skeleton } from '@/components/ui/skeleton';

export default function DashboardPage() {
  const { data, isLoading, error } = useDashboardStats();
  const stats = data;

  if (error) {
    return (
      <div className="flex h-96 items-center justify-center">
        <div className="text-center">
          <p className="text-lg font-medium text-destructive">Failed to load dashboard</p>
          <p className="text-sm text-muted-foreground mt-1">
            Make sure the API server is running at localhost:3000
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Page header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="space-y-1"
      >
        <h1 className="text-2xl font-bold tracking-tight">
          Welcome back, <span className="text-gradient">John</span>
        </h1>
        <p className="text-muted-foreground">
          Here&apos;s what&apos;s happening with your referrals today.
        </p>
      </motion.div>

      {/* Stats grid */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="p-5">
                <Skeleton className="h-4 w-24 mb-3" />
                <Skeleton className="h-8 w-16" />
              </CardContent>
            </Card>
          ))
        ) : (
          <>
            <StatCard
              title="Total Candidates"
              value={stats?.overview?.candidates?.total ?? 0}
              icon={Users}
              trend={{ value: 12, label: 'this week' }}
              delay={0}
            />
            <StatCard
              title="Open Positions"
              value={stats?.overview?.positions?.open ?? 0}
              icon={Briefcase}
              variant="primary"
              delay={0.1}
            />
            <StatCard
              title="Active Matches"
              value={stats?.overview?.matches?.total ?? 0}
              icon={GitCompare}
              subtitle={`${stats?.overview?.matches?.pending ?? 0} pending`}
              variant="accent"
              delay={0.2}
            />
            <StatCard
              title="Pending Referrals"
              value={stats?.overview?.referrals?.pending ?? 0}
              icon={Send}
              subtitle={`${stats?.overview?.referrals?.sent ?? 0} sent`}
              delay={0.3}
            />
          </>
        )}
      </div>

      {/* Main content grid */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Recent matches */}
        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between pb-2">
            <CardTitle className="flex items-center gap-2 text-base font-semibold">
              <TrendingUp className="h-4 w-4 text-primary" />
              Recent Matches
            </CardTitle>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-3">
                {Array.from({ length: 3 }).map((_, i) => (
                  <Skeleton key={i} className="h-16 w-full" />
                ))}
              </div>
            ) : (
              <RecentMatches matches={stats?.recentMatches ?? []} />
            )}
          </CardContent>
        </Card>

        {/* Activity feed */}
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-base font-semibold">Recent Activity</CardTitle>
          </CardHeader>
          <CardContent>
            <ActivityFeed />
          </CardContent>
        </Card>
      </div>

      {/* Leaderboard */}
      {stats?.topConsultants && stats.topConsultants.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
        >
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="flex items-center gap-2 text-base font-semibold">
                <Trophy className="h-4 w-4 text-primary" />
                Top Performers
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {stats.topConsultants.slice(0, 3).map((consultant, index) => (
                  <div
                    key={consultant.id}
                    className="flex items-center gap-3 rounded-lg border border-border p-4"
                  >
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 font-bold text-primary">
                      {index + 1}
                    </div>
                    <div>
                      <p className="font-medium">{consultant.firstName} {consultant.lastName}</p>
                      <p className="text-xs text-muted-foreground">
                        {consultant.totalPlacements} placements · {consultant.totalReferrals} referrals
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </motion.div>
      )}
    </div>
  );
}
