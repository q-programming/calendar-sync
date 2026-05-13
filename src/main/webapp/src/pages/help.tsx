import {Layout} from '@/components/layout';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import DashboardIcon from '@mui/icons-material/Dashboard';
import DescriptionIcon from '@mui/icons-material/Description';
import GoogleIcon from '@mui/icons-material/Google';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import MonitorIcon from '@mui/icons-material/Monitor';
import PersonIcon from '@mui/icons-material/Person';
import SettingsIcon from '@mui/icons-material/Settings';
import SyncIcon from '@mui/icons-material/Sync';

interface SectionProps {
    icon: React.ReactNode;
    title: string;
    subheader: string;
    children: React.ReactNode;
}

function HelpSection({icon, title, subheader, children}: SectionProps) {
    return (
        <Card>
            <CardHeader
                avatar={icon}
                title={title}
                subheader={subheader}
                titleTypographyProps={{fontWeight: 600}}
            />
            <Divider/>
            <CardContent>
                <Stack spacing={2}>{children}</Stack>
            </CardContent>
        </Card>
    );
}

interface ItemProps {
    label: string;
    children: React.ReactNode;
}

function HelpItem({label, children}: ItemProps) {
    return (
        <Box>
            <Typography variant="body1" fontWeight={500} gutterBottom>
                {label}
            </Typography>
            <Typography variant="body2" color="text.secondary">
                {children}
            </Typography>
        </Box>
    );
}

