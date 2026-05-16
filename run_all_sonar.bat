@echo off
echo ============================================
echo   ParkEase - SonarQube Analysis Runner
echo ============================================

SET ROOT=%~dp0

echo.
echo [1/10] Analysing auth-service...
cd "%ROOT%auth-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] auth-service analysis failed.

echo.
echo [2/10] Analysing payment-service...
cd "%ROOT%payment-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] payment-service analysis failed.

echo.
echo [3/10] Analysing vehicle-service...
cd "%ROOT%vehicle-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] vehicle-service analysis failed.

echo.
echo [4/10] Analysing parkingspot-service...
cd "%ROOT%parkingspot-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] parkingspot-service analysis failed.

echo.
echo [5/10] Analysing parkinglot-service...
cd "%ROOT%parkinglot-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] parkinglot-service analysis failed.

echo.
echo [6/10] Analysing notification-service...
cd "%ROOT%notification-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] notification-service analysis failed.

echo.
echo [7/10] Analysing booking-service...
cd "%ROOT%booking-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] booking-service analysis failed.

echo.
echo [8/10] Analysing analytics-service...
cd "%ROOT%analytics-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] analytics-service analysis failed.

echo.
echo [9/10] Analysing discovery-service...
cd "%ROOT%discovery-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] discovery-service analysis failed.

echo.
echo [10/10] Analysing gateway-service...
cd "%ROOT%gateway-service"
call mvnw.cmd clean verify sonar:sonar
IF %ERRORLEVEL% NEQ 0 echo [WARN] gateway-service analysis failed.

echo.
echo ============================================
echo   All services analysed!
echo   Open http://localhost:9000 to view results.
echo ============================================
pause
