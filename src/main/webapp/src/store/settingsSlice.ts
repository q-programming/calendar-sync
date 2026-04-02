import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { SettingsApi } from '@api';
import type { SyncSettings } from '@api';
import { apiConfig, axiosInstance } from '../services/api-instance';

const settingsApi = new SettingsApi(apiConfig, '', axiosInstance);

export const fetchSettings = createAsyncThunk('settings/fetch', () =>
  settingsApi.getSettings().then(r => r.data));

export const updateSettings = createAsyncThunk('settings/update', (body: SyncSettings) =>
  settingsApi.updateSettings(body).then(() => body));

interface SettingsState {
  settings: SyncSettings | null;
}

const settingsSlice = createSlice({
  name: 'settings',
  initialState: { settings: null } as SettingsState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchSettings.fulfilled, (state, { payload }) => { state.settings = payload; })
      .addCase(updateSettings.fulfilled, (state, { payload }) => { state.settings = payload; });
  },
});

export default settingsSlice.reducer;
