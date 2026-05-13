/**
 * Bridge to the openapi-generator axios client.
 * New UI pages use @/lib/api-client-react hooks instead.
 * This file remains for legacy/direct axios usage.
 */
export * from '../client-api';
export { Configuration } from '../client-api/configuration';
export {
    GoogleApi,
    HealthApi,
    LogsApi,
    OutlookApi,
    ProfileApi,
    SettingsApi,
    SyncApi,
} from '../client-api/api';
