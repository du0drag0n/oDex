package io.lemoncore.opendatadex.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect

/** The shared-transition scope around the app's screens, so a Pokémon's artwork can travel between them. */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransition = compositionLocalOf<SharedTransitionScope?> { null }

/** The enter/exit animation of the screen being composed (set per navigation destination). */
val LocalScreenAnimation = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Whether the artwork composed here is the one on show, for content that stays composed while something covers it
 * (the Pokédex list under a Pokémon opened from it); null where the screen's own animation decides.
 */
val LocalArtworkShown = compositionLocalOf<Boolean?> { null }

/**
 * Marks a Pokémon's artwork as the same picture on every screen, keyed by its form id: opening a Pokémon grows its
 * list artwork into the page's, and going back flies it home to its card, as in dataDex 4.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedArtwork(formId: Int): Modifier {
    val shared = LocalSharedTransition.current ?: return this
    val shown = LocalArtworkShown.current
    val screen = LocalScreenAnimation.current
    return with(shared) {
        val state = rememberSharedContentState("art-$formId")
        val bounds = { _: Rect, _: Rect -> spring<Rect>(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow) }
        when {
            shown != null -> this@sharedArtwork.sharedElementWithCallerManagedVisibility(state, shown, boundsTransform = bounds)
            screen != null -> this@sharedArtwork.sharedElement(state, screen, boundsTransform = bounds)
            else -> this@sharedArtwork
        }
    }
}
