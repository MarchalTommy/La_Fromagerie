package com.mtdevelopment.core.presentation.composable

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.controllers.RiveFileController
import app.rive.runtime.kotlin.core.Alignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import app.rive.runtime.kotlin.core.PlayableInstance
import app.rive.runtime.kotlin.core.Rive
import com.mtdevelopment.core.presentation.R
import me.rmyhal.contentment.ContentLoadingIndicator

/**
 * Process-wide, one-shot initialization of the Rive runtime.
 *
 * rive-android ships an `androidx.startup` initializer (`RiveInitializer`) but its own AAR
 * manifest removes the meta-data that registers it (`tools:node="remove"` — verified in
 * 9.6.5, 10.5.3 and 11.1.2). Nothing therefore auto-loads `librive-android.so`: calling
 * [Rive.init] is the application's job. Constructing a [RiveAnimationView] beforehand dies
 * with `UnsatisfiedLinkError: No implementation found for ... FileAssetLoader.constructor()`
 * the moment the view builds its asset loader.
 *
 * That initialization used to live in the home and delivery screens, which made a loaded
 * native library a property of the *navigation history* rather than of the process. Any
 * composition that reached a Rive overlay without those screens having run first crashed —
 * most plausibly after process death, since Compose Navigation restores the back stack and
 * composes only the destination the user was on, never the start destination. Anchoring the
 * call to [RiveAnimation] — the only composable in the app that builds a [RiveAnimationView] —
 * makes the guarantee structural instead of incidental.
 *
 * Kept private on purpose: [RiveAnimation] is the only place in the app that instantiates a
 * Rive view, so there is no legitimate caller that could need to initialize without one.
 */
private object RiveRuntime {

    private const val TAG = "RiveRuntime"

    @Volatile
    private var initialized = false

    @Volatile
    private var available = false

    /**
     * Loads the Rive native library once per process and reports whether a
     * [RiveAnimationView] can now be built. Cheap (a volatile read) on every call after the
     * first; the lock exists so a second thread cannot start building a view while the
     * library is still being loaded.
     *
     * Uses the application context: [Rive.init] hands it to ReLinker, which may keep it for
     * the lifetime of the load, and an Activity context has no business being retained there.
     *
     * A failure is swallowed on purpose. This animation is decoration on top of a loading
     * state — a device that cannot load `librive-android.so` (an ABI the store served wrong,
     * a ReLinker extraction failure on a full disk) must lose the goat, not the checkout.
     * The attempt is never repeated: a native library that failed to load once will not load
     * on the next frame either, and retrying would re-throw on every single recomposition.
     */
    fun ensureInitialized(context: Context): Boolean {
        if (initialized) return available
        return synchronized(this) {
            if (!initialized) {
                try {
                    Rive.init(context.applicationContext)
                    available = true
                } catch (throwable: Throwable) {
                    // Throwable rather than Exception: a missing native symbol arrives as
                    // UnsatisfiedLinkError, which is an Error.
                    Log.e(TAG, "Rive runtime unavailable, falling back to a static overlay", throwable)
                    available = false
                }
                initialized = true
            }
            available
        }
    }
}

