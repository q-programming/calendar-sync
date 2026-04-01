import { Switch, Route, Router as WouterRouter } from "wouter";
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

function Router() {
  return (
    <Switch>
      <Route path="/" component={Home} />
      <Route path="/profile" component={ProfilePage} />
      <Route path="/settings" component={SettingsPage} />
      <Route path="/logs" component={LogsPage} />
      <Route component={NotFound} />
    </Switch>
  );
}

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <SnackbarProvider>
          <WouterRouter base="">
            <Router />
          </WouterRouter>
        </SnackbarProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

export default App;
