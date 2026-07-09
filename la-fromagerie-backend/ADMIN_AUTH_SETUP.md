# Admin PIN auth — one-time Firebase console setup (2026-07-08)

The admin flavor now gates behind a numeric PIN (`:auth` module, added on branch
`claude/admin-pin-auth`). Under the hood the PIN is the **password** of a single
fixed Firebase Auth account; the operator only ever types digits. This gives the
admin app a real authenticated identity (`request.auth != null`), which is the
prerequisite for actually enforcing the hardened Firestore rules
(`firestore.rules.hardened-proposal` — see `FIRESTORE_RULES_AUDIT.md`).

**The app cannot create this account itself** (and dev sessions never write to prod).
These two steps must be done once by the owner in the Firebase console for project
`la-fromagerie-25560`:

1. **Enable the provider.** Authentication → Sign-in method → enable
   **Email/Password** (the plain one; "Email link" not needed).
2. **Create the single operator account.** Authentication → Users → Add user:
   - Email: **`admin@lafromagerie.app`** — must match `AuthConfig.ADMIN_EMAIL`
     exactly (it is an internal identifier, not a real mailbox; nothing is sent to it).
   - Password: the **6-digit PIN** you want to type in the app (e.g. `481920`).

That's it. On next admin-app launch the lock screen appears; entering the PIN signs
in, and Firebase keeps the session across launches (the PIN is asked once per install
unless the user is signed out).

## Changing the PIN

Edit that user's password in the console (Authentication → Users → ⋮ → Reset/Edit).
No app release needed. To move to a 4-digit PIN instead of 6, change
`AuthConfig.PIN_LENGTH` in the `:auth` module and ship a new admin build, then set a
4-digit password.

## What this is and is not

- **Is:** a convenience gate for a hand-distributed, single-operator admin APK, plus
  the authenticated identity the Firestore-rules hardening needs.
- **Is not:** high-entropy auth. A 6-digit PIN is 10^6 combinations; the real
  protections are Firebase Auth's built-in throttling of repeated failures and the
  fact that the APK is not on any store. Do not reuse this pattern for anything
  customer-facing.

## Verifying on device (no payment involved — admin flavor)

`./gradlew :app:installAdminDebug`, launch the app → PIN screen shows → wrong code
shows "Code incorrect." → correct code opens the app → kill & relaunch → app opens
straight to content (session persisted). See `fromagerie-run-and-operate` for adb
mechanics. Until step 1+2 above are done, **every** code returns an error (the
provider/user don't exist yet) — that is expected, not a bug.