@Suppress("LongMethod")
@Composable
fun RiveAnimation(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    @RawRes resId: Int = R.raw.goat_loading,
    autoplay: Boolean = true,
    artboardName: String? = null,
    animationName: String? = null,
    stateMachineName: String? = null,
    fit: Fit = Fit.CONTAIN,
    alignment: Alignment = Alignment.CENTER,
    loop: Loop = Loop.AUTO,
    contentDescription: String?,
    notifyLoop: ((PlayableInstance) -> Unit)? = null,
    notifyPause: ((PlayableInstance) -> Unit)? = null,
    notifyPlay: ((PlayableInstance) -> Unit)? = null,
    notifyStateChanged: ((String, String) -> Unit)? = null,
    notifyStop: ((PlayableInstance) -> Unit)? = null,
    update: (RiveAnimationView) -> Unit = { _ -> }
) {

    ContentLoadingIndicator(
        loading = isLoading,
        minShowTimeMillis = 750,
        delayMillis = 200
    ) {
        var riveAnimationView: RiveAnimationView? = null
        val listener: RiveFileController.Listener?
        val lifecycleOwner = LocalLifecycleOwner.current

        if (LocalInspectionMode.current) { // For Developing only,
            Box(modifier = Modifier) {
                Surface(
                    modifier = modifier
                        .background(Color.Black)
                        .alpha(0.2f)
                        .fillMaxSize(),
                ) {}
                Image(
                    modifier = modifier.size(100.dp),
                    painter = painterResource(id = R.drawable.placeholder), //any image
                    contentDescription = contentDescription
                )
            }
        } else {
            val semantics = if (contentDescription != null) {
                Modifier.semantics {
                    this.contentDescription = contentDescription
                    this.role = Role.Image
                }
            } else {
                Modifier
            }
            listener = object : RiveFileController.Listener {
                override fun notifyLoop(animation: PlayableInstance) {
                    notifyLoop?.invoke(animation)
                }

                override fun notifyPause(animation: PlayableInstance) {
                    notifyPause?.invoke(animation)
                }

                override fun notifyPlay(animation: PlayableInstance) {
                    notifyPlay?.invoke(animation)
                }

                override fun notifyStateChanged(
                    stateMachineName: String,
                    stateName: String
                ) {
                    notifyStateChanged?.invoke(stateMachineName, stateName)
                }

                override fun notifyStop(animation: PlayableInstance) {
                    notifyStop?.invoke(animation)
                }
            }.takeIf {
                (notifyLoop != null) || (notifyPause != null) ||
                        (notifyPlay != null) || (notifyStateChanged != null) ||
                        (notifyStop != null)
            }

            // Initialized here rather than in the AndroidView factory: the factory runs
            // during applyChanges and cannot change what gets composed, so it has no way to
            // fall back when the native runtime is missing. `remember` pins the verdict for
            // the lifetime of this overlay; the call itself is idempotent process-wide.
            val localContext = LocalContext.current
            val isRiveAvailable = remember { RiveRuntime.ensureInitialized(localContext) }

            Box(
                modifier = Modifier,
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                )

                if (isRiveAvailable) {
                    AndroidView(
                        modifier = modifier
                            .then(semantics)
                            .clipToBounds(),
                        factory = { context ->
                            riveAnimationView = RiveAnimationView(context).apply {
                                setRiveResource(
                                    resId = resId,
                                    artboardName = artboardName,
                                    animationName = animationName,
                                    stateMachineName = stateMachineName,
                                    autoplay = autoplay,
                                    fit = fit,
                                    loop = loop,
                                    alignment = alignment
                                )
                            }
                            listener?.let {
                                riveAnimationView?.registerListener(it)
                            }
                            riveAnimationView!!
                        },
                        update = {
                            update.invoke(it)
                        }
                    )
                } else {
                    // Degraded overlay: the scrim above still blocks the screen and reads as
                    // "busy", which is the part of this component that carries the meaning.
                    // The animation is the only thing lost.
                    Image(
                        modifier = Modifier
                            .size(100.dp)
                            .then(semantics),
                        painter = painterResource(id = R.drawable.placeholder),
                        contentDescription = contentDescription
                    )
                }
            }

            DisposableEffect(lifecycleOwner) {
                onDispose {
                    listener?.let {
                        riveAnimationView?.unregisterListener(it)
                    }
                }
            }
        }
    }

}

@Composable
@Preview(showSystemUi = true)
fun RiveComposablePreview() {
    RiveAnimation(
        isLoading = true,
        resId = R.raw.goat_loading,
        autoplay = true,
        animationName = "Bouncing",
        contentDescription = "Just a Rive Animation"
    )
}