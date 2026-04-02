import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Layout } from '@/components/layout';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchLogs } from '@/store/logsSlice';
import { SyncRunStatus } from '@api';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import FormControl from '@mui/material/FormControl';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import RefreshIcon from '@mui/icons-material/Refresh';
import Tooltip from '@mui/material/Tooltip';
import LogsTable from '@/components/log/LogsTable';
import LogsDetails from '@/components/log/LogsDetails';

export default function LogsPage() {
  const { logId } = useParams<{ logId?: string }>();
  const dispatch = useAppDispatch();
  const list = useAppSelector(s => s.logs.list);

  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<SyncRunStatus | 'ALL'>('ALL');

  const fetchCurrentLogs = useCallback(() => {
    dispatch(fetchLogs({
      page,
      size,
      ...(statusFilter !== 'ALL' ? { status: statusFilter } : {}),
    }));
  }, [dispatch, page, size, statusFilter]);

  useEffect(() => {
    if (!logId) fetchCurrentLogs();
  }, [logId, page, size, statusFilter, fetchCurrentLogs]);

  // ── Detail view ──
  if (logId) {
    return <LogsDetails logId={logId} />;
  }

  const totalElements = list?.totalElements ?? 0;

  // ── List view ──
  return (
    <Layout>
      <Stack spacing={4}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
          <Box>
            <Typography variant="h4" data-testid="text-page-title">
              Sync Logs
              <Tooltip title="Refresh current log list" placement="right" arrow>
                <Button onClick={fetchCurrentLogs}><RefreshIcon /></Button>
              </Tooltip>
            </Typography>
            <Typography variant="body2" sx={{ mt: 1 }}>
              History of synchronization runs.{totalElements > 0 && ` (${totalElements} total)`}
            </Typography>
          </Box>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>Status</InputLabel>
            <Select value={statusFilter} label="Status"
              onChange={e => { setStatusFilter(e.target.value as SyncRunStatus | 'ALL'); setPage(0); }}
              data-testid="select-status-filter">
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value={SyncRunStatus.Running}>Running</MenuItem>
              <MenuItem value={SyncRunStatus.Success}>Success</MenuItem>
              <MenuItem value={SyncRunStatus.Failed}>Failed</MenuItem>
            </Select>
          </FormControl>
        </Box>
        <LogsTable page={page} setPage={setPage} />
      </Stack>
    </Layout>
  );
}
