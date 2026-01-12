'use client';

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () => api.dashboard.getStats(),
    refetchInterval: 30000,
  });
}

export function useMatches(params?: Record<string, string>) {
  return useQuery({
    queryKey: ['matches', params],
    queryFn: () => api.matches.getAll(params),
  });
}

export function useMatch(id: string) {
  return useQuery({
    queryKey: ['matches', id],
    queryFn: () => api.matches.getById(id),
    enabled: !!id,
  });
}

export function useUpdateMatchStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      api.matches.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['matches'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

export function useReferrals(params?: Record<string, string>) {
  return useQuery({
    queryKey: ['referrals', params],
    queryFn: () => api.referrals.getAll(params),
  });
}

export function useReferral(id: string) {
  return useQuery({
    queryKey: ['referrals', id],
    queryFn: () => api.referrals.getById(id),
    enabled: !!id,
  });
}

export function useCreateReferral() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: api.referrals.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['referrals'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

export function useUpdateReferralStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      api.referrals.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['referrals'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    },
  });
}

export function useSendReferral() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.referrals.send(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['referrals'] });
    },
  });
}
