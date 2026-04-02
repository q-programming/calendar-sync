import axios from 'axios';
import { Configuration } from '@api';

export const axiosInstance = axios.create({ withCredentials: true });

// basePath becomes the prefix for every generated API call:
// e.g. /calendarsync + /api/settings → /calendarsync/api/settings
// Vite dev proxy forwards /calendarsync/api/* → http://localhost:8080
// Spring Boot context-path is /calendarsync, so it sees /api/settings
export const apiConfig = new Configuration({ basePath: '/calendarsync/api' });
