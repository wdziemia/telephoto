package me.saket.telephoto.sample.gallery

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.size
import me.saket.telephoto.sample.R
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.ZoomableContentTransformation
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable

@Composable
fun ZoomAnywhere(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val zoomableState = rememberZoomableState(
    zoomSpec = ZoomSpec(
      maxZoomFactor = 1f,
      preventOverOrUnderZoom = false,
    ),
  )

  var coordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

  @Suppress("NAME_SHADOWING")
  val content = remember(content, modifier) {
    movableContentOf(content)
  }

  val isZoomedIn = zoomableState.contentTransformation.scaleMetadata.userZoom > 1f
  val overlayView = getOrCreateOverlay()
//  var overlayChild = remember<View?> { null }

  SideEffect {
    println("user zoom = ${zoomableState.contentTransformation.scaleMetadata.userZoom}")
  }

  if (isZoomedIn) {
    val density = LocalDensity.current
    val boundsInWindow = remember {
      coordinates!!.boundsInWindow().let { bounds ->
        density.run {
          DpRect(DpOffset(x = bounds.left.toDp(), y = bounds.top.toDp()), bounds.size.toDpSize())
        }
      }
    }
    overlayView.setContent {
      Box(
        Modifier
          .fillMaxSize()
      ) {
        Box(
          Modifier
            .absoluteOffset(boundsInWindow.left, boundsInWindow.top)
            .requiredSize(boundsInWindow.size)
            .applyTransformation { zoomableState.contentTransformation }
        ) {
          content()
        }
      }
    }
  }

  Box(
    modifier
      .onPlaced { coordinates = it }
      .zoomable(zoomableState),
    propagateMinConstraints = true,
  ) {
    if (!isZoomedIn) {
      overlayView.setContent { }
      content()
    }
  }
}

@Stable
internal fun Modifier.applyTransformation(transformation: () -> ZoomableContentTransformation): Modifier {
  return graphicsLayer {
    @Suppress("NAME_SHADOWING")
    val transformation = transformation()
    scaleX = transformation.scale.scaleX
    scaleY = transformation.scale.scaleY
    rotationZ = transformation.rotationZ
    translationX = transformation.offset.x
    translationY = transformation.offset.y
    transformOrigin = transformation.transformOrigin
  }
}

@Composable
private fun getOrCreateOverlay(): ComposeView {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  return remember(activity) {
    activity.insertOverlay()
  }
}

fun Activity.insertOverlay(): ComposeView {
  val decorView = window.decorView as? ViewGroup
    ?: throw IllegalStateException("Unable to access the decor view.")

  // Check if the overlay already exists
  val existingOverlay = decorView.findViewById<ComposeView>(R.id.overlay_foo)
  if (existingOverlay != null) {
    return existingOverlay
  }

  // Locate the content view within the decor view
//  val contentView = decorView.findViewById<ViewGroup>(android.R.id.content)
//    ?: throw IllegalStateException("Unable to find the content view.")

  // Create a new overlay
  val overlayLayout = ComposeView(this).apply {
    id = R.id.overlay_foo
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
  }

  // Add the overlay as a sibling to the content view
  decorView.addView(overlayLayout)

  return overlayLayout
}

//@Stable
//@SuppressLint("ViewConstructor")
//private class OverlayViewGroup(
//  context: Context,
//  private val decorContentView: View,
//) : AbstractComposeView(context) {
//
//  var overlayContent: @Composable (() -> Unit)? = null
//
//  @Composable
//  override fun Content() {
//    Box(
//      Modifier.fillMaxSize()
//    ) {
//      AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = { decorContentView },
//      )
//
//      overlayContent?.invoke()
//    }
//  }
//}

// todo: delete this when LocalActivity is available in androidx.activity.
private tailrec fun Context.findActivity(): Activity {
  return when (this) {
    is Activity -> this
    is ContextWrapper -> this.baseContext.findActivity()
    else -> throw IllegalArgumentException("Could not find activity!")
  }
}
