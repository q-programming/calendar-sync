import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';

const LogTableHead = () => (
  <TableHead>
    <TableRow>
      <TableCell>Started At</TableCell>
      <TableCell>Status</TableCell>
      <TableCell align="right">Created</TableCell>
      <TableCell align="right">Updated</TableCell>
      <TableCell align="right">Deleted</TableCell>
      <TableCell>Message</TableCell>
    </TableRow>
  </TableHead>
);

export default LogTableHead;
