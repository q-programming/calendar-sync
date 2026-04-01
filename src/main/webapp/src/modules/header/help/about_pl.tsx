import { Box, List, ListItem, ListItemText } from '@mui/material';

export const AboutPl = () => {
    return (
        <Box data-testid='about-pl'>
            <List dense>
                <ListItem>
                    <ListItemText
                        primary='O tej stronie'
                        secondary='To strona do zarządzania synchronizacją wydarzeń Outlook <-> Kalendarz Google.'
                    />
                </ListItem>
                <ListItem>
                    <ListItemText
                        primary='Integracja z Kalendarzem Google'
                        secondary='Połącz swoje konto Google, aby wybrać kalendarz i synchronizować nadchodzące wydarzenia dla wybranego zakresu dni.
                         Jeżeli chcesz usunąć integrację, po prostu rozłącz swoje konto Google w Ustawieniach.'
                    />
                </ListItem>
            </List>
        </Box>
    );
};
