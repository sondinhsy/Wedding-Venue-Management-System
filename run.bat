@echo off
echo Building project...
call mvn clean compile
if %errorlevel% neq 0 (
    echo Build failed!
    pause
    exit /b %errorlevel%
)

echo.
echo Running application...
call mvn javafx:run
pause

