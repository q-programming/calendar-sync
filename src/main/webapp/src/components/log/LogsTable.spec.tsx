import { describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '../../test/renderWithProviders';
import LogsTable from './LogsTable';
import { makePagedSyncRuns, makeSyncRun } from '../../test/fixtures';

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => vi.fn() };
});

describe('LogsTable', () => {
    const noopSetPage = vi.fn();

    it('should show the empty state when there are no logs', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
            initialState: { logs: { list: null, details: null } },
        });

        expect(screen.getByText('No sync logs found.')).toBeInTheDocument();
    });

    it('should show the empty state for an empty page', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
            initialState: { logs: { list: makePagedSyncRuns([]), details: null } },
        });

        expect(screen.getByText('No sync logs found.')).toBeInTheDocument();
    });

    it('should render log rows when content is present', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
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

    it('should not render pagination when there is only one page', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 1 }),
                    details: null,
                },
            },
        });

        expect(screen.queryByTestId('button-prev-page')).not.toBeInTheDocument();
        expect(screen.queryByTestId('button-next-page')).not.toBeInTheDocument();
    });

    it('should render pagination when there are multiple pages', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 3 }),
                    details: null,
                },
            },
        });

        expect(screen.getByTestId('button-prev-page')).toBeInTheDocument();
        expect(screen.getByTestId('button-next-page')).toBeInTheDocument();
    });

    it('should disable the Previous button on the first page', () => {
        renderWithProviders(<LogsTable page={0} setPage={noopSetPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 3 }),
                    details: null,
                },
            },
        });

        expect(screen.getByTestId('button-prev-page')).toBeDisabled();
        expect(screen.getByTestId('button-next-page')).not.toBeDisabled();
    });

    it('should disable the Next button on the last page', () => {
        renderWithProviders(<LogsTable page={2} setPage={noopSetPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 3 }),
                    details: null,
                },
            },
        });

        expect(screen.getByTestId('button-next-page')).toBeDisabled();
        expect(screen.getByTestId('button-prev-page')).not.toBeDisabled();
    });

    it('should display current page number and total', () => {
        renderWithProviders(<LogsTable page={1} setPage={noopSetPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 5 }),
                    details: null,
                },
            },
        });

        expect(screen.getByText('Page 2 of 5')).toBeInTheDocument();
    });

    it('should call setPage with decrement function when Previous is clicked', async () => {
        const setPage = vi.fn();
        const user = userEvent.setup();
        renderWithProviders(<LogsTable page={2} setPage={setPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 5 }),
                    details: null,
                },
            },
        });

        await user.click(screen.getByTestId('button-prev-page'));

        expect(setPage).toHaveBeenCalledOnce();
        // The argument is a function (p => Math.max(0, p - 1))
        const fn = setPage.mock.calls[0][0];
        expect(fn(2)).toBe(1);
        expect(fn(0)).toBe(0); // should not go below 0
    });

    it('should call setPage with increment function when Next is clicked', async () => {
        const setPage = vi.fn();
        const user = userEvent.setup();
        renderWithProviders(<LogsTable page={0} setPage={setPage} />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun()], { totalPages: 3 }),
                    details: null,
                },
            },
        });

        await user.click(screen.getByTestId('button-next-page'));

        expect(setPage).toHaveBeenCalledOnce();
        const fn = setPage.mock.calls[0][0];
        expect(fn(0)).toBe(1);
    });
});
