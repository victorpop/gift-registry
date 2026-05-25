# Deploy Log — 260525-gqs fetchOgMetadata

**Date:** 2026-05-25
**Target project:** gift-registry-ro
**Region:** europe-west3
**Runtime:** nodejs22
**Function:** fetchOgMetadata (2nd Gen, callable)

## Deploy Command

```
firebase deploy --only functions:fetchOgMetadata --project gift-registry-ro
```

## CLI Output

```
=== Deploying to 'gift-registry-ro'...

i  deploying functions
i  functions: preparing codebase default for deployment
i  functions: ensuring required API cloudfunctions.googleapis.com is enabled...
i  functions: ensuring required API cloudbuild.googleapis.com is enabled...
i  artifactregistry: ensuring required API artifactregistry.googleapis.com is enabled...
⚠  functions: package.json indicates an outdated version of firebase-functions. Please upgrade using npm install --save firebase-functions@latest in your functions directory.
i  functions: Loading and analyzing source code for codebase default to determine what to deploy
Serving at port 8744

i  extensions: ensuring required API firebaseextensions.googleapis.com is enabled...
i  functions: Loaded environment variables from .env.
i  functions: preparing functions directory for uploading...
i  functions: packaged /Users/victorpop/ai-projects/gift-registry/functions (293.55 KB) for uploading
i  functions: ensuring required API cloudtasks.googleapis.com is enabled...
i  functions: ensuring required API run.googleapis.com is enabled...
i  functions: ensuring required API eventarc.googleapis.com is enabled...
i  functions: ensuring required API pubsub.googleapis.com is enabled...
i  functions: ensuring required API storage.googleapis.com is enabled...
i  functions: generating the service identity for pubsub.googleapis.com...
i  functions: generating the service identity for eventarc.googleapis.com...
✔  functions: functions source uploaded successfully
i  functions: updating Node.js 22 (2nd Gen) function fetchOgMetadata(europe-west3)...
✔  functions[fetchOgMetadata(europe-west3)] Successful update operation.

✔  Deploy complete!

Project Console: https://console.firebase.google.com/project/gift-registry-ro/overview
```

## Verification — firebase functions:list

```
firebase functions:list --project gift-registry-ro | grep fetchOgMetadata

│ fetchOgMetadata │ v2 │ callable │ europe-west3 │ 256 │ nodejs22 │
```

**Result:** Deploy confirmed — 2nd Gen callable, europe-west3, nodejs22.

## Notes

- Outdated firebase-functions advisory noted (same as Phase 16-06) — non-blocking, advisory only.
- Only `fetchOgMetadata` was redeployed. No other functions, rules, or hosting were touched.
- Phase 16 UAT mid-flight functions (notifications, invites, reservations) were NOT touched.
