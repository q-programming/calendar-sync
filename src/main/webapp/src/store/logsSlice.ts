import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { LogsApi } from '@api';
import type { PagedSyncRuns, SyncRunDetails, SyncRunStatus } from '@api';
import { apiConfig, axiosInstance } from '../services/api-instance';

const logsApi = new LogsApi(apiConfig, '', axiosInstance);

export interface FetchLogsParams {
    page?: number;
    size?: number;
    status?: SyncRunStatus;
}

export const fetchLogs = createAsyncThunk('logs/fetchList', (params: FetchLogsParams) =>
    logsApi.getLogs(params.page, params.size, params.status).then((r) => r.data),
);

export const fetchLogDetails = createAsyncThunk('logs/fetchDetails', (runId: string) =>
    logsApi.getLogDetails(runId).then((r) => r.data),
);

interface LogsState {
    list: PagedSyncRuns | null;
    details: SyncRunDetails | null;
}

const logsSlice = createSlice({
    name: 'logs',
    initialState: { list: null, details: null } as LogsState,
    reducers: {
        clearLogDetails: (state) => {
            state.details = null;
        },
    },
    extraReducers: (builder) => {
        builder
            .addCase(fetchLogs.fulfilled, (state, { payload }) => {
                state.list = payload;
            })
            .addCase(fetchLogDetails.fulfilled, (state, { payload }) => {
                state.details = payload;
            });
    },
});

export const { clearLogDetails } = logsSlice.actions;
export default logsSlice.reducer;
