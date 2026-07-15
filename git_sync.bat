@echo off
SETLOCAL EnableDelayedExpansion

echo ==========================================
echo       Git Safe Sync (Pull & Push)
echo ==========================================
echo.

:: Check if git is installed
where git >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Git is not installed or not in PATH.
    echo Please install Git and try again.
    pause
    exit /b 1
)

:: Check if there are uncommitted changes
git diff-index --quiet HEAD --
if %ERRORLEVEL% neq 0 (
    echo [INFO] Uncommitted changes detected. Stashing changes...
    git stash
    set STASHED=1
) else (
    set STASHED=0
)

:: Pull with rebase
echo [INFO] Fetching and rebasing from remote...
git pull --rebase
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Git pull failed. This could be due to network issues or conflicts.
    if !STASHED! equ 1 (
         echo [INFO] Restoring your stashed changes...
         git stash pop
    )
    pause
    exit /b 1
)

:: Push changes
echo [INFO] Pushing changes to remote...
git push
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Git push failed.
    if !STASHED! equ 1 (
         echo [INFO] Restoring your stashed changes...
         git stash pop
    )
    pause
    exit /b 1
)

:: Restore stash if we stashed earlier
if !STASHED! equ 1 (
    echo [INFO] Restoring your local stashed changes...
    git stash pop
)

echo.
echo [SUCCESS] Git sync completed successfully without merge conflicts!
pause
