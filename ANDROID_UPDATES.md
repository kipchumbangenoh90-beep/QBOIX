# Q-BOIX automatic Android updates

Q-BOIX checks the latest published GitHub Release when the user taps **Settings → APP UPDATES**. The release must contain:

- `QBOIX.apk` — signed Android APK
- `update.json` — version and SHA-256 metadata

The updater checks the version, downloads the APK into app-private storage, verifies its SHA-256 checksum, verifies the package ID and installed signing certificate, and then opens Android's package installer. Android asks the user to approve installation. An in-place Android update keeps the app's private data; never uninstall Q-BOIX as part of an update.

## Required signing setup

**The signing key must be exactly the same key used by the Q-BOIX APK already installed on the phone.** A different key causes Android to reject the update. Do not create a new key for an app that is already installed unless you are deliberately planning a data migration/reinstall.

In GitHub, open the repository's **Settings → Secrets and variables → Actions** and create these repository secrets:

- `QBOIX_KEYSTORE_BASE64` — base64 text of the existing release keystore
- `QBOIX_KEY_ALIAS` — alias inside that keystore
- `QBOIX_KEY_PASSWORD` — key password
- `QBOIX_STORE_PASSWORD` — keystore password

On Linux, encode a keystore without adding line breaks:

```bash
base64 -w 0 qboix-release.jks
```

Copy the output into `QBOIX_KEYSTORE_BASE64`. Keep the original keystore and passwords backed up securely. Never commit the keystore, its passwords, or the base64 output to the repository.

## Publishing updates

After all four secrets are configured, every push to `main` starts **Q-BOIX Android Release**. GitHub Actions builds a signed APK, calculates its SHA-256 hash, and publishes a release containing the APK and update metadata. The Settings update button then finds that release.

The release workflow intentionally fails when signing secrets are missing; it must not publish an unsigned or differently signed APK.

## First-install and data-safety note

The first APK installed on a device must be signed with the permanent Q-BOIX release key. If the currently installed app was installed from a debug build, find and use that exact debug keystore as the signing key for compatible in-place updates, or plan a deliberate migration before switching keys. Do not uninstall the current app to test this: uninstalling normally deletes app-private workout data.

Android requires the user to allow Q-BOIX to install app updates and to confirm the package installation. Q-BOIX cannot silently bypass those system protections.
