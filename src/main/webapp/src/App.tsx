import { BrowserRouter, Routes, Route } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ThemeProvider } from "@mui/material/styles";
import CssBaseline from "@mui/material/CssBaseline";
import theme from "@/theme";
import { SnackbarProvider } from "@/components/snackbar-provider";
import NotFound from "@/pages/not-found";

import Home from "@/pages/home";
import ProfilePage from "@/pages/profile";
import SettingsPage from "@/pages/settings";
import LogsPage from "@/pages/logs";

const queryClient = new QueryClient();

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider>
          <BrowserRouter basename="/calendarsync">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/profile" element={<ProfilePage />} />
              <Route path="/settings" element={<SettingsPage />} />
              <Route path="/logs" element={<LogsPage />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </BrowserRouter>
        </SnackbarProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

export default App;
