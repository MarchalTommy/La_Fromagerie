# Firestore security rules — export & audit (2026-07-08)

## What happened

The 2026-07-07 payment audit flagged that no Firestore rules existed in the repo
(`firebase.json` had no `firestore` section; the rules lived only in the console).
On 2026-07-08 the live rules were exported read-only from project
`la-fromagerie-25560` (`firebase init firestore`, account
`marchal.development@gmail.com`) into this directory:

- `firestore.rules` — the deployed rules, verbatim. Keep it in sync with the
  console: it is now wired into `firebase.json`, so `firebase deploy` (without
  `--only functions`) would push it. Because it is identical to what is live,
  an accidental deploy is a no-op.
- `firestore.indexes.json` — deployed composite indexes (none exist).
- `.firebaserc` — project association, so deploys/exports work from this folder.

## Finding — CRITICAL: production database is world-writable

The deployed rules are:

```
match /{document=**} {
  allow read: if true;
  allow write: if true;
}
```

The in-file comment says writes are admin-only; the actual expression is
`if true`. Combined with the absence of any user auth in the app, **anyone with
the Firebase API key — which ships in every APK and is not a secret — can read,
create, overwrite, and delete every document in every collection**: `orders`
(the money-side record), `products` (catalog and prices), `delivery_paths`,
`preparation_status`, `database_update`. Concretely: a paid order can be erased
or its status flipped, prices can be rewritten, and the client cache-invalidation
timestamps can be poisoned — all with plain `curl`.

Mitigating notes, for honesty:
- The hosted-checkout amount is derived server-side from `orders/{id}.total_price`
  (Cloud Function, Admin SDK), and payment verification compares the paid amount
  to the same field — so tampering `total_price` before paying only changes what
  the customer is charged AND what verification expects; it does not let an
  attacker underpay someone else's order.
- The blast radius is one small shop, but the exposure is total.

## Proposed next step (needs Tommy's sign-off — do NOT deploy from a dev session)

`firestore.rules.hardened-proposal` is a drafted replacement. Without auth in the
app it can only:
- lock `orders` down to the exact write shapes both flavors produce (create
  PENDING-with-positive-total or manual IN_PREPARATION; update = status-only;
  delete = never),
- validate the `database_update` document shape,
- remove the catch-all so unknown collections are denied.

It CANNOT protect `products`/`delivery_paths`/`preparation_status` writes while
the admin flavor is anonymous. The real fix is the "no admin auth" item in
`fromagerie-operations-hardening-frontier` (Firebase Auth for the admin flavor +
App Check), after which the `TODO(admin-auth)` blocks flip to
`request.auth != null`.

Deployment path when approved: review the proposal, rename it over
`firestore.rules`, test with the Firestore emulator or the console Rules
Playground (order create/status-update/manual-order scenarios for BOTH flavors,
old APKs included), then `firebase deploy --only firestore:rules` — and update
this file plus the payment-campaign skill.

## Re-verification one-liners

- Repo matches console: from this directory, `firebase init firestore` into a
  scratch dir and diff, or read the rules in the console.
- Rules wired for deploy: `grep -A3 '"firestore"' firebase.json`
- Proposal still undeployed: the console rules still show the `if true` writes.
