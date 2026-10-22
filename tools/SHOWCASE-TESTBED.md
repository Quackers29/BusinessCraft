# Showcase testbed

The testbed reuses the **two-town** showcase layout (town A on the pad, town B at minimum legal distance) and runs **pluggable checks** you can extend while developing features.

## Run

```bash
bash tools/showcase.sh testbed forge
bash tools/showcase.sh testbed fabric
bash tools/showcase.sh testbed forge trade-contract
bash tools/showcase.sh testbed fabric trade-contract,example-dev-item
```

Omit check names to run every registered check.

## Output layout

Every `tools/showcase.sh` run writes a timestamped folder:

```text
showcase-output/<YYYY-MM-DD_HHMMSS>_<mode>_<loader>/
  screenshots/
  logs/client.log          # Minecraft latest.log (full client session)
  logs/server.log          # Dedicated server log when present; else integrated note
  logs/gradle-console.log  # Gradle + JVM console
  run-meta.json
  … mode-specific JSON / steps.log / checks.log / index.html …
```

Symlinks: `showcase-output/latest`, `showcase-output/latest_<mode>_<loader>`, and `showcase-output/index.html`.

Set `SHOWCASE_ARTIFACT_DIR=/opt/cursor/artifacts/...` to mirror a copy elsewhere. The `showcase-output/` tree is gitignored.

## Registered checks

| Name | Purpose |
|------|---------|
| `trade-contract` | Sell auction from town A, bid from town B, contract board + detail UI, time-skipped delivery |
| `example-dev-item` | Starter template (opens trade from town B); copy to build real dev-item checks |

## Add a new check

1. Implement `ShowcaseTestbedCheck` in `common/.../client/showcase/testbed/`.
2. Register it in `ShowcaseTestbedCheckRegistry` static block.
3. Run: `bash tools/showcase.sh testbed forge your-check-name`

### Example skeleton

```java
public final class MyFeatureCheck implements ShowcaseTestbedCheck {
    @Override
    public String name() {
        return "my-feature";
    }

    @Override
    public void begin(Minecraft minecraft, ShowcaseTestbedContext context) {
        Showcase.setActiveTownBlockPos(context.townBPos());
    }

    @Override
    public boolean tick(Minecraft minecraft, ShowcaseTestbedContext context) {
        // Drive UI via ShowcaseNavigator, or schedule server work on
        // minecraft.getSingleplayerServer().execute(...)
        return false; // return true when finished
    }

    @Override
    public boolean passed() {
        return true;
    }

    @Override
    public Map<String, Object> details() {
        return Map.of();
    }
}
```

Register:

```java
register("my-feature", MyFeatureCheck::new);
```

Use `context.townAPos()`, `context.townBPos()`, `context.townAId()`, and `context.put(...)` to share data between ticks.
