import { describe, it, expect, beforeEach } from 'vitest';
import { setupTestStore } from '../test/testStore';
import { requestFinished, requestStarted, selectIsLoading } from './loadingSlice';

describe('loadingSlice', () => {
    let store: ReturnType<typeof setupTestStore>;

    beforeEach(() => {
        store = setupTestStore();
    });

    describe('initial state', () => {
        it('should initialise with pending count of 0', () => {
            expect(store.getState().loading.pending).toBe(0);
        });
    });

    describe('requestStarted', () => {
        it('should increment pending by 1', () => {
            store.dispatch(requestStarted());
            expect(store.getState().loading.pending).toBe(1);
        });

        it('should accumulate multiple in-flight requests', () => {
            store.dispatch(requestStarted());
            store.dispatch(requestStarted());
            store.dispatch(requestStarted());
            expect(store.getState().loading.pending).toBe(3);
        });
    });

    describe('requestFinished', () => {
        it('should decrement pending when a request completes', () => {
            store.dispatch(requestStarted());
            store.dispatch(requestFinished());
            expect(store.getState().loading.pending).toBe(0);
        });

        it('should not go below 0 (no negative pending)', () => {
            store.dispatch(requestFinished());
            expect(store.getState().loading.pending).toBe(0);
        });

        it('should correctly track partially completed requests', () => {
            store.dispatch(requestStarted());
            store.dispatch(requestStarted());
            store.dispatch(requestFinished());
            expect(store.getState().loading.pending).toBe(1);
        });
    });

    describe('selectIsLoading selector', () => {
        it('should return false when no requests are in flight', () => {
            expect(selectIsLoading(store.getState())).toBe(false);
        });

        it('should return true when at least one request is pending', () => {
            store.dispatch(requestStarted());
            expect(selectIsLoading(store.getState())).toBe(true);
        });

        it('should return false after all requests complete', () => {
            store.dispatch(requestStarted());
            store.dispatch(requestStarted());
            store.dispatch(requestFinished());
            store.dispatch(requestFinished());
            expect(selectIsLoading(store.getState())).toBe(false);
        });
    });

    describe('reset', () => {
        it('should restore initial state after reset', () => {
            store.dispatch(requestStarted());
            store.dispatch(requestStarted());
            store.reset();
            expect(store.getState().loading.pending).toBe(0);
            expect(store.getActions()).toHaveLength(0);
        });
    });
});
