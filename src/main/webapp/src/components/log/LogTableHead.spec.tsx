import { describe, expect, it } from 'vitest';
import { screen } from '@testing-library/react';
import { render } from 'vitest-browser-react';
import LogTableHead from './LogTableHead';

describe('LogTableHead', () => {
    it('should render all column headers', () => {
        render(
            <table>
                <LogTableHead />
            </table>,
        );

        expect(screen.getByText('Started At')).toBeInTheDocument();
        expect(screen.getByText('Status')).toBeInTheDocument();
        expect(screen.getByText('Created')).toBeInTheDocument();
        expect(screen.getByText('Updated')).toBeInTheDocument();
        expect(screen.getByText('Deleted')).toBeInTheDocument();
        expect(screen.getByText('Message')).toBeInTheDocument();
    });

    it('should render exactly 6 column header cells', () => {
        render(
            <table>
                <LogTableHead />
            </table>,
        );

        expect(screen.getAllByRole('columnheader')).toHaveLength(6);
    });
});
