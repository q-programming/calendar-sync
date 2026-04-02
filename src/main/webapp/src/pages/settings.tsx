import { useEffect } from "react";
import { Layout } from "@/components/layout";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { updateSettings } from "@/store/settingsSlice";
import { useSnackbar } from "@/components/snackbar-provider";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
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

const schema = z.object({
  frequencyMinutes: z.coerce.number().min(1, "Must be at least 1 minute"),
  daysPast:         z.coerce.number().min(0, "Cannot be negative"),
  daysFuture:       z.coerce.number().min(0, "Cannot be negative"),
  debugLogging:     z.boolean(),
  syncColorLabels:  z.boolean(),
});
type FormValues = z.infer<typeof schema>;

export default function SettingsPage() {
  const dispatch  = useAppDispatch();
  const settings  = useAppSelector(s => s.settings.settings);
  const { showSnackbar } = useSnackbar();

  const { control, handleSubmit, reset, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { frequencyMinutes: 15, daysPast: 7, daysFuture: 30, debugLogging: false, syncColorLabels: true },
  });

  useEffect(() => { if (settings) reset(settings); }, [settings, reset]);

  const onSubmit = (data: FormValues) => {
    dispatch(updateSettings(data))
      .unwrap()
      .then(() => showSnackbar("Settings saved successfully"))
      .catch(() => showSnackbar("Failed to save settings", "error"));
  };

  return (
    <Layout>
      <Stack spacing={4}>
        <Box>
          <Typography variant="h4">System Settings</Typography>
          <Typography variant="body2" sx={{ mt: 1 }}>Configure background job intervals and sync windows.</Typography>
        </Box>

        <Card sx={{ maxWidth: 600 }}>
          <CardHeader title="Sync Configuration" subheader="Changes apply to the next scheduled run." titleTypographyProps={{ fontWeight: 600 }} />
          <form onSubmit={handleSubmit(onSubmit)}>
            <CardContent>
              <Stack spacing={3}>
                <Controller name="frequencyMinutes" control={control} render={({ field }) => (
                  <TextField {...field} label="Sync Frequency (minutes)" type="number" fullWidth inputProps={{ min: 1 }}
                    error={!!errors.frequencyMinutes} helperText={errors.frequencyMinutes?.message || "How often the background job runs."}
                    InputProps={{ sx: { fontFamily: "monospace" } }} />
                )} />

                <Box sx={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 2 }}>
                  <Controller name="daysPast" control={control} render={({ field }) => (
                    <TextField {...field} label="Days Past" type="number" fullWidth inputProps={{ min: 0 }}
                      error={!!errors.daysPast} helperText={errors.daysPast?.message || "History window to sync."}
                      InputProps={{ sx: { fontFamily: "monospace" } }} />
                  )} />
                  <Controller name="daysFuture" control={control} render={({ field }) => (
                    <TextField {...field} label="Days Future" type="number" fullWidth inputProps={{ min: 0 }}
                      error={!!errors.daysFuture} helperText={errors.daysFuture?.message || "Future window to sync."}
                      InputProps={{ sx: { fontFamily: "monospace" } }} />
                  )} />
                </Box>

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", border: 1, borderColor: "divider", borderRadius: 1, p: 2 }}>
                  <Box>
                    <Typography variant="body1" fontWeight={500}>Debug Logging</Typography>
                    <Typography variant="body2">Capture detailed information in the sync logs.</Typography>
                  </Box>
                  <Controller name="debugLogging" control={control} render={({ field }) => (
                    <Switch checked={field.value} onChange={field.onChange} />
                  )} />
                </Box>

                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", border: 1, borderColor: "divider", borderRadius: 1, p: 2 }}>
                  <Box>
                    <Typography variant="body1" fontWeight={500}>Sync Color Labels</Typography>
                    <Typography variant="body2">Mirror Outlook category colors to Google Calendar event colors.</Typography>
                  </Box>
                  <Controller name="syncColorLabels" control={control} render={({ field }) => (
                    <Switch checked={field.value} onChange={field.onChange} />
                  )} />
                </Box>
              </Stack>
            </CardContent>
            <Divider />
            <CardActions sx={{ px: 3, py: 2 }}>
              <Button type="submit" variant="contained">Save Configuration</Button>
            </CardActions>
          </form>
        </Card>
      </Stack>
    </Layout>
  );
}
