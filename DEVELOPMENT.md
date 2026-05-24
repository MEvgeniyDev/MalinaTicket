# Development

MalinaTicket is a small Paper/Purpur 1.21.11 plugin built against Paper API 26.1.2. The project intentionally stays close to the Bukkit/Paper API and avoids runtime dependencies beyond what the server already provides.

## Local Build

```powershell
.\gradlew.bat clean build
```

The build writes:

- `build/libs/MalinaTicket-26.5.8.jar`
- `build/libs/MalinaTicket-26.5.8.jar.sha256`

For a public release build, set the source revision before building:

```powershell
$env:GIT_COMMIT = "<commit hash>"
.\gradlew.bat clean build
```

If `GIT_COMMIT` is not set, the JAR manifest keeps `Built-From-Revision: local`.

## Release Checklist

1. Run `.\gradlew.bat clean test`.
2. Run `.\gradlew.bat clean build`.
3. Inspect the JAR with `jar tf build/libs/MalinaTicket-26.5.8.jar`.
4. Copy the JAR to a clean Paper/Purpur 1.21.11 server with API 26.1.2.
5. Verify `/ticket`, create, comment, close, staff GUI, reload, and restart persistence.
6. Confirm Russian messages render correctly in chat, GUI lore, and console.

## Maintenance Notes

- Keep ticket storage backward-compatible unless the changelog says otherwise.
- Do not put live server data or generated build output into source control.
- If a public repository is created under a different URL, update `website` in `plugin.yml` before the next release.
