import { describe, expect, it } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { render } from 'vitest-browser-react';
import { SnackbarProvider, useSnackbar } from './snackbar-provider';

function TriggerButton({
    message,
    severity,
}: {
    message: string;
    severity?: 'success' | 'error' | 'warning' | 'info';
}) {
    const { showSnackbar } = useSnackbar();
    return <button onClick={() => showSnackbar(message, severity)}>Show</button>;
}

describe('SnackbarProvider', () => {
    it('should render children', () => {
        render(
            <SnackbarProvider>
                <span>Child Content</span>
            </SnackbarProvider>,
        );
        expect(screen.getByText('Child Content')).toBeInTheDocument();
    });

    it('should show a success snackbar with the given message', async () => {
        const user = userEvent.setup();
        render(
            <SnackbarProvider>
                <TriggerButton message='Saved!' />
            </SnackbarProvider>,
        );

        await user.click(screen.getByRole('button', { name: /show/i }));

        expect(await screen.findByText('Saved!')).toBeInTheDocument();
    });

    it('should show an error snackbar', async () => {
        const user = userEvent.setup();
        render(
            <SnackbarProvider>
                <TriggerButton message='Something went wrong' severity='error' />
            </SnackbarProvider>,
        );

        await user.click(screen.getByRole('button', { name: /show/i }));

        expect(await screen.findByText('Something went wrong')).toBeInTheDocument();
    });

    it('should close the snackbar when the close button is clicked', async () => {
        const user = userEvent.setup();
        render(
            <SnackbarProvider>
                <TriggerButton message='Close me' />
            </SnackbarProvider>,
        );

        await user.click(screen.getByRole('button', { name: /show/i }));
        await screen.findByText('Close me');

        const closeBtn = screen.getByRole('button', { name: /close/i });
        await user.click(closeBtn);

        await waitFor(() => {
            expect(screen.queryByText('Close me')).not.toBeInTheDocument();
        });
    });

    it('should provide a default no-op showSnackbar outside provider without error', () => {
        function Consumer() {
            const { showSnackbar } = useSnackbar();
            return <button onClick={() => showSnackbar('test')}>trigger</button>;
        }
        // Renders without Provider — uses context default (no-op)
        render(<Consumer />);
        expect(screen.getByRole('button')).toBeInTheDocument();
    });
});
