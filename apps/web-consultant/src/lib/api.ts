const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:3222/api';

async function fetchApi<T>(endpoint: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({}));
    throw new Error(error.message || error.error || `HTTP ${res.status}`);
  }

  return res.json();
}

export const api = {
  dashboard: {
    getStats: () => fetchApi<import('@/types').DashboardStats>('/dashboard/stats'),
    getHealth: () => fetchApi<{ status: string }>('/dashboard/health'),
  },

  matches: {
    getAll: (params?: Record<string, string>) => {
      const query = params ? `?${new URLSearchParams(params)}` : '';
      return fetchApi<{ data: import('@/types').Match[] }>(`/matches${query}`);
    },
    getById: (id: string) => fetchApi<{ data: import('@/types').Match }>(`/matches/${id}`),
    updateStatus: (id: string, status: string) =>
      fetchApi<{ data: import('@/types').Match }>(`/matches/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      }),
    getSimilarCandidates: (positionId: string) =>
      fetchApi<{ data: import('@/types').Match[] }>(`/matches/positions/${positionId}/similar-candidates`),
  },

  referrals: {
    getAll: (params?: Record<string, string>) => {
      const query = params ? `?${new URLSearchParams(params)}` : '';
      return fetchApi<{ data: import('@/types').Referral[] }>(`/referrals${query}`);
    },
    getById: (id: string) => fetchApi<{ data: import('@/types').Referral }>(`/referrals/${id}`),
    create: (data: { matchId: string; consultantId?: string; referralSource?: string; notes?: string }) =>
      fetchApi<{ data: import('@/types').Referral }>('/referrals', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    updateStatus: (id: string, status: string) =>
      fetchApi<{ data: import('@/types').Referral }>(`/referrals/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      }),
    send: (id: string) =>
      fetchApi<{ data: import('@/types').Referral }>(`/referrals/${id}/send`, { method: 'POST' }),
    sendPending: () =>
      fetchApi<{ data: { sent: number } }>('/referrals/send-pending', { method: 'POST' }),
  },
};
