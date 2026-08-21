package pg.autyzm.friendlyemotions.therapist.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Thin [Scaffold] wrapper pairing [TherapistTopBar] with a screen's content, so every therapist
 * screen only supplies its content lambda instead of re-wiring the topbar each time.
 */
@Composable
fun TherapistScaffold(
    title: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    modifier: Modifier = Modifier,
    showHomeButton: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TherapistTopBar(
                title = title,
                onBackClick = onBackClick,
                onHomeClick = onHomeClick,
                showHomeButton = showHomeButton,
            )
        },
        content = content,
    )
}
