import { useMutation } from '@tanstack/react-query';
import type { UseMutationOptions } from '@tanstack/react-query';
import { SyncApi } from '@api';
import type { SyncRun } from '@api';
import { apiConfig, axiosInstance } from './api-instance';

const syncApi = new SyncApi(apiConfig, '', axiosInstance);

export function useTriggerSync(
  options?: UseMutationOptions<SyncRun, Error, void>,
) {
  return useMutation<SyncRun, Error, void>({
    mutationFn: () => syncApi.triggerSync().then((r) => r.data),
    ...options,
  });
}
