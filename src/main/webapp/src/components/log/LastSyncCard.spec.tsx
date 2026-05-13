import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../test/renderWithProviders';
import LastSyncCard from './LastSyncCard';
import { makePagedSyncRuns, makeSyncRun } from '@/test/fixtures';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('LastSyncCard', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
    });

    it('should show the empty state when there are no logs', () => {
        renderWithProviders(<LastSyncCard />, {
            initialState: { logs: { list: null, details: null } },
        });

        expect(screen.getByText('No sync runs yet.')).toBeInTheDocument();
    });

    it('should show the empty state when the log list is empty', () => {
        renderWithProviders(<LastSyncCard />, {
            initialState: { logs: { list: makePagedSyncRuns([]), details: null } },
        });

        expect(screen.getByText('No sync runs yet.')).toBeInTheDocument();
    });

    it('should render the table when logs are present', () => {
        renderWithProviders(<LastSyncCard />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun({ id: 'r1' })]),
                    details: null,
                },
            },
        });

        expect(screen.getByTestId('row-log-r1')).toBeInTheDocument();
    });

    it('should respect the maxRows limit', () => {
        const runs = [
            makeSyncRun({ id: 'r1' }),
            makeSyncRun({ id: 'r2' }),
            makeSyncRun({ id: 'r3' }),
        ];
        renderWithProviders(<LastSyncCard maxRows={2} />, {
            initialState: {
                logs: { list: makePagedSyncRuns(runs), details: null },
            },
        });

        expect(screen.getByTestId('row-log-r1')).toBeInTheDocument();
        expect(screen.getByTestId('row-log-r2')).toBeInTheDocument();
        expect(screen.queryByTestId('row-log-r3')).not.toBeInTheDocument();
    });

    it('should default to maxRows=1', () => {
        const runs = [makeSyncRun({ id: 'r1' }), makeSyncRun({ id: 'r2' })];
        renderWithProviders(<LastSyncCard />, {
            initialState: { logs: { list: makePagedSyncRuns(runs), details: null } },
        });

        expect(screen.getByTestId('row-log-r1')).toBeInTheDocument();
        expect(screen.queryByTestId('row-log-r2')).not.toBeInTheDocument();
    });

    it('should render the "Last Sync" card title', () => {
        renderWithProviders(<LastSyncCard />, {
            initialState: { logs: { list: null, details: null } },
        });
        expect(screen.getByText('Last Sync')).toBeInTheDocument();
    });

    it('should navigate to /logs when "View All" is clicked', async () => {
        const user = userEvent.setup();
        renderWithProviders(<LastSyncCard />, {
            initialState: { logs: { list: null, details: null } },
        });

        await user.click(screen.getByRole('button', { name: /view all/i }));

        expect(mockNavigate).toHaveBeenCalledWith('/logs');
    });
});
