package com.example.epubreader.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.epubreader.R
import com.example.epubreader.reader.ReaderTheme

/**
 * The reader's one-time gesture guide. It is a plain walkthrough card, not anchored
 * callouts: the toolbar can be hidden or the page can still be opening, and the card
 * is readable either way.
 */
object ReaderGuide {
    private const val PREFS = "readerGuide"
    private const val SHOWN = "shown"

    fun pending(context: Context): Boolean =
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(SHOWN, false)

    fun done(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(SHOWN, true)
            .apply()
    }
}

private class GuideStep(val title: Int, val body: Int)

@Composable
fun ReaderGuideOverlay(onDone: () -> Unit) {
    val steps = remember {
        listOf(
            GuideStep(R.string.guide_turn_title, R.string.guide_turn_body),
            GuideStep(R.string.guide_chrome_title, R.string.guide_chrome_body),
            GuideStep(R.string.guide_mark_title, R.string.guide_mark_body),
            GuideStep(R.string.guide_voice_title, R.string.guide_voice_body),
        )
    }
    var step by remember { mutableIntStateOf(0) }
    val last = step == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF12100E).copy(alpha = 0.62f))
            // The guide owns every touch, so nothing underneath reacts to it.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ReaderTheme.Paper)
                .padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(
                text = "${step + 1}/${steps.size}",
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = ReaderTheme.Accent,
            )
            Text(
                text = stringResource(steps[step].title),
                modifier = Modifier.padding(top = 6.dp),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = ReaderTheme.Ink,
            )
            Text(
                text = stringResource(steps[step].body),
                modifier = Modifier.padding(top = 8.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = ReaderTheme.Ink.copy(alpha = 0.78f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                for (index in steps.indices) {
                    Box(
                        modifier = Modifier
                            .size(if (index == step) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == step) ReaderTheme.Accent
                                else ReaderTheme.Ink.copy(alpha = 0.20f),
                            ),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDone) {
                    Text(
                        text = stringResource(R.string.guide_skip),
                        fontFamily = FontFamily.Serif,
                        color = ReaderTheme.Ink.copy(alpha = 0.6f),
                    )
                }
                Box(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = { if (last) onDone() else step++ },
                ) {
                    Text(
                        text = stringResource(
                            if (last) R.string.guide_start else R.string.guide_next,
                        ),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = ReaderTheme.Accent,
                    )
                }
            }
        }
    }
}
