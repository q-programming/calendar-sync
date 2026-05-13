import {useEffect} from 'react';
import {BrowserRouter, Route, Routes} from 'react-router-dom';
import {ThemeProvider} from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import theme from '@/theme';
import {SnackbarProvider} from '@/components/snackbar-provider';
import {GlobalLoader} from '@/components/global-loader';
import {useAppDispatch} from '@/store/hooks';
import {fetchProfile} from '@/store/profileSlice';
import {fetchSettings} from '@/store/settingsSlice';
import NotFound from '@/pages/not-found';
import Home from '@/pages/home';
import ProfilePage from '@/pages/profile';
import SettingsPage from '@/pages/settings';
import LogsPage from '@/pages/logs';
import HelpPage from '@/pages/help';

function AppRoutes() {
    const dispatch = useAppDispatch();

    useEffect(() => {
        dispatch(fetchProfile());
        dispatch(fetchSettings());
    }, [dispatch]);

    return (
        <BrowserRouter basename='/calendarsync'>
            <GlobalLoader />
            <Routes>
                <Route path='/' element={<Home />} />
                <Route path='/profile' element={<ProfilePage />} />
                <Route path='/settings' element={<SettingsPage />} />
                <Route path='/logs' element={<LogsPage />} />
                <Route path='/logs/:logId' element={<LogsPage />} />
                <Route path="/help" element={<HelpPage/>}/>
                <Route path='*' element={<NotFound />} />
            </Routes>
        </BrowserRouter>
    );
}

function App() {
    return (
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <SnackbarProvider>
                <AppRoutes />
            </SnackbarProvider>
        </ThemeProvider>
    );
}

export default App;
