import { Box, List, ListItem, ListItemText } from '@mui/material';

export const AboutEn = () => {
    return (
        <Box data-testid='about-en'>
            <List dense>
                <ListItem>
                    <ListItemText
                        secondary='This is App to manage sync  of Outlook <-> Google Calendar'
                    />
                </ListItem>
                <ListItem>
                    <ListItemText
                        primary='Google Calendar integration'
                        secondary='Connect your Google account to select calendars and sync events for a chosen number of days.
                         simply disconnect your Google account in Settings.'
                    />
                </ListItem>
            </List>
        </Box>
    );
};
