package pg.autyzm.friendlyemotions.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pg.autyzm.friendlyemotions.ui.R
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsColors
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsModalShape
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTextStyles
import pg.autyzm.friendlyemotions.ui.theme.FriendlyEmotionsTheme

/**
 * Shared 5-second info splash, modeled on the Figma "screens/Starting-board" node (`1015:4978`).
 * Verbatim between the child app and the Phase 9 therapist "Settings" welcome screen — only
 * [appTitle] and [appIconRes] differ between the two callers.
 *
 * The whole screen advances via [onContinue] on tap; the two URLs inside the bullet list use
 * [LinkAnnotation.Url] instead of a manually nested `clickable`, since [Text] already resolves a
 * tap against a link range internally and only invokes the link handler for taps that land on it.
 */
@Composable
fun InfoSplashScreen(
    appTitle: String,
    @DrawableRes appIconRes: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P50)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onContinue,
                ),
    ) {
        Image(
            painter = painterResource(R.drawable.background_ellipse),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(
                        x = 90.dp,
                        y = 370.dp,
                    )
                    .size(550.dp),
        )
        Image(
            painter = painterResource(R.drawable.mascot),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 70.dp, bottom = 140.dp)
                    .size(200.dp),
        )
        SpeechBubble(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 70.dp, bottom = 350.dp)
                    .widthIn(max = 200.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        painter = painterResource(appIconRes),
                        contentDescription = null,
                        modifier = Modifier.size(90.dp),
                    )
                    Text(
                        text = appTitle,
                        style = FriendlyEmotionsTextStyles.displayD2,
                        color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.core_ui_splash_subtitle),
                    style = FriendlyEmotionsTextStyles.headingH5Regular,
                    color = FriendlyEmotionsColors.Shades.Black,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier =
                    Modifier
                        .padding(start = 70.dp)
                        .widthIn(max = 800.dp)
                        .background(FriendlyEmotionsColors.Shades.White, FriendlyEmotionsModalShape)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SPLASH_BULLETS.forEach { bullet ->
                    BulletRow(bullet.emojiRes, bullet.labelRes, bullet.bodyRes)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(FriendlyEmotionsColors.Shades.White.copy(alpha = 0.5f)),
                horizontalArrangement = Arrangement.spacedBy(64.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.gdansk_university_of_technology_and_eti_logo),
                    contentDescription = null,
                    modifier = Modifier.height(80.dp),
                    contentScale = ContentScale.FillHeight,
                )
                Image(
                    painter = painterResource(R.drawable.iwrd_logo),
                    contentDescription = null,
                    modifier = Modifier.height(80.dp),
                    contentScale = ContentScale.FillHeight,
                )
            }
        }
    }
}

@Composable
private fun SpeechBubble(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300, RoundedCornerShape(10.dp))
                .padding(12.dp),
    ) {
        Text(
            text = stringResource(R.string.core_ui_splash_continue_hint),
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
            textAlign = TextAlign.Center,
        )
    }
}

private data class SplashBullet(
    @DrawableRes val emojiRes: Int,
    val labelRes: Int,
    val bodyRes: Int,
)

private val SPLASH_BULLETS =
    listOf(
        SplashBullet(R.drawable.no_money, R.string.core_ui_splash_bullet1_label, R.string.core_ui_splash_bullet1_body),
        SplashBullet(R.drawable.school, R.string.core_ui_splash_bullet2_label, R.string.core_ui_splash_bullet2_body),
        SplashBullet(R.drawable.puzzle, R.string.core_ui_splash_bullet3_label, R.string.core_ui_splash_bullet3_body),
        SplashBullet(
            R.drawable.teddy_bear,
            R.string.core_ui_splash_bullet4_label,
            R.string.core_ui_splash_bullet4_body,
        ),
        SplashBullet(R.drawable.world, R.string.core_ui_splash_bullet5_label, R.string.core_ui_splash_bullet5_body),
    )

@Composable
private fun BulletRow(
    @DrawableRes emojiRes: Int,
    labelRes: Int,
    bodyRes: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(emojiRes),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )
        Text(
            text = bulletText(labelRes, bodyRes),
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.Shades.Black,
        )
    }
}

@Composable
private fun bulletText(
    labelRes: Int,
    bodyRes: Int,
): AnnotatedString {
    val label = stringResource(labelRes)
    val body = stringResource(bodyRes)
    val linkStart = body.indexOf("https://")
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(label)
        }
        append(" ")
        if (linkStart >= 0) {
            append(body.substring(0, linkStart))
            val url = body.substring(linkStart)
            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
                ),
            ) {
                append(url)
            }
        } else {
            append(body)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun InfoSplashScreenPreview() {
    FriendlyEmotionsTheme {
        InfoSplashScreen(
            appTitle = "Friendly Emotions",
            appIconRes = R.drawable.mascot,
            onContinue = {},
        )
    }
}
