package app.template.patches.nytgames

import app.morphe.patcher.Fingerprint

/**
 * The Hilt-generated Application base class holds the real onCreate().
 *
 * Class hierarchy in 6.34.0:
 *   ReleaseAppCrosswordApplication            (manifest android:name, no onCreate)
 *     -> Hilt_ReleaseAppCrosswordApplication  (generated, defines onCreate)  <-- target
 *       -> CrosswordApplication               (app base)
 *
 * onCreate cannot be renamed/obfuscated (it overrides android.app.Application),
 * and p0 inside it is the Application instance, which is exactly what
 * RefreshRateHelper.install(Application) needs.
 */
internal val applicationOnCreateFingerprint = Fingerprint(
    definingClass = "Lcom/nytimes/games/core/Hilt_ReleaseAppCrosswordApplication;",
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
)
