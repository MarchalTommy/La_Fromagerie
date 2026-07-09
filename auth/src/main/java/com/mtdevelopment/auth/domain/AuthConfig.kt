package com.mtdevelopment.auth.domain

/**
 * Configuration of the minimalist admin gate.
 *
 * The admin app authenticates a single operator with a numeric PIN. Under the hood
 * that PIN is the *password* of one fixed Firebase Auth account ([ADMIN_EMAIL]); the
 * operator never sees or types the address. This gives the app a real authenticated
 * Firebase identity (`request.auth != null`), which is what the hardened Firestore
 * rules gate on — see `la-fromagerie-backend/firestore.rules.hardened-proposal`.
 *
 * SETUP (one-time, Firebase console, project la-fromagerie-25560 — cannot be done from
 * the app): enable the Email/Password sign-in provider, then create a single user with
 * email exactly [ADMIN_EMAIL] and the chosen PIN as its password. Changing the PIN =
 * editing that user's password in the console.
 *
 * Threat model / honesty: a 6-digit PIN is low entropy (10^6). Online brute force is
 * bounded by Firebase Auth's built-in per-account/IP throttling, and the admin APK is
 * hand-distributed (no store), so this is an intentional convenience/security trade-off
 * for a one-operator shop — not bank-grade auth.
 */
object AuthConfig {

    /** Number of digits the operator enters. Change here to switch to a 4-digit PIN. */
    const val PIN_LENGTH: Int = 6

    /**
     * Fixed identity of the single admin operator. NOT a secret (the PIN is the secret
     * and lives only in Firebase Auth). Must match the user created in the console.
     */
    const val ADMIN_EMAIL: String = "admin@lafromagerie.app"
}
