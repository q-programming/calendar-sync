import { afterAll, afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { renderWithProviders } from '../test/renderWithProviders';
import Home from './home';
import { makePagedSyncRuns, makeProfile, makeSettings, makeSyncRun } from '../test/fixtures';
import { SyncRunStatus } from '@api';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
    return { ...actual, useNavigate: () => mockNavigate };
});

const mock = new MockAdapter(axiosInstance);

describe('Home page', () => {
    beforeEach(() => {
        mockNavigate.mockReset();
        mock.reset();
        // Silence background API calls that components make on mount
        mock.onGet(/\/logs/).reply(200, makePagedSyncRuns([]));
        mock.onGet(/\/profile/).reply(200, makeProfile());
    });

    afterAll(() => mock.restore());

    it('should render the Cockpit heading', () => {
        renderWithProviders(<Home />);
        // Layout sidebar also contains 'Cockpit' as nav text; use heading role to be specific
        expect(screen.getByRole('heading', { name: 'Cockpit' })).toBeInTheDocument();
    });

    it('should render the overview description text', () => {
        renderWithProviders(<Home />);
        expect(screen.getByText(/overview of your calendar synchronization/i)).toBeInTheDocument();
    });

    it('should show skeleton placeholders when profile is not loaded', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: { profile: null, outlookCalendars: [], googleCalendars: [] },
                settings: { settings: null },
            },
        });

        expect(document.querySelectorAll('.MuiSkeleton-root').length).toBeGreaterThan(0);
    });

    it('should show "Connected" status when Outlook is connected', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({
                        outlookConnected: true,
                        outlookCalendarName: 'Work Calendar',
                    }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        expect(screen.getAllByText('Connected').length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('Work Calendar')).toBeInTheDocument();
    });

    it('should show "Not Connected" when neither provider is linked', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({ outlookConnected: false, googleConnected: false }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        expect(screen.getAllByText('Not Connected').length).toBe(2);
    });

    it('should show sync settings summary when settings are loaded', () => {
        renderWithProviders(<Home />, {
            initialState: {
                settings: {
                    settings: makeSettings({ frequencyMinutes: 30, daysPast: 14, daysFuture: 60 }),
                },
            },
        });

        expect(screen.getByText('Every 30m')).toBeInTheDocument();
        expect(screen.getByText('-14d to +60d')).toBeInTheDocument();
    });

    it('should disable the Run Sync button when neither provider is connected', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({ outlookConnected: false, googleConnected: false }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        expect(screen.getByRole('button', { name: /run sync now/i })).toBeDisabled();
    });

    it('should disable the Run Sync button when only Outlook is connected', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({ outlookConnected: true, googleConnected: false }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        expect(screen.getByRole('button', { name: /run sync now/i })).toBeDisabled();
    });

    it('should enable the Run Sync button when both providers are connected', () => {
        renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        expect(screen.getByRole('button', { name: /run sync now/i })).not.toBeDisabled();
    });

    it('should dispatch triggerSync when Run Sync button is clicked', async () => {
        const user = userEvent.setup();
        mock.onPost(/\/sync\/run/).reply(200, makeSyncRun());
        mock.onGet(/\/profile/).reply(
            200,
            makeProfile({ outlookConnected: true, googleConnected: true }),
        );

        const { store } = renderWithProviders(<Home />, {
            initialState: {
                profile: {
                    profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                    outlookCalendars: [],
                    googleCalendars: [],
                },
            },
        });

        await user.click(screen.getByRole('button', { name: /run sync now/i }));

        await waitFor(() => {
            expect(store.getActions().map((a) => a.type)).toContain('sync/trigger/pending');
        });
    });

    it('should show "Edit Profile" and "Edit Settings" shortcut buttons', () => {
        renderWithProviders(<Home />);

        expect(screen.getByRole('link', { name: /edit profile/i })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /edit settings/i })).toBeInTheDocument();
    });

    it('should render the LastSyncCard when log list is populated', () => {
        renderWithProviders(<Home />, {
            initialState: {
                logs: {
                    list: makePagedSyncRuns([makeSyncRun({ id: 'latest-run' })]),
                    details: null,
                },
            },
        });

        expect(screen.getByText('Last Sync')).toBeInTheDocument();
    });

    describe('Google token expiry redirect', () => {
        afterEach(() => {
            vi.useRealTimers();
            vi.unstubAllGlobals();
        });

        it('should show warning snackbar when googleTokenExpired is true', async () => {
            renderWithProviders(<Home />, {
                initialState: {
                    profile: {
                        profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                        outlookCalendars: [],
                        googleCalendars: [],
                    },
                    sync: { lastRun: null, googleTokenExpired: true },
                },
            });

            await waitFor(() => {
                expect(screen.getByText(/google token expired/i)).toBeInTheDocument();
            });
        });

        it('should render a warning-severity Alert when googleTokenExpired is true', async () => {
            renderWithProviders(<Home />, {
                initialState: {
                    profile: {
                        profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                        outlookCalendars: [],
                        googleCalendars: [],
                    },
                    sync: { lastRun: null, googleTokenExpired: true },
                },
            });

            await waitFor(() => {
                // MUI Alert with severity="warning" receives this class
                expect(document.querySelector('.MuiAlert-colorWarning')).toBeTruthy();
            });
        });

        it('should NOT show the token-expiry snackbar when googleTokenExpired is false', async () => {
            renderWithProviders(<Home />, {
                initialState: {
                    profile: {
                        profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                        outlookCalendars: [],
                        googleCalendars: [],
                    },
                    sync: { lastRun: null, googleTokenExpired: false },
                },
            });

            // Give effects time to run
            await waitFor(() => {
                expect(screen.queryByText(/google token expired/i)).not.toBeInTheDocument();
            });
        });

        it('should dispatch setGoogleTokenExpired(true) when onSyncDone gets a GOOGLE_TOKEN_EXPIRED log', async () => {
            const tokenExpiredRun = makeSyncRun({ status: SyncRunStatus.GoogleTokenExpired });
            mock.onPost(/\/sync\/run/).reply(200, makeSyncRun({ status: SyncRunStatus.Running }));
            mock.onGet(/\/profile/).reply(
                200,
                makeProfile({ outlookConnected: true, googleConnected: true, syncRunning: false }),
            );
            mock.onGet(/\/logs/).reply(200, makePagedSyncRuns([tokenExpiredRun]));

            const { store } = renderWithProviders(<Home />, {
                initialState: {
                    profile: {
                        profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                        outlookCalendars: [],
                        googleCalendars: [],
                    },
                },
            });

            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: /run sync now/i }));

            await waitFor(() => {
                expect(store.getState().sync.googleTokenExpired).toBe(true);
            });
        });

        it('should NOT dispatch setGoogleTokenExpired when onSyncDone gets a SUCCESS log', async () => {
            const successRun = makeSyncRun({ status: SyncRunStatus.Success });
            mock.onPost(/\/sync\/run/).reply(200, makeSyncRun({ status: SyncRunStatus.Running }));
            mock.onGet(/\/profile/).reply(
                200,
                makeProfile({ outlookConnected: true, googleConnected: true, syncRunning: false }),
            );
            mock.onGet(/\/logs/).reply(200, makePagedSyncRuns([successRun]));

            const { store } = renderWithProviders(<Home />, {
                initialState: {
                    profile: {
                        profile: makeProfile({ outlookConnected: true, googleConnected: true }),
                        outlookCalendars: [],
                        googleCalendars: [],
                    },
                },
            });

            const user = userEvent.setup();
            await user.click(screen.getByRole('button', { name: /run sync now/i }));

            await waitFor(() => {
                expect(store.getActions().map((a) => a.type)).toContain('sync/trigger/fulfilled');
            });

            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });
    });
});
