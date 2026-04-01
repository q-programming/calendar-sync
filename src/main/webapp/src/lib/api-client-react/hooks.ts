import { useQuery, useMutation } from '@tanstack/react-query';
import type { UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import type {
  Profile,
  CalendarRef,
  CalendarSelection,
  SyncSettings,
  SyncRun,
  SyncRunDetails,
  PagedSyncRuns,
  OutlookConnection,
  GetLogsParams,
} from './types';

const BASE = '/calendarsync/api';

async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...((init?.headers as Record<string, string>) ?? {}),
    },
    ...init,
  });
  if (!res.ok) {
    const err = await res.text().catch(() => '');
    const error = Object.assign(new Error(`HTTP ${res.status}: ${err}`), { status: res.status });
    throw error;
  }
  if (res.status === 204 || res.headers.get('content-length') === '0') return undefined as T;
  return res.json() as Promise<T>;
}

// ---- Profile ----

export const getGetProfileQueryKey = () => ['/profile'] as const;

export function useGetProfile<T = Profile>(options?: {
  query?: UseQueryOptions<Profile, Error, T>;
}) {
  const { query: q } = options ?? {};
  return useQuery<Profile, Error, T>({
    queryKey: getGetProfileQueryKey(),
    queryFn: () => apiFetch<Profile>('/profile'),
    ...q,
  });
}

// ---- Outlook ----

export const getGetOutlookCalendarsQueryKey = () => ['/profile/outlook/calendars'] as const;

export function useGetOutlookCalendars<T = CalendarRef[]>(options?: {
  query?: UseQueryOptions<CalendarRef[], Error, T>;
}) {
  const { query: q } = options ?? {};
  return useQuery<CalendarRef[], Error, T>({
    queryKey: getGetOutlookCalendarsQueryKey(),
    queryFn: () => apiFetch<CalendarRef[]>('/profile/outlook/calendars'),
    ...q,
  });
}

export const useConnectOutlook = <TError = Error>(
  options?: UseMutationOptions<void, TError, { data: OutlookConnection }>,
) =>
  useMutation<void, TError, { data: OutlookConnection }>({
    mutationFn: ({ data }) =>
      apiFetch<void>('/profile/outlook/connect', {
        method: 'POST',
        body: JSON.stringify(data),
      }),
    ...options,
  });

export const useSetOutlookCalendar = <TError = Error>(
  options?: UseMutationOptions<void, TError, { data: CalendarSelection }>,
) =>
  useMutation<void, TError, { data: CalendarSelection }>({
    mutationFn: ({ data }) =>
      apiFetch<void>('/profile/outlook/calendar', {
        method: 'PUT',
        body: JSON.stringify(data),
      }),
    ...options,
  });

// ---- Google ----

export const getGetGoogleCalendarsQueryKey = () => ['/profile/google/calendars'] as const;

export function useGetGoogleCalendars<T = CalendarRef[]>(options?: {
  query?: UseQueryOptions<CalendarRef[], Error, T>;
}) {
  const { query: q } = options ?? {};
  return useQuery<CalendarRef[], Error, T>({
    queryKey: getGetGoogleCalendarsQueryKey(),
    queryFn: () => apiFetch<CalendarRef[]>('/profile/google/calendars'),
    ...q,
  });
}

export const useSetGoogleCalendar = <TError = Error>(
  options?: UseMutationOptions<void, TError, { data: CalendarSelection }>,
) =>
  useMutation<void, TError, { data: CalendarSelection }>({
    mutationFn: ({ data }) =>
      apiFetch<void>('/profile/google/calendar', {
        method: 'PUT',
        body: JSON.stringify(data),
      }),
    ...options,
  });

// ---- Settings ----

export const getGetSettingsQueryKey = () => ['/settings'] as const;

export function useGetSettings<T = SyncSettings>(options?: {
  query?: UseQueryOptions<SyncSettings, Error, T>;
}) {
  const { query: q } = options ?? {};
  return useQuery<SyncSettings, Error, T>({
    queryKey: getGetSettingsQueryKey(),
    queryFn: () => apiFetch<SyncSettings>('/settings'),
    ...q,
  });
}

export const useUpdateSettings = <TError = Error>(
  options?: UseMutationOptions<void, TError, { data: SyncSettings }>,
) =>
  useMutation<void, TError, { data: SyncSettings }>({
    mutationFn: ({ data }) =>
      apiFetch<void>('/settings', { method: 'PUT', body: JSON.stringify(data) }),
    ...options,
  });

// ---- Sync ----

export const useTriggerSync = <TError = Error>(
  options?: UseMutationOptions<SyncRun, TError, undefined>,
) =>
  useMutation<SyncRun, TError, undefined>({
    mutationFn: () => apiFetch<SyncRun>('/sync/run', { method: 'POST' }),
    ...options,
  });

// ---- Logs ----

export const getGetLogsQueryKey = (params?: GetLogsParams) => ['/logs', params] as const;

export function useGetLogs(
  params?: GetLogsParams,
  options?: { query?: UseQueryOptions<PagedSyncRuns, Error> },
) {
  const qs = new URLSearchParams();
  if (params?.page !== undefined) qs.set('page', String(params.page));
  if (params?.size !== undefined) qs.set('size', String(params.size));
  if (params?.status) qs.set('status', params.status);
  const query = qs.toString();
  return useQuery<PagedSyncRuns, Error>({
    queryKey: getGetLogsQueryKey(params),
    queryFn: () => apiFetch<PagedSyncRuns>(`/logs${query ? '?' + query : ''}`),
    ...options?.query,
  });
}

export const getGetLogDetailsQueryKey = (runId: string) => ['/logs', runId] as const;

export function useGetLogDetails<T = SyncRunDetails>(
  runId: string,
  options?: { query?: UseQueryOptions<SyncRunDetails, Error, T> },
) {
  const { query: q } = options ?? {};
  return useQuery<SyncRunDetails, Error, T>({
    queryKey: getGetLogDetailsQueryKey(runId),
    queryFn: () => apiFetch<SyncRunDetails>(`/logs/${runId}`),
    enabled: !!runId,
    ...q,
  });
}
