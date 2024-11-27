package com.mtdevelopment.core.presentation.composable

import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import com.mtdevelopment.core.presentation.R
import me.rmyhal.contentment.ContentLoadingIndicator

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

            Box(
                modifier = Modifier,
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.8f))
                )

                AndroidView(
                    modifier = modifier
                        .then(semantics)
                        .clipToBounds(),
                    factory = { context ->
                        riveAnimationView = RiveAnimationView(context).apply {
                            setRiveResource(
                                resId,
                                artboardName,
                                animationName,
                                stateMachineName,
                                autoplay,
                                fit,
                                alignment,
                                loop
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