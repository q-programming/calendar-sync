# Calendar Sync

One-way **Outlook → Google Calendar** synchronisation application.  
Reads events from a local Outlook `.pst` / `.ost` file and mirrors them into a Google Calendar — ideal for users who run Outlook on Windows (or WSL) but prefer Google Calendar as their primary view.

---

## What it does

| Feature | Detail |
|---|---|
| **Source** | Microsoft Outlook `.pst` / `.ost` file read directly via libpst |
| **Destination** | Google Calendar (via official Google Calendar API + OAuth2) |
| **Direction** | One-way: Outlook → Google only |
| **Sync window** | Configurable days-past / days-future around today |
| **Recurring events** | Full recurrence expansion including moved/modified instances |
| **Scheduling** | Background job runs at a configurable frequency; manual trigger available |
| **Colour mapping** | Outlook appointment colours mapped to Google Calendar colours (optional) |
| **Logging** | Per-run structured log with INFO/DEBUG/WARN/ERROR levels, viewable in the UI |

---

## Architecture

```
src/
├── main/
│   ├── java/pl/qprogramming/calendarsync/
│   │   ├── adapter/          # OutlookCalendarAdapter (libpst), GoogleCalendarAdapter
│   │   ├── controller/       # REST controllers (delegate pattern from OpenAPI)
│   │   ├── entity/           # JPA entities (profile, settings, sync runs, log entries)
│   │   ├── port/             # Domain interfaces & DTOs (OutlookEvent, GoogleEvent, …)
│   │   ├── service/          # SyncService, ProfileService, SettingsService, LogService
│   │   └── scheduler/        # Dynamic scheduler (reads frequencyMinutes from DB)
│   ├── resources/
│   │   ├── swagger/api.yml   # OpenAPI contract — source of truth
│   │   └── windows-timezones.properties  # Windows TZ name → IANA mapping
│   └── webapp/               # React + TypeScript UI (built by Maven, served as static)
└── test/                     # JUnit 5 unit tests
```

The API contract is **OpenAPI-first**: `api.yml` drives code generation for both the Spring delegate interfaces and the TypeScript Axios client used by the React UI.

---

## Prerequisites

| Requirement | Version |
|---|---|
| Java | 21+ |
| Maven | 3.9+ (or use the included `mvnw`) |
| Node.js | 20+ (only needed for development builds) |
| Google Cloud project | OAuth2 credentials (see below) |

---

## Google OAuth2 setup

1. Go to [Google Cloud Console](https://console.cloud.google.com/) → **APIs & Services → Credentials**.
2. Create an **OAuth 2.0 Client ID** of type *Web application*.
3. Add `http://localhost:9090/login/oauth2/code/google` as an authorised redirect URI ( or any url if you plan to use the command-line flags to override it).
4. Note your **Client ID** and **Client Secret**.
5. Configure them in `application.yml` or via environment variables / command-line flags:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id:     YOUR_CLIENT_ID
            client-secret: YOUR_CLIENT_SECRET
```

Or pass as JVM args when launching (see scripts below).

---

## Building

```bash
# Full build (compiles Java + React, runs tests, packages fat jar)
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

The resulting jar is `target/calendarsync.jar`.

---

## Running

### Windows — `start.bat`

Place `start.bat` next to the jar and double-click (or run from cmd):

```bat
@echo off

start "CalendarSync Server" java -jar calendarsync.jar ^
 --server.port=9090 ^
 --spring.datasource.url=jdbc:h2:file:./calendarsync;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE ^
 --spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID ^
 --spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET

timeout /t 5 >nul

start msedge http://localhost:9090/calendarsync
```

> Swap `msedge` for `chrome` or `firefox` if preferred.
 
Sample bat and icons can be found in docs folder
---

### Linux / macOS / WSL — `start.sh`

It's possible to run on WSL and consume a Windows-hosted Outlook profile, but for best performance run natively on Windows if possible, as file access to the `.pst` / `.ost` under WSL is quite slow.

```bash
#!/usr/bin/env bash
set -e

GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-YOUR_CLIENT_ID}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-YOUR_CLIENT_SECRET}"

java -jar calendarsync.jar \
  --server.port=9090 \
  --spring.datasource.url="jdbc:h2:file:./calendarsync;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE" \
  --spring.security.oauth2.client.registration.google.client-id="$GOOGLE_CLIENT_ID" \
  --spring.security.oauth2.client.registration.google.client-secret="$GOOGLE_CLIENT_SECRET" \
  &

echo "Waiting for server to start..."
sleep 5

if command -v xdg-open &>/dev/null; then
  xdg-open http://localhost:9090/calendarsync
elif command -v open &>/dev/null; then
  open http://localhost:9090/calendarsync
else
  echo "Open http://localhost:9090/calendarsync in your browser"
fi
```

Make executable: `chmod +x start.sh`

> **WSL tip:** The Outlook `.pst` / `.ost` file lives on the Windows filesystem, accessible at e.g.  
> `/mnt/c/Users/<YourName>/AppData/Local/Microsoft/Outlook/YourProfile.ost`  
> Enter this path in the UI under **Profile → Outlook Profile File**. but expect slower performance due to cross-filesystem access — for best results, run the app natively on Windows.

---

## First-run walkthrough

1. Open the app in your browser (`http://localhost:9090/calendarsync`).
2. Go to **Profile** and click **Connect Google** — complete the OAuth2 flow.
3. Select your target **Google Calendar** from the dropdown.
4. Enter your **Outlook profile path** (path to the `.pst` or `.ost` file).
5. Click **Load Calendars**, select your source calendar, and save.
6. Go to **Settings** and adjust the sync window, frequency, and optional colour mapping. New sync is run if you save
7. Return to the **Home** screen and click **Run Sync Now** to trigger the sync manually.

Subsequent syncs run automatically in the background at the configured frequency.

---

## Configuration reference

| Property | Default | Description |
|---|---|---|
| `server.port` | `9090` | HTTP port |
| `spring.datasource.url` | in-memory H2 | Use `jdbc:h2:file:./calendarsync` for persistence |
| `sync.frequency-minutes` | `60` | Default sync frequency (overridden via UI settings) |
| `sync.days-past` | `14` | How many days back to sync |
| `sync.days-future` | `30` | How many days forward to sync |

All settings can also be changed at runtime through the **Settings** page in the UI.

---

## Development

```bash
# Start backend (serves UI from target/classes/static)
./mvnw spring-boot:run

# Start React dev server (proxies /api to localhost:9090)
cd src/main/webapp
npm install
npm run dev
```

The React dev server runs on `http://localhost:5173`.

### Debugging the frontend in IntelliJ

1. Add a new run configuration: **Attach to Node.js/Chrome**, host `localhost`, port `9229`.
2. Add the following Vitest options to your test run configuration:  
   `--browser --browser.headless=false --no-file-parallelism --inspect-brk --run`
3. Run the test — it opens Chromium and waits for the debugger.
4. Run the "Attach to Chromium" configuration in debug mode.

---

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Security OAuth2, Spring Data JPA, H2
- **API:** OpenAPI 3 (contract-first), openapi-generator-maven-plugin
- **Outlook reading:** [java-libpst](https://github.com/rjohnsondev/java-libpst)
- **Google Calendar:** Google Calendar API v3 (official Java client)
- **Frontend:** React 18, TypeScript, Vite, MUI (Material UI), Redux Toolkit
- **Build:** Maven (bundles React into static resources via frontend-maven-plugin)

## License

See [LICENSE](./LICENSE) for details.
