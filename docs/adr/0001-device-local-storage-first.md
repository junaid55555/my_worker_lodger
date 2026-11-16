# Store data on the device for the first version

The first version keeps all data in a local database on the Android device instead of a shared backend, so in practice one Accommodation Manager (or one shared device) uses it. We chose this to keep the demo small and avoid running infrastructure, accepting that several managers sharing one view of every Property waits for a later backend.

## Consequences

- All storage sits behind a data-layer interface, so a shared backend (Firebase, Supabase or our own server) can replace it later without changing the domain or screens.
- There is no sign-in; the device's own screen lock is the only access control.
- Losing the device can lose the data. We rely on Android's built-in backup and build no export or import in the first version; a shared backend is the real fix.
