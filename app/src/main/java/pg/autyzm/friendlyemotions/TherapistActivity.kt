package pg.autyzm.friendlyemotions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

@AndroidEntryPoint
class TherapistActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FriendlyEmotionsTheme {
                // placeholder Surface until Phase 5/9 add real screens
            }
        }
    }
}
