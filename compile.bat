@echo off
echo ============================================
echo   Compiling Ticket Reservation System...
echo ============================================

if not exist bin mkdir bin

javac -d bin src\shared\models\*.java src\shared\protocol\*.java src\server\*.java src\client\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo   Compilation SUCCESSFUL!
    echo   Output: bin\
    echo.
    echo   To run the Server:  run_server.bat
    echo   To run the Client:  run_client.bat
) else (
    echo.
    echo   Compilation FAILED. Check errors above.
)
pause
