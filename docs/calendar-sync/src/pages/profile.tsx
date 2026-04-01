import { Layout } from "@/components/layout";
import {
  useGetProfile,
  useGetOutlookCalendars,
  useGetGoogleCalendars,
  useSetOutlookCalendar,
  useSetGoogleCalendar,
  useConnectOutlook,
  getGetProfileQueryKey,
} from "@/lib/api-client-react/index";
import { useQueryClient } from "@tanstack/react-query";
import { useSnackbar } from "@/components/snackbar-provider";
import { useState } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import FormControl from "@mui/material/FormControl";
import InputLabel from "@mui/material/InputLabel";
import Select from "@mui/material/Select";
import MenuItem from "@mui/material/MenuItem";
import TextField from "@mui/material/TextField";
import Divider from "@mui/material/Divider";
import CircularProgress from "@mui/material/CircularProgress";
import MonitorIcon from "@mui/icons-material/Monitor";
import GoogleIcon from "@mui/icons-material/Google";
import FolderOpenIcon from "@mui/icons-material/FolderOpen";

export default function ProfilePage() {
  const { data: profile, isLoading: profileLoading } = useGetProfile();
  const [profilePath, setProfilePath] = useState("");

  const { data: outlookCalendars, isLoading: outlookLoading } = useGetOutlookCalendars({
    query: { enabled: !!profile?.outlookConnected },
  });

  const { data: googleCalendars, isLoading: googleLoading } = useGetGoogleCalendars({
    query: { enabled: !!profile?.googleConnected },
  });

  const setOutlookCalendar = useSetOutlookCalendar();
  const setGoogleCalendar = useSetGoogleCalendar();
  const connectOutlook = useConnectOutlook();
  const queryClient = useQueryClient();
  const { showSnackbar } = useSnackbar();

  const handleConnectOutlook = () => {
    if (!profilePath.trim()) {
      showSnackbar("Please enter a valid profile file path.", "warning");
      return;
    }
    connectOutlook.mutate(
      { data: { profilePath: profilePath.trim() } },
      {
        onSuccess: () => {
          showSnackbar("Outlook connected successfully.");
          queryClient.invalidateQueries({ queryKey: getGetProfileQueryKey() });
        },
        onError: () => showSnackbar("Failed to connect Outlook.", "error"),
      }
    );
  };

  const handleOutlookCalendarChange = (calendarId: string) => {
    setOutlookCalendar.mutate(
      { data: { calendarId } },
      {
        onSuccess: () => {
          showSnackbar("Outlook Calendar Updated");
          queryClient.invalidateQueries({ queryKey: getGetProfileQueryKey() });
        },
        onError: () => showSnackbar("Failed to update calendar", "error"),
      }
    );
  };

  const handleGoogleCalendarChange = (calendarId: string) => {
    setGoogleCalendar.mutate(
      { data: { calendarId } },
      {
        onSuccess: () => {
          showSnackbar("Google Calendar Updated");
          queryClient.invalidateQueries({ queryKey: getGetProfileQueryKey() });
        },
        onError: () => showSnackbar("Failed to update calendar", "error"),
      }
    );
  };

  const handleGoogleLogin = () => {
    window.location.href = "/api/auth/login";
  };

  if (profileLoading) {
    return (
      <Layout>
        <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", height: 256 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout>
      <Stack spacing={4}>
        <Box>
          <Typography variant="h4">Profile & Connections</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Manage your Microsoft and Google connections.
          </Typography>
        </Box>

        <Stack spacing={3} sx={{ maxWidth: 600 }}>
          <Card>
            <CardHeader
              avatar={<MonitorIcon sx={{ color: "#00a4ef" }} />}
              title="Source: Microsoft Outlook"
              subheader="Events will be read from this account."
              titleTypographyProps={{ fontWeight: 600 }}
            />
            <CardContent>
              {profile?.outlookConnected ? (
                <Stack spacing={3}>
                  {profile.outlookProfilePath && (
                    <Box>
                      <Typography variant="caption" color="text.secondary" sx={{ textTransform: "uppercase", letterSpacing: 1 }}>
                        Profile File
                      </Typography>
                      <Typography variant="body2" sx={{ fontFamily: "monospace", mt: 0.5 }}>
                        {profile.outlookProfilePath}
                      </Typography>
                    </Box>
                  )}
                  <FormControl fullWidth disabled={outlookLoading || setOutlookCalendar.isPending}>
                    <InputLabel>Select Source Calendar</InputLabel>
                    <Select
                      value={profile.outlookCalendarId || ""}
                      label="Select Source Calendar"
                      onChange={(e) => handleOutlookCalendarChange(e.target.value as string)}
                    >
                      {outlookCalendars?.map((cal) => (
                        <MenuItem key={cal.id} value={cal.id}>
                          {cal.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                </Stack>
              ) : (
                <Box
                  sx={{
                    py: 4,
                    px: 3,
                    textAlign: "center",
                    border: "2px dashed",
                    borderColor: "divider",
                    borderRadius: 1,
                    backgroundColor: "action.hover",
                  }}
                >
                  <FolderOpenIcon sx={{ fontSize: 40, color: "text.disabled", mb: 1 }} />
                  <Typography variant="body2" sx={{ mb: 3 }}>
                    Enter the path to your Outlook profile file (.ost or .pst) to connect.
                  </Typography>
                  <Stack spacing={2} sx={{ maxWidth: 400, mx: "auto" }}>
                    <TextField
                      fullWidth
                      size="small"
                      label="Outlook Profile File Path"
                      placeholder="C:\Users\...\Outlook\profile.ost"
                      value={profilePath}
                      onChange={(e) => setProfilePath(e.target.value)}
                      InputProps={{ sx: { fontFamily: "monospace", fontSize: "0.875rem" } }}
                    />
                    <Button
                      variant="contained"
                      startIcon={connectOutlook.isPending ? <CircularProgress size={16} color="inherit" /> : <MonitorIcon />}
                      onClick={handleConnectOutlook}
                      disabled={connectOutlook.isPending || !profilePath.trim()}
                    >
                      Connect Outlook
                    </Button>
                  </Stack>
                </Box>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader
              avatar={<GoogleIcon sx={{ color: "#4285F4" }} />}
              title="Destination: Google Calendar"
              subheader="Events will be synced to this account."
              titleTypographyProps={{ fontWeight: 600 }}
            />
            <CardContent>
              {profile?.googleConnected ? (
                <Stack spacing={3}>
                  <FormControl fullWidth disabled={googleLoading || setGoogleCalendar.isPending}>
                    <InputLabel>Select Destination Calendar</InputLabel>
                    <Select
                      value={profile.googleCalendarId || ""}
                      label="Select Destination Calendar"
                      onChange={(e) => handleGoogleCalendarChange(e.target.value as string)}
                    >
                      {googleCalendars?.map((cal) => (
                        <MenuItem key={cal.id} value={cal.id}>
                          {cal.name}
                        </MenuItem>
                      ))}
                    </Select>
                  </FormControl>
                  <Divider />
                  <Button variant="outlined" onClick={handleGoogleLogin}>
                    Reconnect Google Account
                  </Button>
                </Stack>
              ) : (
                <Box
                  sx={{
                    py: 4,
                    textAlign: "center",
                    border: "2px dashed",
                    borderColor: "divider",
                    borderRadius: 1,
                    backgroundColor: "action.hover",
                  }}
                >
                  <GoogleIcon sx={{ fontSize: 40, color: "#4285F4", mb: 1 }} />
                  <Typography variant="body2" sx={{ mb: 2 }}>
                    Connect your Google account to select a destination calendar.
                  </Typography>
                  <Button variant="contained" startIcon={<GoogleIcon />} onClick={handleGoogleLogin}>
                    Sign in with Google
                  </Button>
                </Box>
              )}
            </CardContent>
          </Card>
        </Stack>
      </Stack>
    </Layout>
  );
}