export default function HelpPage() {
    return (
        <Layout>
            <Stack spacing={4}>
                <Box>
                    <Typography variant="h4">Help & Documentation</Typography>
                    <Typography variant="body2" sx={{mt: 1}}>
                        Everything you need to know about Calendar Sync.
                    </Typography>
                </Box>

                {/* Overview */}
                <HelpSection
                    icon={<HelpOutlineIcon sx={{color: 'primary.main'}}/>}
                    title="What is Calendar Sync?"
                    subheader="A brief introduction to the application"
                >
                    <Typography variant="body2" color="text.secondary">
                        Calendar Sync is a self-hosted application that automatically mirrors events
                        from your <strong>Microsoft Outlook</strong> calendar into{' '}
                        <strong>Google Calendar</strong>. It runs a background job on a configurable
                        schedule, reading events from a local Outlook profile file (.ost / .pst) and
                        writing them to a chosen Google Calendar — keeping the two in continuous
                        sync without any manual effort.
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                        To get started you need to:
                    </Typography>
                    <Box
                        component="ol"
                        sx={{m: 0, pl: 3, '& li': {mb: 1}}}
                    >
                        <Typography component="li" variant="body2" color="text.secondary">
                            Connect your Outlook profile file on the <strong>Profile</strong> page.
                        </Typography>
                        <Typography component="li" variant="body2" color="text.secondary">
                            Sign in with Google on the <strong>Profile</strong> page.
                        </Typography>
                        <Typography component="li" variant="body2" color="text.secondary">
                            Select source and destination calendars on the <strong>Profile</strong>{' '}
                            page.
                        </Typography>
                        <Typography component="li" variant="body2" color="text.secondary">
                            Optionally adjust the sync window and frequency in{' '}
                            <strong>Settings</strong>.
                        </Typography>
                        <Typography component="li" variant="body2" color="text.secondary">
                            Return to the <strong>Cockpit</strong> and trigger a manual sync or
                            wait for the scheduler to run.
                        </Typography>
                    </Box>
                </HelpSection>

                {/* Cockpit */}
                <HelpSection
                    icon={<DashboardIcon sx={{color: 'primary.main'}}/>}
                    title="Cockpit"
                    subheader="The main dashboard — your day-to-day view"
                >
                    <HelpItem label="Last Sync Status">
                        Shows the result of the most recent sync run: status (Success, Running,
                        Failed, Token Expired), the number of events created / updated / deleted,
                        and any message produced by that run.
                    </HelpItem>
                    <HelpItem label="Sync Now button">
                        Triggers an immediate sync on demand. The button is disabled while a run is
                        already in progress. After the sync completes the status card refreshes
                        automatically.
                    </HelpItem>
                    <HelpItem label="Google token expired">
                        If the sync fails because your Google OAuth token has been revoked or
                        expired, the Cockpit will display a warning and automatically redirect you
                        to re-authorise with Google. You can also reconnect manually from the
                        Profile page at any time.
                    </HelpItem>
                </HelpSection>

                {/* Profile */}
                <HelpSection
                    icon={<PersonIcon sx={{color: 'primary.main'}}/>}
                    title="Profile & Connections"
                    subheader="Manage your Microsoft Outlook and Google Calendar connections"
                >
                    <HelpItem label={`Source: Microsoft Outlook`}>
                        Calendar Sync reads events directly from a local Outlook data file
                        (.ost or .pst). Enter the full path to that file and click{' '}
                        <em>Connect Outlook</em>. Once connected, choose which Outlook calendar
                        to use as the source. You can clear the connection at any time with{' '}
                        <em>Remove Profile File</em>.
                    </HelpItem>
                    <Box sx={{display: 'flex', gap: 1, alignItems: 'flex-start'}}>
                        <MonitorIcon sx={{color: '#00a4ef', mt: 0.25}} fontSize="small"/>
                        <Typography variant="body2" color="text.secondary" fontFamily="monospace">
                            Example: C:\Users\you\AppData\Local\Microsoft\Outlook\you@company.ost
                        </Typography>
                    </Box>
                    <Divider/>
                    <HelpItem label="Destination: Google Calendar">
                        Click <em>Sign in with Google</em> to authorise the application via OAuth.
                        You will be redirected to Google's consent screen and returned here once
                        access is granted. Then select which Google Calendar should receive the
                        synced events. Use <em>Reconnect Google Account</em> if you ever need to
                        re-authorise, and <em>Disconnect</em> to revoke access entirely.
                    </HelpItem>
                    <Box sx={{display: 'flex', gap: 1, alignItems: 'flex-start'}}>
                        <GoogleIcon sx={{color: '#4285F4', mt: 0.25}} fontSize="small"/>
                        <Typography variant="body2" color="text.secondary">
                            Google tokens can expire. If a sync fails with a token error the
                            application will prompt you to reconnect automatically.
                        </Typography>
                    </Box>
                </HelpSection>

                {/* Settings */}
                <HelpSection
                    icon={<SettingsIcon sx={{color: 'primary.main'}}/>}
                    title="Settings"
                    subheader="Configure how and when syncing occurs"
                >
                    <HelpItem label="Sync Frequency (minutes)">
                        How often the background scheduler runs. The default is every 15 minutes.
                        Lower values keep calendars more up to date at the cost of more frequent
                        API calls. Changes take effect on the next scheduled run.
                    </HelpItem>
                    <HelpItem label="Days Past">
                        How many days into the past the sync window extends. Events that started
                        before this boundary are ignored. A value of 7 means only events from the
                        last week onwards are considered.
                    </HelpItem>
                    <HelpItem label="Days Future">
                        How many days ahead the sync window extends. Events further in the future
                        than this boundary are ignored. A value of 30 means events up to one month
                        ahead are synced.
                    </HelpItem>
                    <HelpItem label="Debug Logging">
                        When enabled, the sync engine writes detailed step-by-step output to the
                        log entries. Useful for diagnosing problems. Disable in normal operation to
                        keep logs concise.
                    </HelpItem>
                    <HelpItem label="Sync Color Labels">
                        When enabled, Outlook category colours are mapped to the closest Google
                        Calendar event colour. Disable if you prefer Google events to keep their
                        default colour.
                    </HelpItem>
                </HelpSection>

                {/* Logs */}
                <HelpSection
                    icon={<DescriptionIcon sx={{color: 'primary.main'}}/>}
                    title="Logs"
                    subheader="Inspect the history of every sync run"
                >
                    <HelpItem label="Log table">
                        Each row represents one sync run, showing the start time, status chip,
                        counts of created / updated / deleted events, and a short summary message.
                        Click any row to open the detailed log view for that run.
                    </HelpItem>
                    <HelpItem label="Status chips">
                        <Box component="span">
                            <strong>Success</strong> — all events processed without errors.{' '}
                            <strong>Running</strong> — sync is currently in progress.{' '}
                            <strong>Failed</strong> — an unexpected error stopped the run; check
                            the message column for details.{' '}
                            <strong>Token Expired</strong> — the Google OAuth token was revoked or
                            expired; reconnect Google from the Profile page.
                        </Box>
                    </HelpItem>
                    <HelpItem label="Detailed log view">
                        Clicking a row opens the per-run log lines. Each line has a level (INFO,
                        WARN, ERROR, DEBUG), a timestamp, and the message. Use this view together
                        with <em>Debug Logging</em> enabled in Settings to trace exactly what
                        happened during a sync.
                    </HelpItem>
                    <HelpItem label="Automatic cleanup">
                        Old log entries are removed automatically by the application to prevent
                        unbounded growth. Only recent history is retained.
                    </HelpItem>
                </HelpSection>

                {/* FAQ */}
                <HelpSection
                    icon={<SyncIcon sx={{color: 'primary.main'}}/>}
                    title="Common questions"
                    subheader="Quick answers to frequent issues"
                >
                    <HelpItem label="Events are not appearing in Google Calendar">
                        Check that both Outlook and Google connections are active on the Profile
                        page, the correct calendars are selected, and at least one sync has
                        completed successfully on the Cockpit. Also verify the Days Past / Days
                        Future window covers the events you expect.
                    </HelpItem>
                    <HelpItem label='Sync shows "Failed" every run'>
                        Open the log detail for the failed run and look for ERROR-level lines.
                        Common causes: the Outlook profile file has moved or been locked by another
                        process, or the Google token has expired (look for "Token Expired" status
                        instead).
                    </HelpItem>
                    <HelpItem label="How do I force an immediate sync?">
                        Use the <em>Sync Now</em> button on the Cockpit. The scheduler will still
                        run independently on its configured frequency.
                    </HelpItem>
                    <HelpItem label="The app redirected me to Google unexpectedly">
                        This is intentional. When a sync run detects that your Google token has
                        expired or been revoked, the Cockpit automatically redirects you to
                        re-authorise so syncing can resume. After signing in you will be returned
                        to the application.
                    </HelpItem>
                </HelpSection>
                <Box>
                    <a
                        href="https://www.buymeacoffee.com/q_programming"
                        target="_blank"
                        rel="noreferrer"
                        style={{display: 'block'}}
                    >
                        <img
                            src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png"
                            alt="Buy Me A Coffee"
                            style={{height: 40, width: 145}}
                        />
                    </a>
                </Box>
            </Stack>
        </Layout>
    );
}
