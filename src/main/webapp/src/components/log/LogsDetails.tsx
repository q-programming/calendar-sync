import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout } from '@/components/layout';
import { SyncRunStatus } from '@api';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { formatLocal } from '@/utils/dateUtils';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { getLevelBg, getLevelColor, getStatusChipSx } from '@/utils/colorUtils';
import { fetchLogDetails, clearLogDetails } from '@/store/logsSlice';

interface Props {
  logId: string;
}

const LogsDetails = ({ logId }: Props) => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const details = useAppSelector(s => s.logs.details);

  useEffect(() => {
    dispatch(fetchLogDetails(logId));
    return () => { dispatch(clearLogDetails()); };
  }, [logId, dispatch]);

  if (!details) return null;

  return (
    <Layout>
      <Stack spacing={3} data-testid="log-details-view">
        <Box>
          <Button startIcon={<ArrowBackIcon />} onClick={() => navigate('/logs')} sx={{ mb: 2 }}
            data-testid="button-back-to-logs">Back to Logs</Button>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Typography variant="h4">Log Details</Typography>
            <Chip
              label={details.run.status}
              color={details.run.status === SyncRunStatus.Success ? 'success' : details.run.status === SyncRunStatus.Running ? 'warning' : 'error'}
              size="small"
              sx={getStatusChipSx(details.run.status)}
            />
          </Box>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Run started {formatLocal(details.run.startedAt, "MMMM d, yyyy 'at' HH:mm:ss")}
          </Typography>
        </Box>

        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(5, 1fr)' }, gap: 2 }}>
          {[
            { label: 'Started', value: formatLocal(details.run.startedAt, 'HH:mm:ss') },
            { label: 'Finished', value: details.run.finishedAt ? formatLocal(details.run.finishedAt, 'HH:mm:ss') : '-' },
          ].map(({ label, value }) => (
            <Card key={label}>
              <CardContent sx={{ textAlign: 'center', py: 3 }}>
                <Typography variant="caption" color="text.secondary"
                  sx={{ textTransform: 'uppercase', letterSpacing: 1 }}>{label}</Typography>
                <Typography variant="body2" sx={{ fontFamily: 'monospace', mt: 1 }}>{value}</Typography>
              </CardContent>
            </Card>
          ))}
          <Card sx={{ borderColor: '#bbf7d0', bgcolor: '#f0fdf4' }}>
            <CardContent sx={{ textAlign: 'center', py: 3 }}>
              <Typography variant="h4" sx={{ color: '#15803d', fontWeight: 700 }}>{details.run.created ?? 0}</Typography>
              <Typography variant="caption" sx={{ color: '#16a34a', textTransform: 'uppercase', letterSpacing: 1 }}>Created</Typography>
            </CardContent>
          </Card>
          <Card sx={{ borderColor: '#bfdbfe', bgcolor: '#eff6ff' }}>
            <CardContent sx={{ textAlign: 'center', py: 3 }}>
              <Typography variant="h4" sx={{ color: '#1d4ed8', fontWeight: 700 }}>{details.run.updated ?? 0}</Typography>
              <Typography variant="caption" sx={{ color: '#2563eb', textTransform: 'uppercase', letterSpacing: 1 }}>Updated</Typography>
            </CardContent>
          </Card>
          <Card sx={{ borderColor: '#fde68a', bgcolor: '#fffbeb' }}>
            <CardContent sx={{ textAlign: 'center', py: 3 }}>
              <Typography variant="h4" sx={{ color: '#b45309', fontWeight: 700 }}>{details.run.deleted ?? 0}</Typography>
              <Typography variant="caption" sx={{ color: '#d97706', textTransform: 'uppercase', letterSpacing: 1 }}>Deleted</Typography>
            </CardContent>
          </Card>
        </Box>

        {details.run.message && (
          <Card>
            <CardContent>
              <Typography variant="caption" color="text.secondary"
                sx={{ textTransform: 'uppercase', letterSpacing: 1 }}>Summary</Typography>
              <Typography variant="body2" sx={{ mt: 0.5 }}>{details.run.message}</Typography>
            </CardContent>
          </Card>
        )}

        <Card>
          <CardHeader title={`Log Entries (${details.entries.length})`}
            titleTypographyProps={{ variant: 'overline', color: 'text.secondary', letterSpacing: 1 }} />
          <CardContent sx={{ pt: 0 }}>
            <Stack spacing={1}>
              {details.entries.map((entry, i) => {
                const style = getLevelBg(entry.level);
                return (
                  <Box key={i} data-testid={`log-entry-${i}`} sx={{
                    display: 'flex', gap: 1.5, alignItems: 'flex-start',
                    px: 2, py: 1.5, borderRadius: 1, border: 1, ...style
                  }}>
                    <Typography variant="caption" sx={{ fontFamily: 'monospace', whiteSpace: 'nowrap', mt: 0.25 }}>
                      {formatLocal(entry.timestamp, 'HH:mm:ss.SSS')}
                    </Typography>
                    <Chip label={entry.level} size="small" color={getLevelColor(entry.level)}
                      variant="outlined" sx={{ fontFamily: 'monospace', fontSize: '0.7rem', height: 22 }} />
                    <Typography variant="body2" sx={{ flex: 1, color: style.color }}>{entry.message}</Typography>
                  </Box>
                );
              })}
              {details.entries.length === 0 && (
                <Typography variant="body2" color="text.secondary" sx={{ textAlign: 'center', py: 4 }}>
                  No log entries recorded.
                </Typography>
              )}
            </Stack>
          </CardContent>
        </Card>
      </Stack>
    </Layout>
  );
};

export default LogsDetails;
