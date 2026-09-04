package com.quserh.eorzeaphone.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset

/**
 * Navigation transitions, shared by every AnimatedContent in the app.
 * Spec: 开发/UI-redesign-2026/DESIGN-SPEC.md §5.
 *
 * Springs, not fixed-duration tweens. A tween restarts from zero when interrupted, so
 * double-tapping back replays the whole slide from scratch; a spring continues from the
 * current value and retargeting only moves the goal.
 *
 * dampingRatio is 1.0 everywhere. Bounce is only honest when the gesture itself carried
 * momentum, and tapping a tile or the back button carries none.
 *
 * Stiffness values are measured, not guessed -- 开发/WIKI/_probe_spring_mapping.py and
 * _probe_spring_perceived.py integrate the same model Compose uses. Judge them by t90
 * ("looks done"), not by settle time, which includes a tail of a few sub-pixel frames.
 */
object PhoneMotionSpec {

    /** Forward push. t90 195ms. == Spring.StiffnessMediumLow, Apple response 0.314s. */
    const val PushStiffness = 400f

    /**
     * Back, and every fade-out. t90 148ms.
     *
     * Faster than push on purpose: back is the most-pressed control in the app, and the
     * frequency rule says high-frequency actions get less animation, not equal animation.
     */
    const val PopStiffness = 700f

    /** Reduced motion keeps a fade so the swap stays legible, and drops all travel. */
    const val ReducedInMillis = 90
    const val ReducedOutMillis = 60

    /**
     * How far the outgoing screen travels, as a fraction of the incoming screen's travel.
     *
     * This is the whole source of the "silky" feel in an iOS push: two layers moving at
     * different speeds read as depth. Equal travel reads as one flat filmstrip sliding by.
     */
    const val ParallaxFraction = 0.32f

    /**
     * Slides need an explicit visibility threshold: the default one is tuned for values
     * around 1.0, and applying it to a 900px offset leaves the spring running long after
     * the pixels stopped moving.
     */
    fun offset(stiffness: Float): FiniteAnimationSpec<IntOffset> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = stiffness,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

    fun alpha(stiffness: Float): FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = stiffness,
    )
}

/**
 * One transition for the whole app, picked by depth.
 *
 * There is deliberately no zoom-from-the-tapped-tile variant. A screen scaled below 1.0
 * cannot reach the display edges, and it also depends on AnimatedContent drawing the
 * entering child above the leaving one; a plain push has neither problem and is what iOS
 * uses for hierarchy anyway. Spatial consistency is worth more here than a launch flourish.
 *
 * Enter and exit share one spring on purpose. Asymmetry belongs between *directions*
 * (push vs pop), not between the two halves of a single slide -- mismatched specs make the
 * outgoing screen drift away from the incoming one and open a visible seam.
 */
fun AnimatedContentTransitionScope<*>.phoneNavTransition(
    motionAllowed: Boolean,
    targetDepth: Int,
    initialDepth: Int,
    instantPopToRoot: Boolean = false,
): ContentTransform {
    // Leaving any feature for the phone desktop is a terminal navigation action.  Do not
    // retain the outgoing feature layer long enough to fade it out: several feature
    // screens have their own AnimatedContent, and the two exit animations used to stack
    // into a visible dim/ghost frame (especially when the back button was tapped during a
    // child transition).  An immediate opaque swap is both cheaper and visually stable;
    // pushes and sibling navigation keep their normal motion below.
    if (instantPopToRoot && initialDepth > 0 && targetDepth == 0) {
        return EnterTransition.None togetherWith ExitTransition.None
    }
    if (!motionAllowed) {
        return fadeIn(tween(PhoneMotionSpec.ReducedInMillis)) togetherWith
            fadeOut(tween(PhoneMotionSpec.ReducedOutMillis))
    }

    // Same depth == siblings, not hierarchy. Sliding would claim a parent/child move that
    // did not happen, so siblings crossfade. This is also the old bug: level() had three
    // tiers, so any 1 -> 1 move (app to app, Contacts to Chat) replayed the forward push.
    if (targetDepth == initialDepth) {
        return fadeIn(PhoneMotionSpec.alpha(PhoneMotionSpec.PushStiffness)) togetherWith
            fadeOut(PhoneMotionSpec.alpha(PhoneMotionSpec.PopStiffness))
    }

    val forward = targetDepth > initialDepth
    val stiffness = if (forward) PhoneMotionSpec.PushStiffness else PhoneMotionSpec.PopStiffness
    // Out and back along the same axis: a screen that entered from the right leaves to the
    // right. SlideDirection.Left means "travel leftward", i.e. enter from the right edge.
    val direction = if (forward) {
        AnimatedContentTransitionScope.SlideDirection.Left
    } else {
        AnimatedContentTransitionScope.SlideDirection.Right
    }
    val travel = PhoneMotionSpec.offset(stiffness)

    // No fade on either side. Both screens stay fully opaque for the whole push, which is
    // what stops the backdrop showing through -- crossfading two opaque full-screen
    // surfaces necessarily exposes whatever is behind them at the midpoint, and that read
    // as a black flash on entry. iOS does not fade pushes either.
    //
    // The silkiness is parallax instead: the arriving screen travels the full width while
    // the leaving one moves PARALLAX_FRACTION of it, so they move at different speeds and
    // the new screen reads as sliding *over* the old one rather than swapping with it.
    return slideIntoContainer(direction, travel).togetherWith(
        slideOutOfContainer(direction, travel) { full ->
            (full * PhoneMotionSpec.ParallaxFraction).toInt()
        },
    )
}
