import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import type { CalendarRef, CalendarSelection, OutlookConnection, Profile } from '@api';
import { GoogleApi, OutlookApi, ProfileApi } from '@api';
import { apiConfig, axiosInstance } from '../services/api-instance';

const profileApi = new ProfileApi(apiConfig, '', axiosInstance);
const outlookApi = new OutlookApi(apiConfig, '', axiosInstance);
const googleApi = new GoogleApi(apiConfig, '', axiosInstance);

// utils

const truncatePath = (path?: string, maxLength: number = 50): string => {
    if (!path) {
        return '';
    }
    if (path.length <= maxLength) {
        return path;
    }
    const lastSlashIndex =
        path.lastIndexOf('/') > 0 ? path.lastIndexOf('/') : path.lastIndexOf('\\');
    if (lastSlashIndex <= 0) return path;

    const lastPart = path.substring(lastSlashIndex);
    const ellipsis = '...';
    const availableForStart = maxLength - lastPart.length - ellipsis.length;

    if (availableForStart < 1) {
        return ellipsis + path.slice(-(maxLength - ellipsis.length));
    }

    return path.substring(0, availableForStart) + ellipsis + lastPart;
};

// ── Thunks ────────────────────────────────────────────────────────────────────

export const fetchProfile = createAsyncThunk('profile/fetch', () =>
    profileApi.getProfile().then((r) => r.data),
);

export const fetchProfileSilent = createAsyncThunk('profile/fetchSilent', () =>
    profileApi.getProfile({ silent: true }).then((r) => r.data),
);

export const connectOutlook = createAsyncThunk(
    'profile/connectOutlook',
    (body: OutlookConnection) => outlookApi.connectOutlook(body).then(() => undefined),
);

export const fetchOutlookCalendars = createAsyncThunk('profile/fetchOutlookCalendars', () =>
    outlookApi.getOutlookCalendars().then((r) => r.data),
);

export const setOutlookCalendar = createAsyncThunk(
    'profile/setOutlookCalendar',
    (body: CalendarSelection) => outlookApi.setOutlookCalendar(body).then(() => undefined),
);

export const fetchGoogleCalendars = createAsyncThunk('profile/fetchGoogleCalendars', () =>
    googleApi.getGoogleCalendars().then((r) => r.data),
);

export const setGoogleCalendar = createAsyncThunk(
    'profile/setGoogleCalendar',
    (body: CalendarSelection) => googleApi.setGoogleCalendar(body).then(() => undefined),
);

export const disconnectGoogle = createAsyncThunk('profile/disconnectGoogle', () =>
    googleApi.disconnectGoogle().then(() => undefined),
);

export const disconnectOutlook = createAsyncThunk('profile/disconnectOutlook', () =>
    outlookApi.disconnectOutlook().then(() => undefined),
);

// ── Slice ─────────────────────────────────────────────────────────────────────

interface ProfileState {
    profile: Profile | null;
    outlookCalendars: CalendarRef[];
    googleCalendars: CalendarRef[];
}

const profileSlice = createSlice({
    name: 'profile',
    initialState: { profile: null, outlookCalendars: [], googleCalendars: [] } as ProfileState,
    reducers: {},
    extraReducers: (builder) => {
        builder
            .addCase(fetchProfile.fulfilled, (state, { payload }) => {
                const profile: Profile = {
                    ...payload,
                    outlookProfilePath: truncatePath(payload.outlookProfilePath),
                };
                state.profile = profile;
            })
            .addCase(fetchProfileSilent.fulfilled, (state, { payload }) => {
                const profile: Profile = {
                    ...payload,
                    outlookProfilePath: truncatePath(payload.outlookProfilePath),
                };
                state.profile = profile;
            })
            .addCase(fetchOutlookCalendars.fulfilled, (state, { payload }) => {
                state.outlookCalendars = payload;
            })
            .addCase(fetchGoogleCalendars.fulfilled, (state, { payload }) => {
                state.googleCalendars = payload;
            });
    },
});

export default profileSlice.reducer;
