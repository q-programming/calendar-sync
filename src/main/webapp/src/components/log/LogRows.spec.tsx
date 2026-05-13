import { beforeEach, describe, expect, it, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'vitest-browser-react';
import { MemoryRouter } from 'react-router-dom';
import LogRows from './LogRows';
import { makeSyncRun } from '@/test/fixtures';
import { SyncRunStatus } from '@api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

describe('LogRows', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
    });

    it('should render a row for each log entry', () => {
        const logs = [
            makeSyncRun({ id: 'r1' }),
            makeSyncRun({ id: 'r2' }),
            makeSyncRun({ id: 'r3' }),
        ];
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={logs} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByTestId('row-log-r1')).toBeInTheDocument();
        expect(screen.getByTestId('row-log-r2')).toBeInTheDocument();
        expect(screen.getByTestId('row-log-r3')).toBeInTheDocument();
    });

    it('should render nothing when logs array is empty', () => {
        const { container } = render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(container.querySelectorAll('tr')).toHaveLength(0);
    });

    it('should show a Success chip for successful runs', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ status: SyncRunStatus.Success })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByText('SUCCESS')).toBeInTheDocument();
    });

    it('should show a Failed chip for failed runs', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ status: SyncRunStatus.Failed })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByText('FAILED')).toBeInTheDocument();
    });

    it('should show a Running chip for running sync', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ status: SyncRunStatus.Running })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByText('RUNNING')).toBeInTheDocument();
    });

    it('should display the numeric counts per row', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ created: 10, updated: 5, deleted: 2 })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByText('10')).toBeInTheDocument();
        expect(screen.getByText('5')).toBeInTheDocument();
        expect(screen.getByText('2')).toBeInTheDocument();
    });

    it('should show a dash when counts are absent', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows
                            logs={[
                                makeSyncRun({
                                    created: undefined,
                                    updated: undefined,
                                    deleted: undefined,
                                }),
                            ]}
                        />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getAllByText('-').length).toBeGreaterThanOrEqual(3);
    });

    it('should show the message when present', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ message: 'All done!' })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        expect(screen.getByText('All done!')).toBeInTheDocument();
    });

    it('should show a dash when message is absent', () => {
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ message: undefined })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );
        // At least one dash from missing message field
        expect(screen.getAllByText('-').length).toBeGreaterThanOrEqual(1);
    });

    it('should navigate to log details on row click', async () => {
        const user = userEvent.setup();
        render(
            <MemoryRouter>
                <table>
                    <tbody>
                        <LogRows logs={[makeSyncRun({ id: 'run-42' })]} />
                    </tbody>
                </table>
            </MemoryRouter>,
        );

        await user.click(screen.getByTestId('row-log-run-42'));

        expect(mockNavigate).toHaveBeenCalledWith('/logs/run-42');
    });
});
