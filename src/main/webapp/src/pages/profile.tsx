import { useEffect, useState } from 'react';
import { Layout } from '@/components/layout';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import {
    fetchProfile,
    fetchOutlookCalendars,
    fetchGoogleCalendars,
    connectOutlook,
    setOutlookCalendar,
    setGoogleCalendar,
    disconnectGoogle,
    disconnectOutlook,
} from '@/store/profileSlice';
import { useSnackbar } from '@/components/snackbar-provider';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import TextField from '@mui/material/TextField';
import Divider from '@mui/material/Divider';
import MonitorIcon from '@mui/icons-material/Monitor';
import GoogleIcon from '@mui/icons-material/Google';
import FolderOpenIcon from '@mui/icons-material/FolderOpen';

export default function ProfilePage() {
    const dispatch = useAppDispatch();
    const profile = useAppSelector((s) => s.profile.profile);
    const outlookCalendars = useAppSelector((s) => s.profile.outlookCalendars);
    const googleCalendars = useAppSelector((s) => s.profile.googleCalendars);
    const [profilePath, setProfilePath] = useState('');
    const { showSnackbar } = useSnackbar();

    useEffect(() => {
        if (profile?.outlookConnected) dispatch(fetchOutlookCalendars());
        if (profile?.googleConnected) dispatch(fetchGoogleCalendars());
    }, [dispatch, profile?.outlookConnected, profile?.googleConnected]);

    const handleConnectOutlook = () => {
        if (!profilePath.trim()) {
            showSnackbar('Please enter a valid profile file path.', 'warning');
            return;
        }
        dispatch(connectOutlook({ profilePath: profilePath.trim() }))
            .unwrap()
            .then(() => {
                showSnackbar('Outlook connected successfully.');
                dispatch(fetchProfile());
            })
            .catch(() => showSnackbar('Failed to connect Outlook.', 'error'));
    };

    const handleOutlookCalendarChange = (calendarId: string) => {
        dispatch(setOutlookCalendar({ calendarId }))
            .unwrap()
            .then(() => {
                showSnackbar('Outlook Calendar Updated');
                dispatch(fetchProfile());
            })
            .catch(() => showSnackbar('Failed to update calendar', 'error'));
    };

    const handleGoogleCalendarChange = (calendarId: string) => {
        dispatch(setGoogleCalendar({ calendarId }))
            .unwrap()
            .then(() => {
                showSnackbar('Google Calendar Updated');
                dispatch(fetchProfile());
            })
            .catch(() => showSnackbar('Failed to update calendar', 'error'));
    };

    const handleGoogleLogin = () => {
        window.location.href = `${import.meta.env.BASE_URL}/api/auth/login`;
    };

    const handleDisconnectGoogle = () => {
        dispatch(disconnectGoogle())
            .unwrap()
            .then(() => {
                showSnackbar('Google account disconnected.');
                dispatch(fetchProfile());
            })
            .catch(() => showSnackbar('Failed to disconnect Google.', 'error'));
    };

    const handleDisconnectOutlook = () => {
        dispatch(disconnectOutlook())
            .unwrap()
            .then(() => {
                showSnackbar('Outlook profile cleared.');
                dispatch(fetchProfile());
            })
            .catch(() => showSnackbar('Failed to disconnect Outlook.', 'error'));
    };

    return (
        <Layout>
            <Stack spacing={4}>
                <Box>
                    <Typography variant='h4'>Profile & Connections</Typography>
                    <Typography variant='body2' sx={{ mt: 1 }}>
                        Manage your Microsoft and Google connections.
                    </Typography>
                </Box>

                <Stack spacing={3} sx={{ maxWidth: 600 }}>
                    {/* Outlook */}
                    <Card>
                        <CardHeader
                            avatar={<MonitorIcon sx={{ color: '#00a4ef' }} />}
                            title='Source: Microsoft Outlook'
                            subheader='Events will be read from this account.'
                            titleTypographyProps={{ fontWeight: 600 }}
                        />
                        <CardContent>
                            {profile?.outlookConnected ? (
                                <Stack spacing={3}>
                                    {profile.outlookProfilePath && (
                                        <Box>
                                            <Typography
                                                variant='caption'
                                                color='text.secondary'
                                                sx={{
                                                    textTransform: 'uppercase',
                                                    letterSpacing: 1,
                                                }}
                                            >
                                                Profile File
                                            </Typography>
                                            <Typography
                                                variant='body2'
                                                sx={{
                                                    fontFamily: 'monospace',
                                                    mt: 0.5,
                                                    wordBreak: 'break-word',
                                                }}
                                            >
                                                {profile.outlookProfilePath}
                                            </Typography>
                                        </Box>
                                    )}
                                    <FormControl fullWidth>
                                        <InputLabel>Select Source Calendar</InputLabel>
                                        <Select
                                            value={profile.outlookCalendarId || ''}
                                            label='Select Source Calendar'
                                            onChange={(e) =>
                                                handleOutlookCalendarChange(
                                                    e.target.value as string,
                                                )
                                            }
                                        >
                                            {outlookCalendars.map((cal) => (
                                                <MenuItem key={cal.id} value={cal.id}>
                                                    {cal.name}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </FormControl>
                                    <Divider />
                                    <Button
                                        variant='outlined'
                                        color='error'
                                        onClick={handleDisconnectOutlook}
                                    >
                                        Remove Profile File
                                    </Button>
                                </Stack>
                            ) : (
                                <Box
                                    sx={{
                                        py: 4,
                                        px: 3,
                                        textAlign: 'center',
                                        border: '2px dashed',
                                        borderColor: 'divider',
                                        borderRadius: 1,
                                        backgroundColor: 'action.hover',
                                    }}
                                >
                                    <FolderOpenIcon
                                        sx={{ fontSize: 40, color: 'text.disabled', mb: 1 }}
                                    />
                                    <Typography variant='body2' sx={{ mb: 3 }}>
                                        Enter the path to your Outlook profile file (.ost or .pst)
                                        to connect.
                                    </Typography>
                                    <Stack spacing={2} sx={{ maxWidth: 400, mx: 'auto' }}>
                                        <TextField
                                            fullWidth
                                            size='small'
                                            label='Outlook Profile File Path'
                                            placeholder='C:\Users\...\Outlook\profile.ost'
                                            value={profilePath}
                                            onChange={(e) => setProfilePath(e.target.value)}
                                            InputProps={{
                                                sx: {
                                                    fontFamily: 'monospace',
                                                    fontSize: '0.875rem',
                                                },
                                            }}
                                        />
                                        <Button
                                            variant='contained'
                                            startIcon={<MonitorIcon />}
                                            onClick={handleConnectOutlook}
                                            disabled={!profilePath.trim()}
                                        >
                                            Connect Outlook
                                        </Button>
                                    </Stack>
                                </Box>
                            )}
                        </CardContent>
                    </Card>

                    {/* Google */}
                    <Card>
                        <CardHeader
                            avatar={<GoogleIcon sx={{ color: '#4285F4' }} />}
                            title='Destination: Google Calendar'
                            subheader='Events will be synced to this account.'
                            titleTypographyProps={{ fontWeight: 600 }}
                        />
                        <CardContent>
                            {profile?.googleConnected ? (
                                <Stack spacing={3}>
                                    <FormControl fullWidth>
                                        <InputLabel>Select Destination Calendar</InputLabel>
                                        <Select
                                            value={profile.googleCalendarId || ''}
                                            label='Select Destination Calendar'
                                            onChange={(e) =>
                                                handleGoogleCalendarChange(e.target.value as string)
                                            }
                                        >
                                            {googleCalendars.map((cal) => (
                                                <MenuItem key={cal.id} value={cal.id}>
                                                    {cal.name}
                                                </MenuItem>
                                            ))}
                                        </Select>
                                    </FormControl>
                                    <Divider />
                                    <Stack direction='row' spacing={2}>
                                        <Button variant='outlined' onClick={handleGoogleLogin}>
                                            Reconnect Google Account
                                        </Button>
                                        <Button
                                            variant='outlined'
                                            color='error'
                                            onClick={handleDisconnectGoogle}
                                        >
                                            Disconnect
                                        </Button>
                                    </Stack>
                                </Stack>
                            ) : (
                                <Box
                                    sx={{
                                        py: 4,
                                        textAlign: 'center',
                                        border: '2px dashed',
                                        borderColor: 'divider',
                                        borderRadius: 1,
                                        backgroundColor: 'action.hover',
                                    }}
                                >
                                    <GoogleIcon sx={{ fontSize: 40, color: '#4285F4', mb: 1 }} />
                                    <Typography variant='body2' sx={{ mb: 2 }}>
                                        Connect your Google account to select a destination
                                        calendar.
                                    </Typography>
                                    <Button
                                        variant='contained'
                                        startIcon={<GoogleIcon />}
                                        onClick={handleGoogleLogin}
                                    >
                                        Sign in with Google
                                    </Button>
                                </Box>
                            )}
                        </CardContent>
                    </Card>
                </Stack>
            </Stack>
        </Layout>
    );
}
