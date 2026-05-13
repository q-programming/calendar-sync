import { afterEach, describe, expect, it } from 'vitest';
import { waitFor } from '@testing-library/react';
import { renderWithProviders } from '../test/renderWithProviders';
import { GlobalLoader } from './global-loader';
import { requestFinished, requestStarted } from '@/store/loadingSlice';

/**
 * MUI Backdrop always renders children in the DOM (even when closed).
 * It applies `visibility: hidden` to the root element when `open={false}` (Fade exited state).
 * We check this inline style to determine whether the spinner is actually visible.
 */
const isProgressVisible = () => {
    const backdrop = document.querySelector('.MuiBackdrop-root') as HTMLElement | null;
    if (!backdrop) return false;
    return backdrop.style.visibility !== 'hidden';
};

describe('GlobalLoader', () => {
    it('should not render the spinner when idle', () => {
        renderWithProviders(<GlobalLoader />);
        // Backdrop in 'exited' state has visibility:hidden applied by MUI Fade
        expect(isProgressVisible()).toBe(false);
    });

    it('should render the spinner after 150ms delay when a request is in flight', async () => {
        // Pre-populate store so isLoading=true immediately; component sets a 150ms timer then shows
        renderWithProviders(<GlobalLoader />, {
            initialState: { loading: { pending: 1 } },
        });

        await waitFor(() => expect(isProgressVisible()).toBe(true), {
            timeout: 2000,
            interval: 50,
        });
    });

    it('should hide the spinner when the request finishes', async () => {
        const { store } = renderWithProviders(<GlobalLoader />, {
            initialState: { loading: { pending: 1 } },
        });

        await waitFor(() => expect(isProgressVisible()).toBe(true), {
            timeout: 2000,
            interval: 50,
        });

        store.dispatch(requestFinished());

        // MUI Fade exit transition takes ~225ms; wait for it to complete
        await waitFor(() => expect(isProgressVisible()).toBe(false), {
            timeout: 2000,
            interval: 50,
        });
    });

    it('should NOT show the spinner if loading stops before the 150ms threshold', async () => {
        const { store } = renderWithProviders(<GlobalLoader />);

        store.dispatch(requestStarted());
        // Stop immediately — before the 150ms timer fires; timer is cleared by effect cleanup
        store.dispatch(requestFinished());

        // Wait beyond the threshold to confirm spinner never appeared
        await new Promise<void>((r) => setTimeout(r, 300));

        expect(isProgressVisible()).toBe(false);
    });
});

afterEach(() => {
    /* cleanup handled by vitest-browser-react */
});
