import { Layout } from "@/components/layout";
import { useGetLogs, useGetLogDetails } from "@/services/logs.service";
import { useState } from "react";
import { format } from "date-fns";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Chip from "@mui/material/Chip";
import Stack from "@mui/material/Stack";
import Table from "@mui/material/Table";
import TableBody from "@mui/material/TableBody";
import TableCell from "@mui/material/TableCell";
import TableContainer from "@mui/material/TableContainer";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import ArrowBackIcon from "@mui/icons-material/ArrowBack";
import ChevronLeftIcon from "@mui/icons-material/ChevronLeft";
import ChevronRightIcon from "@mui/icons-material/ChevronRight";

export default function LogsPage() {
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedRunId, setSelectedRunId] = useState<string | null>(null);

  const { data: pagedLogs, isLoading } = useGetLogs({
    page,
    size,
    ...(statusFilter !== "ALL" ? { status: statusFilter as "SUCCESS" | "FAILED" } : {}),
  });

  const { data: logDetails, isLoading: detailsLoading } = useGetLogDetails(selectedRunId ?? "", {
    enabled: !!selectedRunId,
  });

  const handleStatusChange = (value: string) => {
    setStatusFilter(value);
    setPage(0);
  };

  const logs = pagedLogs?.content ?? [];
  const totalPages = pagedLogs?.totalPages ?? 0;
  const totalElements = pagedLogs?.totalElements ?? 0;

  const getLevelColor = (level: string): "error" | "warning" | "info" | "default" => {
    switch (level) {
      case "ERROR": return "error";
      case "WARN": return "warning";
      case "DEBUG": return "default";
      default: return "info";
    }
  };

  const getLevelBg = (level: string) => {
    switch (level) {
      case "ERROR": return { bgcolor: "#fef2f2", borderColor: "#fecaca", color: "#dc2626" };
      case "WARN": return { bgcolor: "#fffbeb", borderColor: "#fde68a", color: "#d97706" };
      case "DEBUG": return { bgcolor: "#f9fafb", borderColor: "#e5e7eb", color: "#6b7280" };
      default: return { bgcolor: "#eff6ff", borderColor: "#bfdbfe", color: "#2563eb" };
    }
  };

  if (selectedRunId) {
    return (
      <Layout>
        <Stack spacing={3} data-testid="log-details-view">
          <Box>
            <Button
              startIcon={<ArrowBackIcon />}
              onClick={() => setSelectedRunId(null)}
              sx={{ mb: 2 }}
              data-testid="button-back-to-logs"
            >
              Back to Logs
            </Button>
            <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
              <Typography variant="h4">Log Details</Typography>
              {logDetails?.run && (
                <Chip
                  label={logDetails.run.status}
                  color={logDetails.run.status === "SUCCESS" ? "success" : "error"}
                  size="small"
                />
              )}
            </Box>
            {logDetails?.run && (
              <Typography variant="body2" sx={{ mt: 1 }}>
                Run started {format(new Date(logDetails.run.startedAt), "MMMM d, yyyy 'at' HH:mm:ss")}
              </Typography>
            )}
          </Box>

          {detailsLoading ? (
            <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
              <CircularProgress />
            </Box>
          ) : logDetails ? (
            <>
              <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr 1fr", md: "repeat(5, 1fr)" }, gap: 2 }}>
                <Card>
                  <CardContent sx={{ textAlign: "center", py: 3 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: 1 }}>
                      Started
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: "monospace", mt: 1 }}>
                      {format(new Date(logDetails.run.startedAt), "HH:mm:ss")}
                    </Typography>
                  </CardContent>
                </Card>
                <Card>
                  <CardContent sx={{ textAlign: "center", py: 3 }}>
                    <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: 1 }}>
                      Finished
                    </Typography>
                    <Typography variant="body2" sx={{ fontFamily: "monospace", mt: 1 }}>
                      {logDetails.run.finishedAt
                        ? format(new Date(logDetails.run.finishedAt), "HH:mm:ss")
                        : "-"}
                    </Typography>
                  </CardContent>
                </Card>
                <Card sx={{ borderColor: "#bbf7d0", bgcolor: "#f0fdf4" }}>
                  <CardContent sx={{ textAlign: "center", py: 3 }}>
                    <Typography variant="h4" sx={{ color: "#15803d", fontWeight: 700 }}>
                      {logDetails.run.created ?? 0}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "#16a34a", textTransform: "uppercase", letterSpacing: 1 }}>
                      Created
                    </Typography>
                  </CardContent>
                </Card>
                <Card sx={{ borderColor: "#bfdbfe", bgcolor: "#eff6ff" }}>
                  <CardContent sx={{ textAlign: "center", py: 3 }}>
                    <Typography variant="h4" sx={{ color: "#1d4ed8", fontWeight: 700 }}>
                      {logDetails.run.updated ?? 0}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "#2563eb", textTransform: "uppercase", letterSpacing: 1 }}>
                      Updated
                    </Typography>
                  </CardContent>
                </Card>
                <Card sx={{ borderColor: "#fde68a", bgcolor: "#fffbeb" }}>
                  <CardContent sx={{ textAlign: "center", py: 3 }}>
                    <Typography variant="h4" sx={{ color: "#b45309", fontWeight: 700 }}>
                      {logDetails.run.deleted ?? 0}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "#d97706", textTransform: "uppercase", letterSpacing: 1 }}>
                      Deleted
                    </Typography>
                  </CardContent>
                </Card>
              </Box>

              {logDetails.run.message && (
                <Card>
                  <CardContent>
                    <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: 1 }}>
                      Summary
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 0.5 }}>
                      {logDetails.run.message}
                    </Typography>
                  </CardContent>
                </Card>
              )}

              <Card>
                <CardHeader
                  title={`Log Entries (${logDetails.entries.length})`}
                  titleTypographyProps={{
                    variant: "overline",
                    color: "text.secondary",
                    letterSpacing: 1,
                  }}
                />
                <CardContent sx={{ pt: 0 }}>
                  <Stack spacing={1}>
                    {logDetails.entries.map((entry, i) => {
                      const style = getLevelBg(entry.level);
                      return (
                        <Box
                          key={i}
                          data-testid={`log-entry-${i}`}
                          sx={{
                            display: "flex",
                            gap: 1.5,
                            alignItems: "flex-start",
                            px: 2,
                            py: 1.5,
                            borderRadius: 1,
                            border: 1,
                            ...style,
                          }}
                        >
                          <Typography
                            variant="caption"
                            sx={{ fontFamily: "monospace", whiteSpace: "nowrap", mt: 0.25 }}
                          >
                            {format(new Date(entry.timestamp), "HH:mm:ss.SSS")}
                          </Typography>
                          <Chip
                            label={entry.level}
                            size="small"
                            color={getLevelColor(entry.level)}
                            variant="outlined"
                            sx={{ fontFamily: "monospace", fontSize: "0.7rem", height: 22 }}
                          />
                          <Typography variant="body2" sx={{ flex: 1, color: style.color }}>
                            {entry.message}
                          </Typography>
                        </Box>
                      );
                    })}
                    {logDetails.entries.length === 0 && (
                      <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 4 }}>
                        No log entries recorded.
                      </Typography>
                    )}
                  </Stack>
                </CardContent>
              </Card>
            </>
          ) : null}
        </Stack>
      </Layout>
    );
  }

  return (
    <Layout>
      <Stack spacing={4}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end" }}>
          <Box>
            <Typography variant="h4" data-testid="text-page-title">Sync Logs</Typography>
            <Typography variant="body2" sx={{ mt: 1 }}>
              History of synchronization runs.
              {totalElements > 0 && ` (${totalElements} total)`}
            </Typography>
          </Box>
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel>Status</InputLabel>
            <Select
              value={statusFilter}
              label="Status"
              onChange={(e) => handleStatusChange(e.target.value)}
              data-testid="select-status-filter"
            >
              <MenuItem value="ALL">All Statuses</MenuItem>
              <MenuItem value="SUCCESS">Success</MenuItem>
              <MenuItem value="FAILED">Failed</MenuItem>
            </Select>
          </FormControl>
        </Box>

        <Card>
          <CardContent>
            {isLoading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 6 }}>
                <CircularProgress />
              </Box>
            ) : logs.length > 0 ? (
              <>
                <TableContainer>
                  <Table>
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
                    <TableBody>
                      {logs.map((log) => (
                        <TableRow
                          key={log.id}
                          hover
                          sx={{ cursor: "pointer" }}
                          onClick={() => setSelectedRunId(log.id)}
                          data-testid={`row-log-${log.id}`}
                        >
                          <TableCell sx={{ fontFamily: "monospace", fontSize: "0.875rem" }}>
                            {format(new Date(log.startedAt), "MMM d, yyyy HH:mm:ss")}
                          </TableCell>
                          <TableCell>
                            <Chip
                              label={log.status}
                              color={log.status === "SUCCESS" ? "success" : "error"}
                              size="small"
                            />
                          </TableCell>
                          <TableCell align="right" sx={{ fontFamily: "monospace" }}>{log.created ?? "-"}</TableCell>
                          <TableCell align="right" sx={{ fontFamily: "monospace" }}>{log.updated ?? "-"}</TableCell>
                          <TableCell align="right" sx={{ fontFamily: "monospace" }}>{log.deleted ?? "-"}</TableCell>
                          <TableCell
                            sx={{
                              maxWidth: 300,
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                              whiteSpace: "nowrap",
                            }}
                          >
                            {log.message || "-"}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>

                {totalPages > 1 && (
                  <>
                    <Divider />
                    <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", pt: 2 }}>
                      <Typography variant="body2" color="text.secondary">
                        Page {page + 1} of {totalPages}
                      </Typography>
                      <Stack direction="row" spacing={1}>
                        <Button
                          variant="outlined"
                          size="small"
                          disabled={page === 0}
                          onClick={() => setPage((p) => Math.max(0, p - 1))}
                          startIcon={<ChevronLeftIcon />}
                          data-testid="button-prev-page"
                        >
                          Previous
                        </Button>
                        <Button
                          variant="outlined"
                          size="small"
                          disabled={page >= totalPages - 1}
                          onClick={() => setPage((p) => p + 1)}
                          endIcon={<ChevronRightIcon />}
                          data-testid="button-next-page"
                        >
                          Next
                        </Button>
                      </Stack>
                    </Box>
                  </>
                )}
              </>
            ) : (
              <Box
                sx={{
                  py: 6,
                  textAlign: "center",
                  border: "2px dashed",
                  borderColor: "divider",
                  borderRadius: 2,
                }}
              >
                <Typography variant="body2" color="text.secondary">
                  No sync logs found.
                </Typography>
              </Box>
            )}
          </CardContent>
        </Card>
      </Stack>
    </Layout>
  );
}
