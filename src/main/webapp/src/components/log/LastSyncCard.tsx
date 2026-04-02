import { useNavigate } from 'react-router-dom';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardHeader from '@mui/material/CardHeader';
import TableContainer from '@mui/material/TableContainer';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import { useAppSelector } from '@/store/hooks';
import LogTableHead from '@/components/log/LogTableHead';
import LogRows from '@/components/log/LogRows';

interface Props {
  maxRows?: number;
}

const LastSyncCard = ({ maxRows = 1 }: Props) => {
  const navigate = useNavigate();
  const list = useAppSelector(s => s.logs.list);
  const logs = (list?.content ?? []).slice(0, maxRows);

  return (
    <Card>
      <CardHeader
        title="Last Sync"
        titleTypographyProps={{ fontWeight: 600 }}
        action={<Button size="small" onClick={() => navigate('/logs')}>View All</Button>}
      />
      <CardContent sx={{ pt: 0 }}>
        {logs.length > 0 ? (
          <TableContainer>
            <Table size="small">
              <LogTableHead />
              <TableBody><LogRows logs={logs} /></TableBody>
            </Table>
          </TableContainer>
        ) : (
          <Box sx={{ py: 3, textAlign: 'center', border: '2px dashed', borderColor: 'divider', borderRadius: 2 }}>
            <Typography variant="body2" color="text.secondary">No sync runs yet.</Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default LastSyncCard;
