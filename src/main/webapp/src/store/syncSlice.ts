import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { SyncApi } from '@api';
import type { SyncRun } from '@api';
import { apiConfig, axiosInstance } from '../services/api-instance';

const syncApi = new SyncApi(apiConfig, '', axiosInstance);

export const triggerSync = createAsyncThunk('sync/trigger', () =>
  syncApi.triggerSync({ silent: true } as any).then(r => r.data));

interface SyncState {
  lastRun: SyncRun | null;
}

const syncSlice = createSlice({
  name: 'sync',
  initialState: { lastRun: null } as SyncState,
  reducers: {},
  extraReducers: (builder) => {
    builder.addCase(triggerSync.fulfilled, (state, { payload }) => { state.lastRun = payload; });
  },
});

export default syncSlice.reducer;
