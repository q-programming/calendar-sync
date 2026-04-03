@echo off

start "CalendarSync Server" java -jar calendarsync.jar ^
 --server.port=9090 ^
 --spring.datasource.url=jdbc:h2:file:./calendarsync;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE ^
 --spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID ^
 --spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET

timeout /t 5 >nul

start msedge http://localhost:9090/calendarsync
