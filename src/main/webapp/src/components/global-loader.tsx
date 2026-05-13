import { useState, useEffect } from 'react';
import Backdrop from '@mui/material/Backdrop';
import CircularProgress from '@mui/material/CircularProgress';
import { useAppSelector } from '@/store/hooks';
import { selectIsLoading } from '@/store/loadingSlice';

/**
 * Full-screen overlay shown whenever any API request is in flight.
 * A 150ms delay prevents flicker on fast responses.
 */
export function GlobalLoader() {
    const isLoading = useAppSelector(selectIsLoading);
    const [show, setShow] = useState(false);

    useEffect(() => {
        if (isLoading) {
            const t = setTimeout(() => setShow(true), 150);
            return () => clearTimeout(t);
        } else {
            setShow(false);
        }
    }, [isLoading]);

    return (
        <Backdrop open={show} sx={{ zIndex: 9999, color: '#fff', backdropFilter: 'blur(2px)' }}>
            <CircularProgress color='inherit' />
        </Backdrop>
    );
}
