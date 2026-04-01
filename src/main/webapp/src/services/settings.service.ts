import { useQuery, useMutation } from '@tanstack/react-query';
import type { UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { SettingsApi } from '@api';
import type { SyncSettings } from '@api';
import { apiConfig, axiosInstance } from './api-instance';

const settingsApi = new SettingsApi(apiConfig, '', axiosInstance);

type ExtraQueryOptions<TData, TSelect = TData> = Omit<
  UseQueryOptions<TData, Error, TSelect>,
  'queryKey' | 'queryFn'
>;

export const settingsKeys = {
  settings: () => ['settings'] as const,
};

export function useGetSettings<T = SyncSettings>(options?: ExtraQueryOptions<SyncSettings, T>) {
  return useQuery<SyncSettings, Error, T>({
    queryKey: settingsKeys.settings(),
    queryFn: () => settingsApi.getSettings().then((r) => r.data),
    ...options,
  });
}

export function useUpdateSettings(
  options?: UseMutationOptions<void, Error, SyncSettings>,
) {
  return useMutation<void, Error, SyncSettings>({
    mutationFn: (body) => settingsApi.updateSettings(body).then(() => undefined),
    ...options,
  });
}
