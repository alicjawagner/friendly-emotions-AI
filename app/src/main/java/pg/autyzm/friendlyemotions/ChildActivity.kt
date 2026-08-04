package pg.autyzm.friendlyemotions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import pg.autyzm.friendlyemotions.child.navigation.ChildNavigationHost
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

@AndroidEntryPoint
class ChildActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FriendlyEmotionsTheme {
                ChildNavigationHost()
            }
        }
    }
}
