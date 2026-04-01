import { useQuery, useMutation } from '@tanstack/react-query';
import type { UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { ProfileApi, OutlookApi, GoogleApi } from '@api';
import type { CalendarSelection, OutlookConnection, Profile, CalendarRef } from '@api';
import { apiConfig, axiosInstance } from './api-instance';

const profileApi = new ProfileApi(apiConfig, '', axiosInstance);
const outlookApi = new OutlookApi(apiConfig, '', axiosInstance);
const googleApi = new GoogleApi(apiConfig, '', axiosInstance);

// ── Query keys ────────────────────────────────────────────────────────────────

export const profileKeys = {
  profile: () => ['profile'] as const,
  outlookCalendars: () => ['profile', 'outlook', 'calendars'] as const,
  googleCalendars: () => ['profile', 'google', 'calendars'] as const,
};

// ── Shared extra-options type (no queryKey / queryFn — those come from the service) ──

type ExtraQueryOptions<TData, TSelect = TData> = Omit<
  UseQueryOptions<TData, Error, TSelect>,
  'queryKey' | 'queryFn'
>;

// ── Profile ───────────────────────────────────────────────────────────────────

export function useGetProfile<T = Profile>(options?: ExtraQueryOptions<Profile, T>) {
  return useQuery<Profile, Error, T>({
    queryKey: profileKeys.profile(),
    queryFn: () => profileApi.getProfile().then((r) => r.data),
    ...options,
  });
}

// ── Outlook ───────────────────────────────────────────────────────────────────

export function useGetOutlookCalendars<T = CalendarRef[]>(
  options?: ExtraQueryOptions<CalendarRef[], T>,
) {
  return useQuery<CalendarRef[], Error, T>({
    queryKey: profileKeys.outlookCalendars(),
    queryFn: () => outlookApi.getOutlookCalendars().then((r) => r.data),
    ...options,
  });
}

export function useConnectOutlook(
  options?: UseMutationOptions<void, Error, OutlookConnection>,
) {
  return useMutation<void, Error, OutlookConnection>({
    mutationFn: (body) => outlookApi.connectOutlook(body).then(() => undefined),
    ...options,
  });
}

export function useSetOutlookCalendar(
  options?: UseMutationOptions<void, Error, CalendarSelection>,
) {
  return useMutation<void, Error, CalendarSelection>({
    mutationFn: (body) => outlookApi.setOutlookCalendar(body).then(() => undefined),
    ...options,
  });
}

// ── Google ────────────────────────────────────────────────────────────────────

export function useGetGoogleCalendars<T = CalendarRef[]>(
  options?: ExtraQueryOptions<CalendarRef[], T>,
) {
  return useQuery<CalendarRef[], Error, T>({
    queryKey: profileKeys.googleCalendars(),
    queryFn: () => googleApi.getGoogleCalendars().then((r) => r.data),
    ...options,
  });
}

export function useSetGoogleCalendar(
  options?: UseMutationOptions<void, Error, CalendarSelection>,
) {
  return useMutation<void, Error, CalendarSelection>({
    mutationFn: (body) => googleApi.setGoogleCalendar(body).then(() => undefined),
    ...options,
  });
}
