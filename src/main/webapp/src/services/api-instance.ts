import axios from 'axios';
import { Configuration } from '@api';

/**
 * Base path matches the Spring Boot context path + API prefix.
 * The generated client appends paths like /api/profile, so basePath here
 * is the context-path prefix only.
 */
export const BASE_PATH = '/calendarsync';

export const axiosInstance = axios.create({
  baseURL: BASE_PATH,
  withCredentials: true,
});

/**
 * Shared Configuration for all generated API classes.
 * basePath is set to empty string because axiosInstance already has the baseURL,
 * and the generated paths start with /api/...
 */
export const apiConfig = new Configuration({ basePath: '' });
