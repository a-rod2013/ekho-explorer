# Contributing

## Branching
- `main`: stable, demo-ready. Only updated by merging `develop` at the end of a sprint.
- `develop`: integration branch. All feature work merges here by pull request.
- `feature/<short-name>`: your work. Always branch from `develop`.
  - Examples: `feature/auth-screen`, `feature/species-search`, `fix/map-crash`

Never commit directly to `main` or `develop`.

## Daily workflow
```bash
git checkout develop
git pull
git checkout -b feature/my-feature
# ...work, commit often...
git fetch origin
git merge origin/develop     # do this at least daily to catch conflicts early
git push -u origin feature/my-feature
# open a PR into develop
```

## Pull requests
- Keep them small (ideally under ~400 changed lines).
- At least one teammate must review before merge.
- CI must pass.
- Squash-merge into `develop` to keep history readable.

## Commit messages
Short imperative summary, e.g. `Add observation capture screen`. Optional prefix: `feat:`, `fix:`, `docs:`, `chore:`.

## Shared files: handle with care
These cause most merge conflicts. Keep edits minimal and tell the team before big changes:
- `gradle/libs.versions.toml` and any `build.gradle.kts`: **only the designated build owner changes versions**. Need a new dependency? Ask in chat or open an issue.
- `AndroidManifest.xml`
- `res/values/strings.xml`: prefix keys by feature (`feed_title`, `auth_error_invalid`)
- The central navigation file
- 'ui/theme/*'

## Do not
- Accept Android Studio prompts to upgrade AGP/Gradle/Kotlin on your own.
- Commit `local.properties`, `google-services.json`, keystores, or API keys.
- Reformat whole files you did not otherwise change.

## Styling
- Read colors, text styles, spacing, and shapes from the theme (see [docs/STYLE_GUIDE.md](docs/STYLE_GUIDE.md)).
- Don't hardcode hex values, font sizes, font names, or `dp`/`sp` numbers in screens.
- Wrap every `@Preview` in `EkhoExplorerTheme`.
- Changes to files in `ui/theme/` go in a small PR, are announced in chat, and update the style guide.

