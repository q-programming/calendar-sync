import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { renderWithProviders } from '../test/renderWithProviders';
import LogsPage from './logs';
import { makePagedSyncRuns, makeSyncRun, makeSyncRunDetails } from '../test/fixtures';

const mockNavigate = vi.fn();
let mockParamsReturn: { logId?: string } = {};

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
        useParams: () => mockParamsReturn,
    };
});

const mock = new MockAdapter(axiosInstance);

// Restore mock once after ALL tests in this file (both describe blocks share the same mock instance)
afterAll(() => mock.restore());

describe('LogsPage — list view', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
        mockParamsReturn = {};
        mock.reset();
        mock.onGet(/\/logs/).reply(200, makePagedSyncRuns([]));
    });

    it('should render the Sync Logs heading', () => {
        renderWithProviders(<LogsPage />);
        expect(screen.getByTestId('text-page-title')).toHaveTextContent('Sync Logs');
    });

    it('should render the status filter dropdown', () => {
        renderWithProviders(<LogsPage />);
        expect(screen.getByTestId('select-status-filter')).toBeInTheDocument();
    });

    it('should dispatch fetchLogs on mount', async () => {
        const { store } = renderWithProviders(<LogsPage />);

        await waitFor(() => {
            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/pending');
        });
    });

    it('should show total count in description when logs are present', () => {
        renderWithProviders(<LogsPage />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalElements: 42, totalPages: 3 }),
                    details: null,
                },
            },
        });

        expect(screen.getByText(/42 total/i)).toBeInTheDocument();
    });

    it('should render log rows from the store', () => {
        renderWithProviders(<LogsPage />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun({ id: 'r1' }), makeSyncRun({ id: 'r2' })]),
                    details: null,
                },
            },
        });

        expect(screen.getByTestId('row-log-r1')).toBeInTheDocument();
        expect(screen.getByTestId('row-log-r2')).toBeInTheDocument();
    });

    it('should dispatch fetchLogs when the refresh button is clicked', async () => {
        const user = userEvent.setup();
        const { store } = renderWithProviders(<LogsPage />);

        // Wait for mount dispatch to settle
        await waitFor(() =>
            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/pending'),
        );

        store.reset();

        await user.click(screen.getByRole('button', { name: /refresh/i }));

        await waitFor(() => {
            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/pending');
        });
    });

    it('should dispatch fetchLogs with status filter when a filter is selected', async () => {
        const user = userEvent.setup();
        mock.onGet(/\/logs/).reply(200, makePagedSyncRuns([]));

        const { store } = renderWithProviders(<LogsPage />);

        // Wait for initial fetch
        await waitFor(() =>
            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/fulfilled'),
        );

        store.reset();

        // MUI Select: click the combobox (inner interactive element) to open the dropdown
        await user.click(screen.getByRole('combobox'));
        await user.click(await screen.findByRole('option', { name: /success/i }));

        await waitFor(() => {
            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/pending');
        });
    });
});

describe('LogsPage — detail view', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
        mockParamsReturn = { logId: 'run-99' };
        mock.reset();
        mock.onGet(/\/logs\/run-99/).reply(
            200,
            makeSyncRunDetails({ run: makeSyncRun({ id: 'run-99' }) }),
        );
    });

    it('should render LogsDetails when logId param is present', async () => {
        renderWithProviders(<LogsPage />);

        // LogsPage renders <LogsDetails> when logId is present (not the list view)
        await screen.findByTestId('log-details-view');
        expect(screen.queryByTestId('text-page-title')).not.toBeInTheDocument();
    });
});
