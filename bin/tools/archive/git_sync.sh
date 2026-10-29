#!/bin/bash

# Git Safe Sync Script
echo "=========================================="
echo "      Git Safe Sync (Pull & Push)"
echo "=========================================="
echo ""

# Check if git is installed
if ! command -v git &> /dev/null; then
    echo "[ERROR] Git is not installed or not in PATH."
    exit 1
fi

# Check for uncommitted changes
if ! git diff-index --quiet HEAD --; then
    echo "[INFO] Uncommitted changes detected. Stashing changes..."
    git stash
    STASHED=1
else
    STASHED=0
fi

# Pull with rebase
echo "[INFO] Fetching and rebasing from remote..."
if ! git pull --rebase; then
    echo ""
    echo "[ERROR] Git pull failed."
    if [ $STASHED -eq 1 ]; then
        echo "[INFO] Restoring your stashed changes..."
        git stash pop
    fi
    exit 1
fi

# Push changes
echo "[INFO] Pushing changes to remote..."
if ! git push; then
    echo ""
    echo "[ERROR] Git push failed."
    if [ $STASHED -eq 1 ]; then
        echo "[INFO] Restoring your stashed changes..."
        git stash pop
    fi
    exit 1
fi

# Restore stash if needed
if [ $STASHED -eq 1 ]; then
    echo "[INFO] Restoring your local stashed changes..."
    git stash pop
fi

echo ""
echo "[SUCCESS] Git sync completed successfully without merge conflicts!"
