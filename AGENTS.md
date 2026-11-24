# ZenMath Android — Agent Rules

## Post-Change Commit, Push & Release Tagging

Always after finishing a significant change in this project (after verifying tests/build pass):

1. Create a proper conventional commit (`feat:`, `fix:`, `refactor:`, etc.).
2. If the `gitid` CLI tool is available (`command -v gitid`), use it (following the `gitid` skill; never reveal tokens) to push the branch and push a new `vX.Y.Z` tag so GitHub Actions (`.github/workflows/release.yml`) builds and publishes the release APK:
   - `gitid push personal origin HEAD` (or the bound profile from `gitid status --json`)
   - `git tag -a vX.Y.Z -m "<summary>"`
   - `gitid push personal origin vX.Y.Z`
3. If `gitid` is unavailable, fall back to standard `git push` for both the branch and tag.

See [.agents/rules/git-release-workflow.md](.agents/rules/git-release-workflow.md) for the full rule.
