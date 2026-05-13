import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { setupTestStore } from '../test/testStore';
import {
    connectOutlook,
    disconnectGoogle,
    disconnectOutlook,
    fetchGoogleCalendars,
    fetchOutlookCalendars,
    fetchProfile,
    fetchProfileSilent,
    setGoogleCalendar,
    setOutlookCalendar,
} from './profileSlice';
import { makeCalendarRef, makeProfile } from '../test/fixtures';

const mock = new MockAdapter(axiosInstance);

describe('profileSlice', () => {
    let store: ReturnType<typeof setupTestStore>;

    beforeEach(() => {
        store = setupTestStore();
        store.reset();
        mock.reset();
    });

    afterAll(() => mock.restore());

    describe('initial state', () => {
        it('should have null profile and empty calendar lists', () => {
            const { profile, outlookCalendars, googleCalendars } = store.getState().profile;
            expect(profile).toBeNull();
            expect(outlookCalendars).toEqual([]);
            expect(googleCalendars).toEqual([]);
        });
    });

    describe('fetchProfile thunk', () => {
        it('should populate profile state on success', async () => {
            const data = makeProfile({ outlookConnected: true, googleConnected: true });
            mock.onGet(/\/profile/).reply(200, data);

            await store.dispatch(fetchProfile());

            expect(store.getState().profile.profile).toMatchObject({
                outlookConnected: true,
                googleConnected: true,
            });
        });

        it('should truncate long outlookProfilePath to ≤50 chars', async () => {
            const longPath =
                'C:\\Users\\SomeLongUsername\\AppData\\Local\\Microsoft\\Outlook\\VeryLongProfileName.ost';
            mock.onGet(/\/profile/).reply(
                200,
                makeProfile({ outlookConnected: true, outlookProfilePath: longPath }),
            );

            await store.dispatch(fetchProfile());

            const saved = store.getState().profile.profile;
            expect((saved?.outlookProfilePath ?? '').length).toBeLessThanOrEqual(50);
        });

        it('should leave short outlookProfilePath unchanged', async () => {
            const shortPath = 'C:\\profile.ost';
            mock.onGet(/\/profile/).reply(
                200,
                makeProfile({ outlookConnected: true, outlookProfilePath: shortPath }),
            );

            await store.dispatch(fetchProfile());

            expect(store.getState().profile.profile?.outlookProfilePath).toBe(shortPath);
        });

        it('should handle missing outlookProfilePath gracefully', async () => {
            mock.onGet(/\/profile/).reply(200, makeProfile());

            await store.dispatch(fetchProfile());

            // truncatePath('') returns '' for undefined/null path
            expect(store.getState().profile.profile?.outlookProfilePath).toBe('');
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onGet(/\/profile/).reply(200, makeProfile());

            await store.dispatch(fetchProfile());

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('profile/fetch/pending');
            expect(types).toContain('profile/fetch/fulfilled');
        });

        it('should dispatch rejected on server error', async () => {
            mock.onGet(/\/profile/).reply(500);

            await store.dispatch(fetchProfile());

            expect(store.getActions().map((a) => a.type)).toContain('profile/fetch/rejected');
        });
    });

    describe('fetchProfileSilent thunk', () => {
        it('should update profile state just like fetchProfile', async () => {
            const data = makeProfile({ syncRunning: true });
            mock.onGet(/\/profile/).reply(200, data);

            await store.dispatch(fetchProfileSilent());

            expect(store.getState().profile.profile?.syncRunning).toBe(true);
        });

        it('should dispatch profile/fetchSilent actions', async () => {
            mock.onGet(/\/profile/).reply(200, makeProfile());

            await store.dispatch(fetchProfileSilent());

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('profile/fetchSilent/pending');
            expect(types).toContain('profile/fetchSilent/fulfilled');
        });
    });

    describe('fetchOutlookCalendars thunk', () => {
        it('should populate outlookCalendars on success', async () => {
            const calendars = [makeCalendarRef({ id: 'cal-1', name: 'Inbox' })];
            mock.onGet(/\/profile\/outlook\/calendars/).reply(200, calendars);

            await store.dispatch(fetchOutlookCalendars());

            expect(store.getState().profile.outlookCalendars).toEqual(calendars);
        });

        it('should dispatch fulfilled action', async () => {
            mock.onGet(/\/profile\/outlook\/calendars/).reply(200, []);

            await store.dispatch(fetchOutlookCalendars());

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/fetchOutlookCalendars/fulfilled',
            );
        });
    });

    describe('fetchGoogleCalendars thunk', () => {
        it('should populate googleCalendars on success', async () => {
            const calendars = [makeCalendarRef({ id: 'gcal-1', name: 'Primary' })];
            mock.onGet(/\/profile\/google\/calendars/).reply(200, calendars);

            await store.dispatch(fetchGoogleCalendars());

            expect(store.getState().profile.googleCalendars).toEqual(calendars);
        });
    });

    describe('connectOutlook thunk', () => {
        it('should dispatch pending then fulfilled actions', async () => {
            mock.onPost(/\/profile\/outlook\/connect/).reply(200);

            await store.dispatch(connectOutlook({ profilePath: 'C:\\profile.ost' }));

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('profile/connectOutlook/pending');
            expect(types).toContain('profile/connectOutlook/fulfilled');
        });

        it('should dispatch rejected on failure', async () => {
            mock.onPost(/\/profile\/outlook\/connect/).reply(400);

            await store.dispatch(connectOutlook({ profilePath: 'bad-path' }));

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/connectOutlook/rejected',
            );
        });
    });

    describe('setOutlookCalendar thunk', () => {
        it('should dispatch fulfilled when calendar selection succeeds', async () => {
            mock.onPut(/\/profile\/outlook\/calendar/).reply(200);

            await store.dispatch(setOutlookCalendar({ calendarId: 'cal-1' }));

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/setOutlookCalendar/fulfilled',
            );
        });
    });

    describe('setGoogleCalendar thunk', () => {
        it('should dispatch fulfilled when calendar selection succeeds', async () => {
            mock.onPut(/\/profile\/google\/calendar/).reply(200);

            await store.dispatch(setGoogleCalendar({ calendarId: 'gcal-1' }));

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/setGoogleCalendar/fulfilled',
            );
        });
    });

    describe('disconnectGoogle thunk', () => {
        it('should dispatch fulfilled on success', async () => {
            mock.onDelete(/\/profile\/google\/disconnect/).reply(200);

            await store.dispatch(disconnectGoogle());

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/disconnectGoogle/fulfilled',
            );
        });
    });

    describe('disconnectOutlook thunk', () => {
        it('should dispatch fulfilled on success', async () => {
            mock.onDelete(/\/profile\/outlook\/disconnect/).reply(200);

            await store.dispatch(disconnectOutlook());

            expect(store.getActions().map((a) => a.type)).toContain(
                'profile/disconnectOutlook/fulfilled',
            );
        });
    });
});
