# Lattice

Lattice is a focused Android reader for Markdown and Obsidian vaults stored on GitHub. Sign in, select a repository, browse its folders, and read notes in a calm native interface.

[Download the latest Android APK](https://github.com/itishrishikesh/lattice-android/releases/latest)

## Highlights

- GitHub OAuth device flow, with fine-grained personal access tokens as a no-backend fallback
- Private and public repository support
- Fast repository-wide note search
- Obsidian wiki links (`[[Note]]`, `[[Note|Label]]`, and heading suffixes)
- Markdown headings, lists, tasks, quotes, code fences, dividers, front matter, tags, inline code, bold, and strikethrough
- Encrypted credential storage using Android Keystore
- Local cache for previously opened notes
- Recent notes and light/dark reading themes

## Run the app

1. Open this directory in Android Studio Quail 4 (2026.1.4) or newer.
2. Let Gradle sync and install Android SDK 37 if prompted.
3. Run the `app` configuration on an Android 7.0+ device or emulator.

The app works immediately with a GitHub fine-grained personal access token. For a read-only vault, grant only:

- **Contents:** Read-only
- **Metadata:** Read-only

## Enable “Continue with GitHub”

GitHub's device flow requires a client ID owned by the app publisher; it cannot be safely invented or shared in source control.

1. Create a GitHub OAuth App under **Settings → Developer settings → OAuth Apps**.
2. Enable **Device Flow** for the OAuth App.
3. Add this line to your untracked `local.properties` file:

   ```properties
   github.clientId=YOUR_OAUTH_APP_CLIENT_ID
   ```

No client secret is embedded in the app. OAuth credentials and manually entered tokens are encrypted locally.

## Project structure

```text
app/src/main/java/com/lattice/notes/
├── data/            GitHub API, encrypted storage, cache, models
├── ui/components/   Reusable visual components
├── ui/markdown/     Small Obsidian-aware Markdown parser
├── ui/screens/      Sign in, repository picker, explorer, reader
└── ui/theme/        Paper-and-ink Material 3 theme
```

## Current scope

Lattice is deliberately read-only: your Git repository remains the source of truth. Standard Markdown images, Obsidian embeds, graph view, submodules, and repositories whose Git tree is truncated by GitHub are sensible next additions.

## Security notes

- Tokens are encrypted with an AES-GCM key held by Android Keystore.
- Android backups are disabled so credentials are not copied to cloud backup.
- The app requests no permissions beyond internet access.
- Logs never include credentials or note content.
