import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { SyncApi } from '@api';
import type { SyncRun } from '@api';
import { SyncRunStatus } from '@api';
import { apiConfig, axiosInstance } from '../services/api-instance';

const syncApi = new SyncApi(apiConfig, '', axiosInstance);

export const triggerSync = createAsyncThunk('sync/trigger', () =>
    syncApi.triggerSync({ silent: true }).then((r) => r.data),
);

interface SyncState {
    lastRun: SyncRun | null;
    googleTokenExpired: boolean;
}

const syncSlice = createSlice({
    name: 'sync',
    initialState: { lastRun: null, googleTokenExpired: false } as SyncState,
    reducers: {
        setGoogleTokenExpired: (state, { payload }: { payload: boolean }) => {
            state.googleTokenExpired = payload;
        },
    },
    extraReducers: (builder) => {
        builder.addCase(triggerSync.fulfilled, (state, { payload }) => {
            state.lastRun = payload;
            if (payload.status === SyncRunStatus.GoogleTokenExpired) {
                state.googleTokenExpired = true;
            }
        });
    },
});

export const { setGoogleTokenExpired } = syncSlice.actions;
export default syncSlice.reducer;
