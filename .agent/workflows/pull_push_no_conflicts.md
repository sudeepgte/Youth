---
description: How to push and pull code without merge conflicts
---

Follow these steps to safely push and pull your code without generating cluttered merge commits or conflicts.

## Pre-requisites
We have configured your local repository (`.git/config`) with the following settings:
- **`pull.rebase = true`**: Automatically rebases your local commits on top of the incoming commits from the remote branch, instead of creating a merge commit.
- **`rebase.autoStash = true`**: Automatically stashes any uncommitted local changes before pulling, and unstashes (pops) them after the pull/rebase completes.

---

### Step 1: Work on your local changes
Write your code, make modifications, and run tests locally.

### Step 2: Keep your work staged/committed or clean
Choose one of the two approaches before pulling:
1. **Commit your changes locally (Recommended)**:
   ```bash
   git add .
   git commit -m "Your descriptive commit message"
   ```
2. **Leave them unstaged/uncommitted**:
   Since `autoStash = true` is configured, Git will automatically stash them for you during pull, but committing is always safer.

### Step 3: Pull the latest changes from the remote
Run:
```bash
git pull
```
- Git will fetch the remote changes.
- It will stash your uncommitted changes if any exist.
- It will rewind your local commits, apply the remote commits, and then re-apply your local commits on top.
- It will restore (pop) your stashed changes.

> [!NOTE]
> If a conflict does happen (e.g., you and someone else edited the exact same line of code), Git will pause and ask you to resolve it. Since you are rebasing, you resolve conflicts and then run `git rebase --continue`.

### Step 4: Push your clean history to the remote
Once the pull succeeds, push your changes:
```bash
git push
```
Your commits will be cleanly appended to the remote branch without any messy "Merge branch 'main'..." commits.
