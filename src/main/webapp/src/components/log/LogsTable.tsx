import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import TableContainer from '@mui/material/TableContainer';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { useAppSelector } from '@/store/hooks';
import LogTableHead from '@/components/log/LogTableHead';
import LogRows from '@/components/log/LogRows';

interface Props {
  page: number;
  setPage: (fn: (p: number) => number) => void;
}

const LogsTable = ({ page, setPage }: Props) => {
  const list = useAppSelector(s => s.logs.list);
  const logs = list?.content ?? [];
  const totalPages = list?.totalPages ?? 0;

  return (
    <Card>
      <CardContent>
        {logs.length > 0 ? (
          <>
            <TableContainer>
              <Table>
                <LogTableHead />
                <TableBody><LogRows logs={logs} /></TableBody>
              </Table>
            </TableContainer>
            {totalPages > 1 && (
              <>
                <Divider />
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pt: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Page {page + 1} of {totalPages}
                  </Typography>
                  <Stack direction="row" spacing={1}>
                    <Button variant="outlined" size="small" disabled={page === 0}
                      onClick={() => setPage(p => Math.max(0, p - 1))}
                      startIcon={<ChevronLeftIcon />} data-testid="button-prev-page">Previous</Button>
                    <Button variant="outlined" size="small" disabled={page >= totalPages - 1}
                      onClick={() => setPage(p => p + 1)}
                      endIcon={<ChevronRightIcon />} data-testid="button-next-page">Next</Button>
                  </Stack>
                </Box>
              </>
            )}
          </>
        ) : (
          <Box sx={{ py: 6, textAlign: 'center', border: '2px dashed', borderColor: 'divider', borderRadius: 2 }}>
            <Typography variant="body2" color="text.secondary">No sync logs found.</Typography>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default LogsTable;
