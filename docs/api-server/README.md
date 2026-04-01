# Calendar Sync — API Server

Express + PostgreSQL backend for the Calendar Sync dashboard.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy `.env.example` to `.env` and fill in your PostgreSQL connection string:
   ```bash
   cp .env.example .env
   ```

3. Push the database schema:
   ```bash
   npm run db:push
   ```

4. Start the server:
   ```bash
   npm run dev
   ```

The API runs on `http://localhost:3001` by default (set `PORT` in `.env`).

## API Endpoints

- `GET /api/health` — Health check
- `GET /api/profile` — Get user profile & connection status
- `POST /api/profile/outlook/connect` — Connect Outlook with profile path
- `GET /api/outlook/calendars` — List Outlook calendars
- `POST /api/outlook/calendar` — Set active Outlook calendar
- `GET /api/google/calendars` — List Google calendars
- `POST /api/google/calendar` — Set active Google calendar
- `GET /api/settings` — Get sync settings
- `PUT /api/settings` — Update sync settings
- `GET /api/logs` — Get paginated sync run history
- `GET /api/logs/:id` — Get sync run details
- `POST /api/sync/trigger` — Trigger a manual sync
