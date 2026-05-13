import { createSlice } from '@reduxjs/toolkit';

/**
 * Tracks in-flight axios requests.
 * Incremented by request interceptor, decremented by response/error interceptor.
 * Used by GlobalLoader to show an overlay while any API call is pending.
 */
const loadingSlice = createSlice({
    name: 'loading',
    initialState: { pending: 0 },
    reducers: {
        requestStarted: (state) => {
            state.pending += 1;
        },
        requestFinished: (state) => {
            state.pending = Math.max(0, state.pending - 1);
        },
    },
});

export const { requestStarted, requestFinished } = loadingSlice.actions;
export const selectIsLoading = (state: { loading: { pending: number } }) =>
    state.loading.pending > 0;
export default loadingSlice.reducer;
