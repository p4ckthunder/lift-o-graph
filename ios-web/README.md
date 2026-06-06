# Lift-O-Graph iOS Web Version

This is the separate iPhone/Safari version of Lift-O-Graph. It is a static Progressive Web App, so it can be hosted as plain files and shared with a normal link.

## What This Version Does

- Runs in Safari on iPhone.
- Can be added to the iPhone Home Screen.
- Stores training days, exercises, logs, settings, and API connector settings in browser local storage.
- Works offline after the first successful load.
- Keeps the Android app untouched.

## Local Preview

From this folder:

```powershell
python -m http.server 4173
```

Open:

```text
http://localhost:4173
```

## Sharing It With People

Host the contents of this `ios-web` folder on any static web host, then send people the HTTPS URL.

Good simple hosting options:

- GitHub Pages
- Netlify
- Vercel
- Cloudflare Pages

On iPhone, users open the URL in Safari. To install it like an app, they use Safari's share button and choose Add to Home Screen.

## GitHub Pages Setup

This repo includes a GitHub Actions workflow that publishes this folder to GitHub Pages.

1. Create a GitHub repository for Lift-O-Graph.
2. Push this project to the repository.
3. In GitHub, open the repo settings.
4. Go to Pages.
5. Under Build and deployment, set Source to GitHub Actions.
6. Push any change to `main` or `master`, or run the Deploy iOS web app to GitHub Pages workflow manually.

After the workflow finishes, GitHub shows the public iPhone link in the Pages settings and on the workflow run.

The usual URL shape is:

```text
https://YOUR-GITHUB-USERNAME.github.io/YOUR-REPO-NAME/
```

## Important Notes

- iPhone data is local to each user's browser unless you connect the optional API connector or add cloud sync later.
- Safari requires HTTPS for reliable PWA behavior once hosted publicly.
- Voice input depends on browser speech recognition support. The manual text parser works without it.
