import type { AppDispatch } from './store';
import { axiosInstance } from '../services/api-instance';
import { requestStarted, requestFinished } from './loadingSlice';

declare module 'axios' {
    interface AxiosRequestConfig {
        silent?: boolean; // skip global loading overlay when true
    }
}

export function setupInterceptors(dispatch: AppDispatch) {
    axiosInstance.interceptors.request.use((config) => {
        if (!config.silent) dispatch(requestStarted());
        return config;
    });

    axiosInstance.interceptors.response.use(
        (response) => {
            if (!response.config.silent) dispatch(requestFinished());
            return response;
        },
        (error) => {
            if (!error.config?.silent) dispatch(requestFinished());
            return Promise.reject(error);
        },
    );
}
