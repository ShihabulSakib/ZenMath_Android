---
trigger: always_on
---

# Git Commit, Push & Tag Release Rule

Always follow this workflow after finishing any significant change (feature, bug fix, UI/UX update, or refactor) in this project:

1. **Verify before committing**: Ensure tests and builds pass (`./gradlew testDebugUnitTest`).
2. **Make a proper commit**: Stage the relevant changes and create a clear, conventional commit message (`feat:`, `fix:`, `refactor:`, etc.).
3. **Push code and a version tag using `gitid` (if available)**:
   - Check if `gitid` is available (`command -v gitid`).
   - If `gitid` is available, follow the `gitid` skill conventions (never reveal tokens or inspect `~/.config/gitid/profiles.json`), verify the bound profile (`gitid status --json`), and push using `gitid`:
     - Push commits on the current branch (e.g., `gitid push personal origin HEAD` or the repo's bound profile).
     - Determine the next semantic version tag (`vX.Y.Z`, incrementing patch for fixes or minor for features based on `git tag --sort=-v:refname | head -n 5`).
     - Create an annotated tag (`git tag -a vX.Y.Z -m "<summary>"`) and push it (`gitid push personal origin vX.Y.Z`) so `.github/workflows/release.yml` automatically compiles and publishes the signed release APK.
   - If `gitid` is not available in the environment, fall back to standard `git push origin HEAD` and `git push origin vX.Y.Z` (or inform the user if authentication is required).
