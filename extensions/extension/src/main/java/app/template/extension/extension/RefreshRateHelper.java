package app.template.extension.extension;

/**
 * RefreshRateHelper — runtime helper that opts NYT Games into the display's
 * highest / variable refresh rate.
 *
 * Why this is needed:
 *   The NYT Games app never votes for a frame rate (no setFrameRate /
 *   preferredRefreshRate / preferredDisplayModeId anywhere in its own code).
 *   On high-refresh (90/120 Hz) and VRR panels, Android's power-saving
 *   "frame rate override" therefore pins the app to 60 Hz — the panel drops
 *   to 60 Hz and the app's Choreographer is only serviced 60 times/sec,
 *   producing the 60 fps judder. Voting for the panel's top mode from the
 *   app's own window lifts that override so the UI can render up to the
 *   panel's maximum (and adapt below it on VRR displays).
 *
 * Strategy:
 *   install() is invoked once from the Application's onCreate. It registers a
 *   process-wide ActivityLifecycleCallbacks that, for every Activity window,
 *   sets preferredRefreshRate + preferredDisplayModeId to the highest-refresh
 *   mode available at the current resolution. On Android 15+ (API 35) it also
 *   sets the view's requested frame-rate category to HIGH via reflection.
 *
 * Everything is wrapped in try/catch so a failure can never crash the host app.
 */

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public final class RefreshRateHelper {

    private static volatile boolean installed = false;

    private RefreshRateHelper() {
    }

    /** Called once from the Application's onCreate. p0 == the Application. */
    public static void install(final Application app) {
        if (app == null || installed) {
            return;
        }
        installed = true;
        try {
            app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    apply(activity);
                }

                @Override
                public void onActivityStarted(Activity activity) {
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    // Re-apply on resume: covers windows recreated after config changes.
                    apply(activity);
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    /** Vote the given Activity's window onto the display's highest refresh mode. */
    public static void apply(Activity activity) {
        try {
            if (activity == null) {
                return;
            }
            Window window = activity.getWindow();
            if (window == null) {
                return;
            }
            Display display = displayOf(activity);
            if (display == null) {
                return;
            }

            Display.Mode current = display.getMode();
            Display.Mode[] modes = display.getSupportedModes();

            Display.Mode best = current;
            float bestRate = current != null ? current.getRefreshRate() : 0f;

            if (modes != null && current != null) {
                // Prefer the highest refresh rate that keeps the current resolution.
                for (Display.Mode m : modes) {
                    if (m.getPhysicalWidth() == current.getPhysicalWidth()
                            && m.getPhysicalHeight() == current.getPhysicalHeight()
                            && m.getRefreshRate() > bestRate + 0.1f) {
                        best = m;
                        bestRate = m.getRefreshRate();
                    }
                }
            }

            WindowManager.LayoutParams lp = window.getAttributes();
            boolean changed = false;

            if (best != null && lp.preferredDisplayModeId != best.getModeId()) {
                lp.preferredDisplayModeId = best.getModeId();
                changed = true;
            }
            if (bestRate > 0f && Math.abs(lp.preferredRefreshRate - bestRate) > 0.1f) {
                lp.preferredRefreshRate = bestRate;
                changed = true;
            }
            if (changed) {
                window.setAttributes(lp);
            }

            // Android 15+ frame-rate category API — nudge the decor view to HIGH.
            if (Build.VERSION.SDK_INT >= 35 && bestRate > 0f) {
                requestHighFrameRate(window.getDecorView(), bestRate);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Display displayOf(Activity activity) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Display d = activity.getDisplay();
                if (d != null) {
                    return d;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return activity.getWindowManager().getDefaultDisplay();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** View.setRequestedFrameRate(float) via reflection (added in API 35). */
    private static void requestHighFrameRate(View view, float rate) {
        try {
            if (view == null) {
                return;
            }
            View.class.getMethod("setRequestedFrameRate", float.class).invoke(view, rate);
        } catch (Throwable ignored) {
        }
    }
}
