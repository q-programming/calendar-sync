import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { renderWithProviders } from '../../test/renderWithProviders';
import LogsDetails from './LogsDetails';
import { makeLogEntry, makeSyncRun, makeSyncRunDetails } from '../../test/fixtures';
import { LogLevel, SyncRunStatus } from '@api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

const mock = new MockAdapter(axiosInstance);

describe('LogsDetails', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
        mock.reset();
    });

    afterAll(() => mock.restore());

    it('should return nothing when details are not yet loaded', () => {
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

        const { store } = renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        // Before the async fetch resolves, details = null → renders nothing
        expect(store.getState().logs.details).toBeNull();
        expect(screen.queryByTestId('log-details-view')).not.toBeInTheDocument();
    });

    it('should dispatch fetchLogDetails on mount', async () => {
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

        const { store } = renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await waitFor(() => {
            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('logs/fetchDetails/pending');
        });
    });

    it('should render the details view once data is loaded', async () => {
        const details = makeSyncRunDetails();
        mock.onGet(/\/logs\/run-1/).reply(200, details);

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        expect(await screen.findByTestId('log-details-view')).toBeInTheDocument();
    });

    it('should display "Log Details" heading after load', async () => {
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        expect(await screen.findByText('Log Details')).toBeInTheDocument();
    });

    it('should show the run status chip', async () => {
        mock.onGet(/\/logs\/run-1/).reply(
            200,
            makeSyncRunDetails({ run: makeSyncRun({ status: SyncRunStatus.Success }) }),
        );

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('log-details-view');
        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
    });

    it('should display stat cards with created/updated/deleted counts', async () => {
        mock.onGet(/\/logs\/run-1/).reply(
            200,
            makeSyncRunDetails({ run: makeSyncRun({ created: 7, updated: 3, deleted: 1 }) }),
        );

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('log-details-view');
        expect(screen.getByText('7')).toBeInTheDocument();
        expect(screen.getByText('3')).toBeInTheDocument();
        expect(screen.getByText('1')).toBeInTheDocument();
    });

    it('should display summary message when present', async () => {
        mock.onGet(/\/logs\/run-1/).reply(
            200,
            makeSyncRunDetails({ run: makeSyncRun({ message: 'All good!' }) }),
        );

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('log-details-view');
        expect(screen.getByText('All good!')).toBeInTheDocument();
    });

    it('should list log entries with their messages', async () => {
        const entries = [
            makeLogEntry({ level: LogLevel.Info, message: 'Starting job' }),
            makeLogEntry({ level: LogLevel.Error, message: 'Connection failed' }),
        ];
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails({ entries }));

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('log-details-view');
        expect(screen.getByText('Starting job')).toBeInTheDocument();
        expect(screen.getByText('Connection failed')).toBeInTheDocument();
    });

    it('should show "No log entries recorded" when entries are empty', async () => {
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails({ entries: [] }));

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        expect(await screen.findByText(/no log entries recorded/i)).toBeInTheDocument();
    });

    it('should navigate back to /logs on back button click', async () => {
        const user = userEvent.setup();
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

        renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('button-back-to-logs');
        await user.click(screen.getByTestId('button-back-to-logs'));

        expect(mockNavigate).toHaveBeenCalledWith('/logs');
    });

    it('should dispatch clearLogDetails on unmount', async () => {
        mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

        const { store, unmount } = renderWithProviders(<LogsDetails logId='run-1' />, {
            initialState: { logs: { list: null, details: null } },
        });

        await screen.findByTestId('log-details-view');
        unmount();

        await waitFor(() => {
            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('logs/clearLogDetails');
        });
    });
});
