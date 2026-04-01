import { Layout } from "@/components/layout";
import { useGetProfile, useGetSettings, useTriggerSync, getGetLogsQueryKey } from "@/lib/api-client-react/index";
import { useQueryClient } from "@tanstack/react-query";
import { useSnackbar } from "@/components/snackbar-provider";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Skeleton from "@mui/material/Skeleton";
import Stack from "@mui/material/Stack";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";
import PlayArrowIcon from "@mui/icons-material/PlayArrow";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import CircularProgress from "@mui/material/CircularProgress";
import { Link } from "wouter";

export default function Home() {
  const { data: profile, isLoading: profileLoading } = useGetProfile();
  const { data: settings, isLoading: settingsLoading } = useGetSettings();
  const triggerSync = useTriggerSync();
  const queryClient = useQueryClient();
  const { showSnackbar } = useSnackbar();

  const handleRunSync = () => {
    triggerSync.mutate(undefined, {
      onSuccess: () => {
        showSnackbar("A manual sync run has been started.", "success");
        queryClient.invalidateQueries({ queryKey: getGetLogsQueryKey() });
      },
      onError: () => {
        showSnackbar("There was an error triggering the sync.", "error");
      },
    });
  };

  const StatusIcon = ({ connected }: { connected?: boolean }) =>
    connected ? (
      <CheckCircleIcon sx={{ color: "success.main" }} />
    ) : (
      <CancelIcon sx={{ color: "text.disabled" }} />
    );

  return (
    <Layout>
      <Stack spacing={4}>
        <Box>
          <Typography variant="h4">Cockpit</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Overview of your calendar synchronization status.
          </Typography>
        </Box>

        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "1fr 1fr" }, gap: 3 }}>
          <Card>
            <CardHeader
              title="Microsoft Outlook"
              titleTypographyProps={{ variant: "subtitle1", fontWeight: 600 }}
              action={profileLoading ? <CircularProgress size={20} /> : <StatusIcon connected={profile?.outlookConnected} />}
            />
            <CardContent sx={{ pt: 0 }}>
              {profileLoading ? (
                <Skeleton variant="rounded" height={40} />
              ) : profile?.outlookConnected ? (
                <Box>
                  <Typography variant="body2" sx={{ color: "success.main", fontWeight: 500 }}>Connected</Typography>
                  <Typography variant="body2" sx={{ fontFamily: "monospace", mt: 0.5 }}>
                    {profile.outlookCalendarName || "No calendar selected"}
                  </Typography>
                </Box>
              ) : (
                <Box>
                  <Typography variant="body2">Not Connected</Typography>
                  <Typography variant="body2">Authenticate in Profile.</Typography>
                </Box>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader
              title="Google Calendar"
              titleTypographyProps={{ variant: "subtitle1", fontWeight: 600 }}
              action={profileLoading ? <CircularProgress size={20} /> : <StatusIcon connected={profile?.googleConnected} />}
            />
            <CardContent sx={{ pt: 0 }}>
              {profileLoading ? (
                <Skeleton variant="rounded" height={40} />
              ) : profile?.googleConnected ? (
                <Box>
                  <Typography variant="body2" sx={{ color: "success.main", fontWeight: 500 }}>Connected</Typography>
                  <Typography variant="body2" sx={{ fontFamily: "monospace", mt: 0.5 }}>
                    {profile.googleCalendarName || "No calendar selected"}
                  </Typography>
                </Box>
              ) : (
                <Box>
                  <Typography variant="body2">Not Connected</Typography>
                  <Typography variant="body2">Authenticate in Profile.</Typography>
                </Box>
              )}
            </CardContent>
          </Card>
        </Box>

        <Box sx={{ display: "grid", gridTemplateColumns: { xs: "1fr", md: "2fr 1fr" }, gap: 3 }}>
          <Card>
            <CardHeader
              title="Sync Settings Summary"
              subheader="Current configuration for background jobs"
              titleTypographyProps={{ variant: "subtitle1", fontWeight: 600 }}
            />
            <CardContent sx={{ pt: 0 }}>
              {settingsLoading ? (
                <Stack spacing={1}>
                  <Skeleton variant="rounded" height={20} width="33%" />
                  <Skeleton variant="rounded" height={20} width="50%" />
                </Stack>
              ) : settings ? (
                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
                  <Box>
                    <Typography variant="body2">Frequency</Typography>
                    <Typography variant="h5" sx={{ fontFamily: "monospace", mt: 0.5 }}>
                      Every {settings.frequencyMinutes}m
                    </Typography>
                  </Box>
                  <Box>
                    <Typography variant="body2">Window</Typography>
                    <Typography variant="body1" sx={{ fontFamily: "monospace", mt: 0.5 }}>
                      -{settings.daysPast}d to +{settings.daysFuture}d
                    </Typography>
                  </Box>
                </Box>
              ) : (
                <Typography variant="body2">Settings unavailable</Typography>
              )}
            </CardContent>
          </Card>

          <Stack spacing={1.5}>
            <Button
              variant="contained"
              size="large"
              startIcon={triggerSync.isPending ? <CircularProgress size={20} color="inherit" /> : <PlayArrowIcon />}
              onClick={handleRunSync}
              disabled={triggerSync.isPending || profileLoading || !profile?.outlookConnected || !profile?.googleConnected}
              sx={{ py: 2, justifyContent: "flex-start" }}
            >
              <Box sx={{ textAlign: "left" }}>
                <Typography variant="body2" sx={{ color: "inherit", fontWeight: 500 }}>Run Sync Now</Typography>
                <Typography variant="caption" sx={{ opacity: 0.7 }}>Trigger manual job</Typography>
              </Box>
            </Button>
            <Button
              variant="outlined"
              component={Link}
              href="/profile"
              endIcon={<ArrowForwardIcon />}
              sx={{ justifyContent: "space-between" }}
            >
              Edit Profile
            </Button>
            <Button
              variant="outlined"
              component={Link}
              href="/settings"
              endIcon={<ArrowForwardIcon />}
              sx={{ justifyContent: "space-between" }}
            >
              Edit Settings
            </Button>
          </Stack>
        </Box>
      </Stack>
    </Layout>
  );
}
