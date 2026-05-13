import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { setupTestStore } from '../test/testStore';
import { fetchSettings, updateSettings } from './settingsSlice';
import { makeSettings } from '../test/fixtures';

const mock = new MockAdapter(axiosInstance);

describe('settingsSlice', () => {
    let store: ReturnType<typeof setupTestStore>;

    beforeEach(() => {
        store = setupTestStore();
        store.reset();
        mock.reset();
    });

    afterAll(() => mock.restore());

    describe('fetchSettings thunk', () => {
        it('should populate settings on success', async () => {
            const data = makeSettings({ frequencyMinutes: 10 });
            mock.onGet(/\/settings/).reply(200, data);

            await store.dispatch(fetchSettings());

            expect(store.getState().settings.settings).toEqual(data);
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onGet(/\/settings/).reply(200, makeSettings());

            await store.dispatch(fetchSettings());

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('settings/fetch/pending');
            expect(types).toContain('settings/fetch/fulfilled');
        });

        it('should dispatch rejected on server error', async () => {
            mock.onGet(/\/settings/).reply(500);

            await store.dispatch(fetchSettings());

            expect(store.getActions().map((a) => a.type)).toContain('settings/fetch/rejected');
        });

        it('should not change state on failure', async () => {
            mock.onGet(/\/settings/).reply(500);

            await store.dispatch(fetchSettings());

            expect(store.getState().settings.settings).toBeNull();
        });
    });

    describe('updateSettings thunk', () => {
        it('should update settings in state on success', async () => {
            const updated = makeSettings({ frequencyMinutes: 60, daysFuture: 90 });
            mock.onPut(/\/settings/).reply(200);

            await store.dispatch(updateSettings(updated));

            expect(store.getState().settings.settings).toEqual(updated);
        });

        it('should carry the sent body as the fulfilled payload', async () => {
            const body = makeSettings({ frequencyMinutes: 30 });
            mock.onPut(/\/settings/).reply(200);

            await store.dispatch(updateSettings(body));

            const fulfilled = store
                .getActions()
                .find((a) => a.type === 'settings/update/fulfilled');
            expect(fulfilled?.payload).toEqual(body);
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onPut(/\/settings/).reply(200);

            await store.dispatch(updateSettings(makeSettings()));

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('settings/update/pending');
            expect(types).toContain('settings/update/fulfilled');
        });

        it('should dispatch rejected on server error', async () => {
            mock.onPut(/\/settings/).reply(500);

            await store.dispatch(updateSettings(makeSettings()));

            expect(store.getActions().map((a) => a.type)).toContain('settings/update/rejected');
        });
    });
});
