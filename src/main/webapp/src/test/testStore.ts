import { combineReducers, configureStore, createAction } from '@reduxjs/toolkit';
import type { EnhancedStore, Middleware, UnknownAction, PayloadAction } from '@reduxjs/toolkit';
import profileReducer from '../store/profileSlice';
import settingsReducer from '../store/settingsSlice';
import syncReducer from '../store/syncSlice';
import logsReducer from '../store/logsSlice';
import loadingReducer from '../store/loadingSlice';
import type { RootState, AppDispatch } from '../store/store';

export const defaultRootState: RootState = {
    profile: { profile: null, outlookCalendars: [], googleCalendars: [] },
    settings: { settings: null },
    sync: { lastRun: null, googleTokenExpired: false },
    logs: { list: null, details: null },
    loading: { pending: 0 },
};

const rootReducer = combineReducers({
    profile: profileReducer,
    settings: settingsReducer,
    sync: syncReducer,
    logs: logsReducer,
    loading: loadingReducer,
});

export const resetAction = createAction('RESET_STORE');

export const resetStore = (store: { dispatch: AppDispatch }) => store.dispatch(resetAction());

const createRootReducer = (initialState: RootState) => {
    return (state: RootState | undefined, action: PayloadAction) => {
        if (action.type === resetAction.type) {
            return initialState;
        }
        return rootReducer(state, action);
    };
};

export type TestStore = Omit<EnhancedStore<RootState>, 'dispatch'> & {
    dispatch: AppDispatch;
    getActions: () => UnknownAction[];
    reset: () => void;
};

/**
 * Creates a test-configured Redux store with action tracking and reset capability.
 *
 * @param state - Partial initial state merged over the default root state.
 * @param noInitialStateClone - When true, skips deep-cloning the initial state (useful for performance).
 */
export function setupTestStore(
    state?: Partial<RootState>,
    noInitialStateClone?: boolean,
): TestStore {
    const actions: UnknownAction[] = [];
    const fullState: RootState = { ...defaultRootState, ...state };
    const preloadedState = noInitialStateClone ? fullState : structuredClone(fullState);

    const trackActionsMiddleware: Middleware<object, RootState> = () => (next) => (action) => {
        const typedAction = action as UnknownAction;
        if (typedAction.type === resetAction.type) {
            actions.length = 0;
        } else {
            actions.push(typedAction);
        }
        return next(typedAction);
    };

    const store = configureStore({
        middleware: (getDefaultMiddleware) =>
            getDefaultMiddleware({ serializableCheck: false }).concat(trackActionsMiddleware),
        preloadedState,
        reducer: createRootReducer(preloadedState),
    });

    return {
        ...store,
        getActions: () => actions,
        reset: () => store.dispatch(resetAction()),
    };
}
