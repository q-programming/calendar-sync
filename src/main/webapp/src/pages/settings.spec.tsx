import { afterAll, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { renderWithProviders } from '../test/renderWithProviders';
import SettingsPage from './settings';
import { makeSettings } from '../test/fixtures';

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => vi.fn() };
});

const mock = new MockAdapter(axiosInstance);

describe('SettingsPage', () => {
    beforeEach(() => {
        mock.reset();
        mock.onPut(/\/settings/).reply(200);
    });

    afterAll(() => mock.restore());

    it('should render the System Settings heading', () => {
        renderWithProviders(<SettingsPage />);
        expect(screen.getByText('System Settings')).toBeInTheDocument();
    });

    it('should render the Sync Configuration card', () => {
        renderWithProviders(<SettingsPage />);
        expect(screen.getByText('Sync Configuration')).toBeInTheDocument();
    });

    it('should pre-fill the form with values from the store', async () => {
        renderWithProviders(<SettingsPage />, {
            initialState: {
                settings: {
                    settings: makeSettings({ frequencyMinutes: 45, daysPast: 14, daysFuture: 60 }),
                },
            },
        });

        await waitFor(() => {
            expect(screen.getByDisplayValue('45')).toBeInTheDocument();
            expect(screen.getByDisplayValue('14')).toBeInTheDocument();
            expect(screen.getByDisplayValue('60')).toBeInTheDocument();
        });
    });

    it('should show a validation error when frequency is 0', async () => {
        const user = userEvent.setup();
        renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings({ frequencyMinutes: 0 }) } },
        });

        // Wait for the form to reset to the store values
        await waitFor(() => screen.getByDisplayValue('0'));
        await user.click(screen.getByRole('button', { name: /save configuration/i }));

        await waitFor(() => {
            expect(document.body.textContent).toContain('Must be at least 1 minute');
        });
    });

    it('should show validation errors when past/future days are negative', async () => {
        const user = userEvent.setup();
        renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings({ daysPast: -1 }) } },
        });

        // Wait for the form to reset to the store values
        await waitFor(() => screen.getByDisplayValue('-1'));
        await user.click(screen.getByRole('button', { name: /save configuration/i }));

        await waitFor(() => {
            expect(document.body.textContent).toContain('Cannot be negative');
        });
    });

    it('should dispatch updateSettings on valid form submit', async () => {
        const user = userEvent.setup();
        const { store } = renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings() } },
        });

        await waitFor(() => screen.getByDisplayValue('15'));

        await user.click(screen.getByRole('button', { name: /save configuration/i }));

        await waitFor(() => {
            expect(store.getActions().map((a) => a.type)).toContain('settings/update/pending');
        });
    });

    it('should submit with updated frequency when field is changed', async () => {
        const user = userEvent.setup();
        const { store } = renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings({ frequencyMinutes: 15 }) } },
        });

        await waitFor(() => screen.getByDisplayValue('15'));

        const input = screen.getByLabelText(/sync frequency/i);
        await user.clear(input);
        await user.type(input, '60');
        await user.click(screen.getByRole('button', { name: /save configuration/i }));

        await waitFor(() => {
            const fulfilled = store
                .getActions()
                .find((a) => a.type === 'settings/update/fulfilled');
            expect(fulfilled).toMatchObject({ payload: { frequencyMinutes: 60 } });
        });
    });

    it('should render Debug Logging toggle', () => {
        renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings({ debugLogging: false }) } },
        });

        expect(screen.getByText('Debug Logging')).toBeInTheDocument();
        // Switch renders a hidden <input type="checkbox"> in the DOM
        expect(document.querySelector('input[type="checkbox"]')).toBeInTheDocument();
    });

    it('should render Sync Color Labels toggle', () => {
        renderWithProviders(<SettingsPage />, {
            initialState: { settings: { settings: makeSettings({ syncColorLabels: true }) } },
        });

        expect(screen.getByText('Sync Color Labels')).toBeInTheDocument();
    });

    it('should render the Save Configuration submit button', () => {
        renderWithProviders(<SettingsPage />);
        expect(screen.getByRole('button', { name: /save configuration/i })).toBeInTheDocument();
    });
});
