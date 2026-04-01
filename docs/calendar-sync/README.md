# Calendar Sync — Frontend

React + Vite + Material UI dashboard for one-way Outlook → Google Calendar sync.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

3. Make sure the API server is running (see `api-server/README.md`).

4. Start the dev server:
   ```bash
   npm run dev
   ```

The frontend runs on `http://localhost:5173` and proxies `/api/*` requests to the API server.

## Pages

- **Cockpit** (`/`) — Status overview of connections and sync
- **Profile** (`/profile`) — Connect Outlook & Google accounts, select calendars
- **Settings** (`/settings`) — Configure sync frequency and time window
- **Logs** (`/logs`) — Paginated sync run history with detail view

## Production Build

```bash
npm run build
npm run preview
```

For production, serve the `dist/` folder with a static file server and configure API proxying to point `/api/*` to the API server.
