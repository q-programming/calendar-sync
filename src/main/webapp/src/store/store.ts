import { configureStore } from '@reduxjs/toolkit';
import profileReducer from './profileSlice';
import settingsReducer from './settingsSlice';
import syncReducer from './syncSlice';
import logsReducer from './logsSlice';
import loadingReducer from './loadingSlice';

export const store = configureStore({
  reducer: {
    profile: profileReducer,
    settings: settingsReducer,
    sync: syncReducer,
    logs: logsReducer,
    loading: loadingReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
