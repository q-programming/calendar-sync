import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { setupTestStore } from '../test/testStore';
import { triggerSync, setGoogleTokenExpired } from './syncSlice';
import { makeSyncRun } from '../test/fixtures';
import { SyncRunStatus } from '@api';

const mock = new MockAdapter(axiosInstance);

describe('syncSlice', () => {
    let store: ReturnType<typeof setupTestStore>;

    beforeEach(() => {
        store = setupTestStore();
        store.reset();
        mock.reset();
    });

    afterAll(() => mock.restore());

    describe('initial state', () => {
        it('should have lastRun as null', () => {
            expect(store.getState().sync.lastRun).toBeNull();
        });

        it('should have googleTokenExpired as false', () => {
            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });
    });

    describe('setGoogleTokenExpired action', () => {
        it('should set googleTokenExpired to true', () => {
            store.dispatch(setGoogleTokenExpired(true));
            expect(store.getState().sync.googleTokenExpired).toBe(true);
        });

        it('should set googleTokenExpired back to false', () => {
            store.dispatch(setGoogleTokenExpired(true));
            store.dispatch(setGoogleTokenExpired(false));
            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });
    });

    describe('triggerSync thunk', () => {
        it('should set lastRun on success', async () => {
            const run = makeSyncRun({ status: SyncRunStatus.Running });
            mock.onPost(/\/sync\/run/).reply(200, run);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.lastRun).toEqual(run);
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onPost(/\/sync\/run/).reply(200, makeSyncRun());

            await store.dispatch(triggerSync());

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('sync/trigger/pending');
            expect(types).toContain('sync/trigger/fulfilled');
        });

        it('should dispatch rejected on server error', async () => {
            mock.onPost(/\/sync\/run/).reply(500);

            await store.dispatch(triggerSync());

            expect(store.getActions().map((a) => a.type)).toContain('sync/trigger/rejected');
        });

        it('should not update lastRun on failure', async () => {
            mock.onPost(/\/sync\/run/).reply(500);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.lastRun).toBeNull();
        });

        it('should set googleTokenExpired to true when run status is GOOGLE_TOKEN_EXPIRED', async () => {
            const run = makeSyncRun({ status: SyncRunStatus.GoogleTokenExpired });
            mock.onPost(/\/sync\/run/).reply(200, run);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.googleTokenExpired).toBe(true);
        });

        it('should NOT set googleTokenExpired when run status is SUCCESS', async () => {
            const run = makeSyncRun({ status: SyncRunStatus.Success });
            mock.onPost(/\/sync\/run/).reply(200, run);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });

        it('should NOT set googleTokenExpired when run status is FAILED', async () => {
            const run = makeSyncRun({ status: SyncRunStatus.Failed });
            mock.onPost(/\/sync\/run/).reply(200, run);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });

        it('should NOT set googleTokenExpired when run status is RUNNING', async () => {
            const run = makeSyncRun({ status: SyncRunStatus.Running });
            mock.onPost(/\/sync\/run/).reply(200, run);

            await store.dispatch(triggerSync());

            expect(store.getState().sync.googleTokenExpired).toBe(false);
        });
    });
});
