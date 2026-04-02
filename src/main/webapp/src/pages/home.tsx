import {Layout} from '@/components/layout';
import {useAppDispatch, useAppSelector} from '@/store/hooks';
import {triggerSync} from '@/store/syncSlice';
import {fetchProfileSilent} from '@/store/profileSlice';
import {fetchLogs} from '@/store/logsSlice';
import {useSnackbar} from '@/components/snackbar-provider';
import {useCallback, useEffect, useRef} from 'react';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';
import CircularProgress from '@mui/material/CircularProgress';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import ArrowForwardIcon from '@mui/icons-material/ArrowForward';
import {Link} from 'react-router-dom';
import LastSyncCard from '@/components/log/LastSyncCard';

export default function Home() {
    const dispatch = useAppDispatch();
    const profile = useAppSelector(s => s.profile.profile);
    const settings = useAppSelector(s => s.settings.settings);
    const list = useAppSelector(s => s.logs.list);
    const {showSnackbar} = useSnackbar();
    const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);

    const syncRunning = profile?.syncRunning ?? false;

    const fetchCurrentLogs = useCallback(() => {
        dispatch(fetchLogs({
            page: 0,
            size: 1,
        }));
    }, [dispatch]);

    // Poll /api/profile every 2s while sync is running so the button reflects live state
    useEffect(() => {
        fetchCurrentLogs();
        if (syncRunning) {
            if (!pollRef.current) {
                pollRef.current = setInterval(() => {
                    dispatch(fetchProfileSilent());
                }, 2000);
            }
        } else {
            if (pollRef.current) {
                clearInterval(pollRef.current);
                pollRef.current = null;
                dispatch(fetchLogs({}));
            }
        }
        return () => {
            if (pollRef.current) {
                clearInterval(pollRef.current);
                pollRef.current = null;
            }
        };
    }, [syncRunning, dispatch, fetchCurrentLogs]);

    const handleRunSync = () => {
        dispatch(triggerSync()).then(() => {
            showSnackbar('Sync started.', 'success');
            dispatch(fetchProfileSilent());
        });
    };

    const StatusIcon = ({connected}: { connected?: boolean }) =>
        connected
            ? <CheckCircleIcon sx={{color: 'success.main'}}/>
            : <CancelIcon sx={{color: 'text.disabled'}}/>;

    return (
        <Layout>
            <Stack spacing={4}>
                <Box>
                    <Typography variant="h4">Cockpit</Typography>
                    <Typography variant="body2" sx={{mt: 1}}>
                        Overview of your calendar synchronization status.
                    </Typography>
                </Box>

                <Box sx={{display: 'grid', gridTemplateColumns: {xs: '1fr', md: '1fr 1fr'}, gap: 3}}>
                    {[
                        {
                            title: 'Microsoft Outlook',
                            connected: profile?.outlookConnected,
                            calendarName: profile?.outlookCalendarName
                        },
                        {
                            title: 'Google Calendar',
                            connected: profile?.googleConnected,
                            calendarName: profile?.googleCalendarName
                        },
                    ].map(({title, connected, calendarName}) => (
                        <Card key={title}>
                            <CardHeader
                                title={title}
                                titleTypographyProps={{variant: 'subtitle1', fontWeight: 600}}
                                action={profile === null ? undefined : <StatusIcon connected={connected}/>}
                            />
                            <CardContent sx={{pt: 0}}>
                                {profile === null ? (
                                    <Skeleton variant="rounded" height={40}/>
                                ) : connected ? (
                                    <Box>
                                        <Typography variant="body2"
                                                    sx={{color: 'success.main', fontWeight: 500}}>Connected</Typography>
                                        <Typography variant="body2" sx={{fontFamily: 'monospace', mt: 0.5}}>
                                            {calendarName || 'No calendar selected'}
                                        </Typography>
                                    </Box>
                                ) : (
                                    <Box>
                                        <Typography variant="body2">Not Connected</Typography>
                                        <Typography variant="body2">Authenticate in Profile.</Typography>
                                    </Box>
                                )}
                            </CardContent>
                        </Card>
                    ))}
                </Box>

                <Box sx={{display: 'grid', gridTemplateColumns: {xs: '1fr', md: '2fr 1fr'}, gap: 3}}>
                    <Card>
                        <CardHeader
                            title="Sync Settings Summary"
                            subheader="Current configuration for background jobs"
                            titleTypographyProps={{variant: 'subtitle1', fontWeight: 600}}
                        />
                        <CardContent sx={{pt: 0}}>
                            {settings === null ? (
                                <Stack spacing={1}>
                                    <Skeleton variant="rounded" height={20} width="33%"/>
                                    <Skeleton variant="rounded" height={20} width="50%"/>
                                </Stack>
                            ) : (
                                <Box sx={{display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2}}>
                                    <Box>
                                        <Typography variant="body2">Frequency</Typography>
                                        <Typography variant="body1" sx={{fontFamily: 'monospace', mt: 0.5}}>
                                            Every {settings.frequencyMinutes}m
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography variant="body2">Window</Typography>
                                        <Typography variant="body1" sx={{fontFamily: 'monospace', mt: 0.5}}>
                                            -{settings.daysPast}d to +{settings.daysFuture}d
                                        </Typography>
                                    </Box>
                                </Box>
                            )}
                        </CardContent>
                    </Card>

                    <Stack spacing={1.5}>
                        <Button
                            variant="contained"
                            size="large"
                            startIcon={syncRunning ? <CircularProgress size={18} color="inherit"/> : <PlayArrowIcon/>}
                            onClick={handleRunSync}
                            disabled={syncRunning || !profile?.outlookConnected || !profile?.googleConnected}
                            sx={{py: 2, justifyContent: 'flex-start'}}
                        >
                            <Box sx={{textAlign: 'left'}}>
                                <Typography variant="body2" sx={{color: 'inherit', fontWeight: 500}}>
                                    {syncRunning ? 'Sync Running…' : 'Run Sync Now'}
                                </Typography>
                                <Typography variant="caption" sx={{opacity: 0.7}}>Trigger manual job</Typography>
                            </Box>
                        </Button>
                        <Button variant="outlined" component={Link} to="/profile" endIcon={<ArrowForwardIcon/>}
                                sx={{justifyContent: 'space-between'}}>
                            Edit Profile
                        </Button>
                        <Button variant="outlined" component={Link} to="/settings" endIcon={<ArrowForwardIcon/>}
                                sx={{justifyContent: 'space-between'}}>
                            Edit Settings
                        </Button>
                    </Stack>
                </Box>
                {list ? <LastSyncCard maxRows={1} /> : null}

            </Stack>
        </Layout>
    );
}
