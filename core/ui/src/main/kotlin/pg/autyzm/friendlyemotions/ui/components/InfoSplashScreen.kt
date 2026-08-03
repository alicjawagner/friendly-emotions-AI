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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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

/** Matches a `http(s)://...` run of non-whitespace characters inside a bullet body string. */
private val URL_PATTERN = Regex("https?://\\S+")

/**
 * The 5-second child-and-therapist-app info splash, modeled on the Figma "screens/Starting-board"
 * node (`1015:4978`): a full-bleed purple background, an app icon + title header, a subtitle, a
 * white card of "about" bullets, a mascot + speech bubble illustration, and a translucent bottom
 * bar with sponsor logos. Shared between `:feature:child` and the future Phase 9 therapist welcome
 * screen — only [appTitle] and [appIconRes] differ between callers.
 *
 * The whole screen is a single tap target that invokes [onContinue]. The two URLs inside the
 * bullet card are independently clickable: each is wrapped in its own [LinkAnnotation.Url] rather
 * than a manual `pointerInput`, which is the standard Compose mechanism for a clickable substring
 * inside a larger `Text` — it only consumes taps that land on the link itself, letting every other
 * tap (elsewhere on the card, or on the background) continue bubbling up to the outer [onContinue].
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
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SplashHeader(appTitle = appTitle, appIconRes = appIconRes)
            Text(
                text = stringResource(R.string.core_ui_splash_subtitle),
                style = FriendlyEmotionsTextStyles.headingH5Regular,
                color = FriendlyEmotionsColors.Shades.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(SUBTITLE_WIDTH_FRACTION),
            )
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 32.dp, end = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SplashInfoCard(modifier = Modifier.weight(CARD_WEIGHT))
                Box(modifier = Modifier.weight(MASCOT_AREA_WEIGHT).fillMaxHeight()) {
                    SplashMascotArea()
                }
            }
        }
        SplashBottomBar(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth())
    }
}

@Composable
private fun SplashHeader(
    appTitle: String,
    @DrawableRes appIconRes: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(appIconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .size(64.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
        )
        Text(
            text = appTitle,
            style = FriendlyEmotionsTextStyles.displayD2,
            color = FriendlyEmotionsColors.PrimaryFriendlyEmotions.P1000,
        )
    }
}

@Composable
private fun SplashInfoCard(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .background(FriendlyEmotionsColors.Shades.White, FriendlyEmotionsModalShape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BulletRow(
            iconRes = R.drawable.no_money,
            label = stringResource(R.string.core_ui_splash_bullet1_label),
            body = stringResource(R.string.core_ui_splash_bullet1_body),
        )
        BulletRow(
            iconRes = R.drawable.school,
            label = stringResource(R.string.core_ui_splash_bullet2_label),
            body = stringResource(R.string.core_ui_splash_bullet2_body),
        )
        BulletRow(
            iconRes = R.drawable.puzzle,
            label = stringResource(R.string.core_ui_splash_bullet3_label),
            body = stringResource(R.string.core_ui_splash_bullet3_body),
        )
        BulletRow(
            iconRes = R.drawable.teddy_bear,
            label = stringResource(R.string.core_ui_splash_bullet4_label),
            body = stringResource(R.string.core_ui_splash_bullet4_body),
        )
        BulletRow(
            iconRes = R.drawable.world,
            label = stringResource(R.string.core_ui_splash_bullet5_label),
            body = stringResource(R.string.core_ui_splash_bullet5_body),
        )
    }
}

@Composable
private fun BulletRow(
    @DrawableRes iconRes: Int,
    label: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = bulletText(label, body),
            style = FriendlyEmotionsTextStyles.bodyRegular,
            color = FriendlyEmotionsColors.Shades.Black,
        )
    }
}

/**
 * A bold [label] followed by [body], with any embedded URL turned into an independently
 * clickable link (see the [InfoSplashScreen] doc for why [LinkAnnotation.Url] and not a manual
 * `Modifier.clickable` is used here).
 */
private fun bulletText(
    label: String,
    body: String,
): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(label) }
        append(" ")
        val urlMatch = URL_PATTERN.find(body)
        if (urlMatch == null) {
            append(body)
        } else {
            append(body.substring(0, urlMatch.range.first))
            withLink(
                LinkAnnotation.Url(
                    url = urlMatch.value,
                    styles = TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline)),
                ),
            ) {
                append(urlMatch.value)
            }
            append(body.substring(urlMatch.range.last + 1))
        }
    }

@Composable
private fun SplashMascotArea() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.background_ellipse),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(ELLIPSE_SIZE)
                    .rotate(BACKGROUND_ELLIPSE_ROTATION_DEGREES),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = SPEECH_BUBBLE_MAX_WIDTH)
                    .background(FriendlyEmotionsColors.PrimaryFriendlyEmotions.P300, RoundedCornerShape(10.dp))
                    .padding(10.dp),
        ) {
            Text(
                text = stringResource(R.string.core_ui_splash_continue_hint),
                style = FriendlyEmotionsTextStyles.bodyRegular,
                color = FriendlyEmotionsColors.Shades.Black,
                textAlign = TextAlign.Center,
            )
        }
        Image(
            painter = painterResource(R.drawable.mascot),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.BottomEnd).size(MASCOT_SIZE),
        )
    }
}

@Composable
private fun SplashBottomBar(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(FriendlyEmotionsColors.Overlay.S500)
                .padding(vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.gdansk_university_of_technology_and_eti_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(48.dp).width(230.dp),
            )
            Image(
                painter = painterResource(R.drawable.iwrd_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(56.dp).width(160.dp),
            )
        }
    }
}

private const val SUBTITLE_WIDTH_FRACTION = 0.75f
private const val CARD_WEIGHT = 1.1f
private const val MASCOT_AREA_WEIGHT = 0.7f
private const val BACKGROUND_ELLIPSE_ROTATION_DEGREES = -2f
private val ELLIPSE_SIZE = 240.dp
private val MASCOT_SIZE = 160.dp
private val SPEECH_BUBBLE_MAX_WIDTH = 180.dp

@Preview(showBackground = true, widthDp = 960, heightDp = 600)
@Composable
private fun InfoSplashScreenPreview() {
    FriendlyEmotionsTheme {
        InfoSplashScreen(
            appTitle = "Friendly Emotions",
            appIconRes = R.drawable.friendly_emotions_logo,
            onContinue = {},
        )
    }
}
