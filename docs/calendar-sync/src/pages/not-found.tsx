import Box from "@mui/material/Box";
import Card from "@mui/material/Card";
import CardContent from "@mui/material/CardContent";
import Typography from "@mui/material/Typography";
import ErrorIcon from "@mui/icons-material/Error";

export default function NotFound() {
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        bgcolor: "background.default",
      }}
    >
      <Card sx={{ maxWidth: 400, mx: 2 }}>
        <CardContent>
          <Box sx={{ display: "flex", alignItems: "center", gap: 1, mb: 2 }}>
            <ErrorIcon color="error" fontSize="large" />
            <Typography variant="h5">404 Page Not Found</Typography>
          </Box>
          <Typography variant="body2" color="text.secondary">
            Did you forget to add the page to the router?
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
}
