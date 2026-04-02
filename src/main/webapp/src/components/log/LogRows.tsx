import { useNavigate } from 'react-router-dom';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import Chip from '@mui/material/Chip';
import { type SyncRun, SyncRunStatus } from '@api';
import { formatLocal } from '@/utils/dateUtils';
import { getStatusChipSx } from '@/utils/colorUtils';

interface Props {
  logs: SyncRun[];
}

const LogRows = ({ logs }: Props) => {
  const navigate = useNavigate();
  return (
    <>
      {logs.map(log => (
        <TableRow key={log.id} hover sx={{ cursor: 'pointer' }}
          onClick={() => navigate(`/logs/${log.id}`)}
          data-testid={`row-log-${log.id}`}>
          <TableCell sx={{ fontFamily: 'monospace', fontSize: '0.875rem' }}>
            {formatLocal(log.startedAt, 'MMM d, yyyy HH:mm:ss')}
          </TableCell>
          <TableCell>
            <Chip
              label={log.status}
              color={log.status === SyncRunStatus.Success ? 'success' : log.status === SyncRunStatus.Running ? 'warning' : 'error'}
              size="small"
              sx={getStatusChipSx(log.status)}
            />
          </TableCell>
          <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{log.created ?? '-'}</TableCell>
          <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{log.updated ?? '-'}</TableCell>
          <TableCell align="right" sx={{ fontFamily: 'monospace' }}>{log.deleted ?? '-'}</TableCell>
          <TableCell sx={{ maxWidth: 300, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {log.message || '-'}
          </TableCell>
        </TableRow>
      ))}
    </>
  );
};

export default LogRows;
