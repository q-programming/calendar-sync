import { useQuery } from '@tanstack/react-query';
import type { UseQueryOptions } from '@tanstack/react-query';
import { LogsApi } from '@api';
import type { PagedSyncRuns, SyncRunDetails } from '@api';
import { apiConfig, axiosInstance } from './api-instance';

const logsApi = new LogsApi(apiConfig, '', axiosInstance);

type ExtraQueryOptions<TData, TSelect = TData> = Omit<
  UseQueryOptions<TData, Error, TSelect>,
  'queryKey' | 'queryFn'
>;

export type LogsStatus = 'SUCCESS' | 'FAILED';

export interface GetLogsParams {
  page?: number;
  size?: number;
  status?: LogsStatus;
}

export const logsKeys = {
  list: (params?: GetLogsParams) => ['logs', 'list', params] as const,
  detail: (runId: string) => ['logs', 'detail', runId] as const,
};

export function useGetLogs(
  params?: GetLogsParams,
  options?: ExtraQueryOptions<PagedSyncRuns>,
) {
  return useQuery<PagedSyncRuns, Error>({
    queryKey: logsKeys.list(params),
    queryFn: () =>
      logsApi.getLogs(params?.page, params?.size, params?.status).then((r) => r.data),
    ...options,
  });
}

export function useGetLogDetails<T = SyncRunDetails>(
  runId: string,
  options?: ExtraQueryOptions<SyncRunDetails, T>,
) {
  return useQuery<SyncRunDetails, Error, T>({
    queryKey: logsKeys.detail(runId),
    queryFn: () => logsApi.getLogDetails(runId).then((r) => r.data),
    enabled: !!runId,
    ...options,
  });
}
