import {Link, useLocation} from 'react-router-dom';
import {createContext, useContext, useState} from 'react';
import Box from '@mui/material/Box';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PersonIcon from '@mui/icons-material/Person';
import SettingsIcon from '@mui/icons-material/Settings';
import DescriptionIcon from '@mui/icons-material/Description';
import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import MenuIcon from '@mui/icons-material/Menu';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';

const DRAWER_WIDTH = 240;
const DRAWER_COLLAPSED_WIDTH = 64;

const SidebarContext = createContext({ collapsed: false, toggle: () => {} });
export const useSidebar = () => useContext(SidebarContext);

const navigation = [
    { name: 'Cockpit', href: '/', icon: <DashboardIcon /> },
    { name: 'Profile', href: '/profile', icon: <PersonIcon /> },
    { name: 'Settings', href: '/settings', icon: <SettingsIcon /> },
    { name: 'Logs', href: '/logs', icon: <DescriptionIcon /> },
    {name: 'Help', href: '/help', icon: <HelpOutlineIcon/>},
];

export function Layout({ children }: { children: React.ReactNode }) {
    const location = useLocation();
    const [collapsed, setCollapsed] = useState(false);
    const toggle = () => setCollapsed((c) => !c);
    const drawerWidth = collapsed ? DRAWER_COLLAPSED_WIDTH : DRAWER_WIDTH;

    return (
        <SidebarContext.Provider value={{ collapsed, toggle }}>
            <Box sx={{ display: 'flex', minHeight: '100dvh' }}>
                <Drawer
                    variant='permanent'
                    sx={{
                        width: drawerWidth,
                        flexShrink: 0,
                        transition: 'width 200ms',
                        '& .MuiDrawer-paper': {
                            width: drawerWidth,
                            boxSizing: 'border-box',
                            transition: 'width 200ms',
                            overflowX: 'hidden',
                            borderRight: '1px solid',
                            borderColor: 'divider',
                        },
                    }}
                >
                    <Box
                        sx={{
                            height: 64,
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: collapsed ? 'center' : 'space-between',
                            px: collapsed ? 1 : 2,
                        }}
                    >
                        {!collapsed && (
                            <Typography
                                variant='subtitle2'
                                sx={{
                                    fontFamily: "'JetBrains Mono', monospace",
                                    fontWeight: 700,
                                    letterSpacing: '-0.02em',
                                }}
                            >
                                CALENDAR_SYNC
                            </Typography>
                        )}
                        <IconButton
                            onClick={toggle}
                            size='small'
                            data-testid='button-toggle-sidebar'
                        >
                            {collapsed ? (
                                <MenuIcon fontSize='small' />
                            ) : (
                                <MenuOpenIcon fontSize='small' />
                            )}
                        </IconButton>
                    </Box>
                    <Divider />
                    <List sx={{ px: 1, pt: 1 }}>
                        {navigation.map((item) => {
                            const isActive = location.pathname === item.href;
                            const button = (
                                <ListItemButton
                                    key={item.name}
                                    component={Link}
                                    to={item.href}
                                    selected={isActive}
                                    sx={{
                                        borderRadius: 1,
                                        mb: 0.5,
                                        minHeight: 44,
                                        justifyContent: collapsed ? 'center' : 'flex-start',
                                        px: collapsed ? 1.5 : 2,
                                        '&.Mui-selected': {
                                            backgroundColor: 'primary.main',
                                            color: 'primary.contrastText',
                                            '&:hover': { backgroundColor: 'primary.dark' },
                                            '& .MuiListItemIcon-root': {
                                                color: 'primary.contrastText',
                                            },
                                        },
                                    }}
                                >
                                    <ListItemIcon
                                        sx={{
                                            minWidth: collapsed ? 0 : 36,
                                            mr: collapsed ? 0 : 1.5,
                                            justifyContent: 'center',
                                            color: isActive ? 'inherit' : 'text.secondary',
                                        }}
                                    >
                                        {item.icon}
                                    </ListItemIcon>
                                    {!collapsed && (
                                        <ListItemText
                                            primary={item.name}
                                            primaryTypographyProps={{
                                                fontSize: '0.875rem',
                                                fontWeight: 500,
                                            }}
                                        />
                                    )}
                                </ListItemButton>
                            );

                            if (collapsed) {
                                return (
                                    <Tooltip
                                        key={item.name}
                                        title={item.name}
                                        placement='right'
                                        arrow
                                    >
                                        {button}
                                    </Tooltip>
                                );
                            }
                            return <Box key={item.name}>{button}</Box>;
                        })}
                    </List>
                </Drawer>

                <Box
                    component='main'
                    sx={{
                        flexGrow: 1,
                        height: '100dvh',
                        overflow: 'auto',
                        backgroundColor: 'background.default',
                    }}
                >
                    <Box sx={{ maxWidth: 960, mx: 'auto', p: 4 }}>{children}</Box>
                </Box>
            </Box>
        </SidebarContext.Provider>
    );
}
