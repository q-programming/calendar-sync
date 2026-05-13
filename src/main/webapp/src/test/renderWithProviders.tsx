import type { ReactElement } from 'react';
import { render } from 'vitest-browser-react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import { SnackbarProvider } from '@/components/snackbar-provider';
import theme from '@/theme';
import { setupTestStore, defaultRootState } from './testStore';
import type { RootState } from '../store/store';
import type { TestStore } from './testStore';

export interface RenderWithProvidersOptions {
    initialState?: Partial<RootState>;
    route?: string;
}

export interface RenderWithProvidersResult {
    store: TestStore;
    unmount: () => void;
}

/**
 * Renders a React element inside all required providers:
 *  - Redux store (test-configured with action tracking)
 *  - MemoryRouter (for react-router-dom hooks/links)
 *  - MUI ThemeProvider + CssBaseline
 *  - SnackbarProvider
 *
 * Returns the render result plus the test store for action/state assertions.
 */
export function renderWithProviders(
    ui: ReactElement,
    { initialState = {}, route = '/' }: RenderWithProvidersOptions = {},
): RenderWithProvidersResult {
    const store = setupTestStore({ ...defaultRootState, ...initialState });

    const { unmount } = render(
        <Provider store={store}>
            <MemoryRouter initialEntries={[route]}>
                <ThemeProvider theme={theme}>
                    <CssBaseline />
                    <SnackbarProvider>{ui}</SnackbarProvider>
                </ThemeProvider>
            </MemoryRouter>
        </Provider>,
    );

    return { store, unmount };
}
