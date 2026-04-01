import { Layout } from "@/components/layout";
import { useGetSettings, useUpdateSettings, settingsKeys } from "@/services/settings.service";
import { useQueryClient } from "@tanstack/react-query";
import { useSnackbar } from "@/components/snackbar-provider";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import { useEffect } from "react";
import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import CardHeader from "@mui/material/CardHeader";
import CardActions from "@mui/material/CardActions";
import Typography from "@mui/material/Typography";
import TextField from "@mui/material/TextField";
import Switch from "@mui/material/Switch";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import Divider from "@mui/material/Divider";
import CircularProgress from "@mui/material/CircularProgress";

const settingsSchema = z.object({
  frequencyMinutes: z.coerce.number().min(1, "Must be at least 1 minute"),
  daysPast: z.coerce.number().min(0, "Cannot be negative"),
  daysFuture: z.coerce.number().min(0, "Cannot be negative"),
  debugLogging: z.boolean(),
});

type SettingsFormValues = z.infer<typeof settingsSchema>;

export default function SettingsPage() {
  const { data: settings, isLoading } = useGetSettings();
  const updateSettings = useUpdateSettings();
  const { showSnackbar } = useSnackbar();
  const queryClient = useQueryClient();

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<SettingsFormValues>({
    resolver: zodResolver(settingsSchema),
    defaultValues: {
      frequencyMinutes: 15,
      daysPast: 7,
      daysFuture: 30,
      debugLogging: false,
    },
  });

  useEffect(() => {
    if (settings) {
      reset({
        frequencyMinutes: settings.frequencyMinutes,
        daysPast: settings.daysPast,
        daysFuture: settings.daysFuture,
        debugLogging: settings.debugLogging,
      });
    }
  }, [settings, reset]);

  const onSubmit = (data: SettingsFormValues) => {
    updateSettings.mutate(
      data,
      {
        onSuccess: () => {
          showSnackbar("Settings saved successfully");
          queryClient.invalidateQueries({ queryKey: settingsKeys.settings() });
        },
        onError: () => showSnackbar("Failed to save settings", "error"),
      }
    );
  };

  if (isLoading) {
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
          <Typography variant="h4">System Settings</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>
            Configure background job intervals and sync windows.
          </Typography>
        </Box>

        <Card sx={{ maxWidth: 600 }}>
          <CardHeader
            title="Sync Configuration"
            subheader="Changes apply to the next scheduled run."
            titleTypographyProps={{ fontWeight: 600 }}
          />
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent>
              <Stack spacing={3}>
                <Controller
                  name="frequencyMinutes"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Sync Frequency (minutes)"
                      type="number"
                      fullWidth
                      inputProps={{ min: 1 }}
                      error={!!errors.frequencyMinutes}
                      helperText={errors.frequencyMinutes?.message || "How often the background job runs."}
                      InputProps={{ sx: { fontFamily: "monospace" } }}
                    />
                  )}
                />

                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
                  <Controller
                    name="daysPast"
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Days Past"
                        type="number"
                        fullWidth
                        inputProps={{ min: 0 }}
                        error={!!errors.daysPast}
                        helperText={errors.daysPast?.message || "History window to sync."}
                        InputProps={{ sx: { fontFamily: "monospace" } }}
                      />
                    )}
                  />
                  <Controller
                    name="daysFuture"
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Days Future"
                        type="number"
                        fullWidth
                        inputProps={{ min: 0 }}
                        error={!!errors.daysFuture}
                        helperText={errors.daysFuture?.message || "Future window to sync."}
                        InputProps={{ sx: { fontFamily: "monospace" } }}
                      />
                    )}
                  />
                </Box>

                <Box
                  sx={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    border: 1,
                    borderColor: "divider",
                    borderRadius: 1,
                    p: 2,
                  }}
                >
                  <Box>
                    <Typography variant="body1" fontWeight={500}>
                      Debug Logging
                    </Typography>
                    <Typography variant="body2">
                      Capture detailed information in the sync logs.
                    </Typography>
                  </Box>
                  <Controller
                    name="debugLogging"
                    control={control}
                    render={({ field }) => (
                      <Switch checked={field.value} onChange={field.onChange} />
                    )}
                  />
                </Box>
              </Stack>
            </CardContent>
            <Divider />
            <CardActions sx={{ px: 3, py: 2 }}>
              <Button
                type="submit"
                variant="contained"
                disabled={updateSettings.isPending}
                startIcon={updateSettings.isPending ? <CircularProgress size={16} color="inherit" /> : undefined}
              >
                Save Configuration
              </Button>
            </CardActions>
          </form>
        </Card>
      </Stack>
    </Layout>
  );
}
