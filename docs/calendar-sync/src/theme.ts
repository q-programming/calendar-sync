import { createTheme } from "@mui/material/styles";

const theme = createTheme({
  palette: {
    primary: {
      main: "#3b82f6",
      contrastText: "#fff",
    },
    secondary: {
      main: "#64748b",
    },
    error: {
      main: "#ef4444",
    },
    warning: {
      main: "#f59e0b",
    },
    success: {
      main: "#10b981",
    },
    background: {
      default: "#f1f5f9",
      paper: "#ffffff",
    },
    text: {
      primary: "#1e293b",
      secondary: "#64748b",
    },
  },
  typography: {
    fontFamily: "'Inter', sans-serif",
    h4: {
      fontWeight: 600,
      letterSpacing: "-0.01em",
    },
    h5: {
      fontWeight: 600,
      letterSpacing: "-0.01em",
    },
    h6: {
      fontWeight: 600,
    },
    body2: {
      color: "#64748b",
    },
  },
  shape: {
    borderRadius: 4,
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: "0px 1px 3px rgba(0,0,0,0.06), 0px 1px 2px rgba(0,0,0,0.04)",
          border: "1px solid #e2e8f0",
          borderRadius: 4,
        },
      },
    },
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: "none",
          fontWeight: 500,
          borderRadius: 4,
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: "0.75rem",
          borderRadius: 4,
        },
      },
    },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 4,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        rounded: {
          borderRadius: 4,
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 600,
          color: "#64748b",
          fontSize: "0.8125rem",
        },
      },
    },
  },
});

export default theme;
