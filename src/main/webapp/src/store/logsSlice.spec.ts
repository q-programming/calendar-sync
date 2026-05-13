import { afterAll, beforeEach, describe, expect, it } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import { axiosInstance } from '@/services/api-instance';
import { setupTestStore } from '../test/testStore';
import { clearLogDetails, fetchLogDetails, fetchLogs } from './logsSlice';
import { makePagedSyncRuns, makeSyncRun, makeSyncRunDetails } from '../test/fixtures';

const mock = new MockAdapter(axiosInstance);

describe('logsSlice', () => {
    let store: ReturnType<typeof setupTestStore>;

    beforeEach(() => {
        store = setupTestStore();
        store.reset();
        mock.reset();
    });

    afterAll(() => mock.restore());

    describe('clearLogDetails', () => {
        it('should set details to null', () => {
            store = setupTestStore({ logs: { list: null, details: makeSyncRunDetails() } });
            store.dispatch(clearLogDetails());
            expect(store.getState().logs.details).toBeNull();
        });

        it('should leave list state untouched', () => {
            const list = makePagedSyncRuns([makeSyncRun()]);
            store = setupTestStore({ logs: { list, details: makeSyncRunDetails() } });
            store.dispatch(clearLogDetails());
            expect(store.getState().logs.list).toEqual(list);
        });
    });

    describe('fetchLogs thunk', () => {
        it('should populate list on success', async () => {
            const data = makePagedSyncRuns([makeSyncRun(), makeSyncRun({ id: 'run-2' })]);
            mock.onGet(/\/logs/).reply(200, data);

            await store.dispatch(fetchLogs({ page: 0, size: 20 }));

            expect(store.getState().logs.list).toEqual(data);
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onGet(/\/logs/).reply(200, makePagedSyncRuns());

            await store.dispatch(fetchLogs({ page: 0, size: 20 }));

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('logs/fetchList/pending');
            expect(types).toContain('logs/fetchList/fulfilled');
        });

        it('should dispatch rejected action on server error', async () => {
            mock.onGet(/\/logs/).reply(500);

            await store.dispatch(fetchLogs({ page: 0, size: 20 }));

            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchList/rejected');
        });

        it('should not mutate list state on failure', async () => {
            mock.onGet(/\/logs/).reply(500);

            await store.dispatch(fetchLogs({ page: 0, size: 20 }));

            expect(store.getState().logs.list).toBeNull();
        });

        it('should track dispatched actions independently per call', async () => {
            mock.onGet(/\/logs/).reply(200, makePagedSyncRuns());

            await store.dispatch(fetchLogs({ page: 0, size: 20 }));
            store.reset();
            await store.dispatch(fetchLogs({ page: 1, size: 20 }));

            expect(store.getActions()).toHaveLength(2); // pending + fulfilled for second call only
        });
    });

    describe('fetchLogDetails thunk', () => {
        it('should populate details on success', async () => {
            const data = makeSyncRunDetails();
            mock.onGet(/\/logs\/run-1/).reply(200, data);

            await store.dispatch(fetchLogDetails('run-1'));

            expect(store.getState().logs.details).toEqual(data);
        });

        it('should dispatch pending then fulfilled actions', async () => {
            mock.onGet(/\/logs\/run-1/).reply(200, makeSyncRunDetails());

            await store.dispatch(fetchLogDetails('run-1'));

            const types = store.getActions().map((a) => a.type);
            expect(types).toContain('logs/fetchDetails/pending');
            expect(types).toContain('logs/fetchDetails/fulfilled');
        });

        it('should dispatch rejected on API failure', async () => {
            mock.onGet(/\/logs\/run-1/).reply(404);

            await store.dispatch(fetchLogDetails('run-1'));

            expect(store.getActions().map((a) => a.type)).toContain('logs/fetchDetails/rejected');
        });
    });
});
