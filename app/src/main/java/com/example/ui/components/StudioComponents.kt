package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MicProfile
import com.example.model.RecordingMode
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkIconBox
import com.example.ui.theme.DarkSegmentBg
import com.example.ui.theme.DarkTealOnAccent
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.MeterTrack
import com.example.ui.theme.RadioBorderUnselected
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextChipUnselected
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Section Label:
 * font-size: 11px; font-weight: 600; letter-spacing: 1px; text-transform: uppercase; color: #9AA1AB; margin: 20px 0 8px
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        fontFamily = InterFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = TextSecondary,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}

/**
 * Mic Selector Item:
 * height: 52px; padding: 0 12px; margin-bottom: 8px; border-radius: 12px; background: #171A1F; border: 1px solid #262B33
 * .mic.on { border-color: #2DD4BF }
 * .ic { width: 32px; height: 32px; border-radius: 8px; background: #20252C }
 * .rd { width: 16px; height: 16px; border-radius: 50%; border: 1.5px solid #5A626D }
 * .on .rd { border: 5px solid #2DD4BF }
 */
@Composable
fun MicSelectorItem(
    profile: MicProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardBg)
            .border(
                width = 1.dp,
                color = if (isSelected) TealAccent else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp)
            .testTag("mic_${profile.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkIconBox),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = profile.displayName,
                tint = if (isSelected) TealAccent else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Titles
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile.displayName,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            val desc = when (profile) {
                MicProfile.MIC_CLEAR -> "Natural voice, light noise reduction"
                MicProfile.MIC_STUDIO -> "Warm, rich sound with compression"
                MicProfile.MIC_CRISP -> "Bright clarity, strong noise gate"
            }
            Text(
                text = desc,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextSecondary
            )
        }

        // Radio indicator
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 5.dp else 1.5.dp,
                    color = if (isSelected) TealAccent else RadioBorderUnselected,
                    shape = CircleShape
                )
        )
    }
}

/**
 * Mode Chip:
 * height: 32px; padding: 0 14px; border-radius: 16px; border: 1px solid #262B33; background: #171A1F; color: #C4C9D0; font: 500 13px
 * .chip.on { background: #2DD4BF; border-color: #2DD4BF; color: #06231F }
 */
@Composable
fun DesignModeChip(
    name: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) TealAccent else DarkCardBg)
            .border(
                width = 1.dp,
                color = if (isSelected) TealAccent else DarkBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp)
            .testTag("chip_$name"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name,
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) DarkTealOnAccent else TextChipUnselected
        )
    }
}

/**
 * Dashed Segmented Level Meter:
 * height: 10px; background: repeating-linear-gradient(90deg,#2A3039 0 6px,transparent 6px 9px)
 * fill: height: 10px; width: 62%; background: repeating-linear-gradient(90deg,#2DD4BF 0 6px,transparent 6px 9px)
 */
@Composable
fun SegmentedLevelMeter(
    levelRatio: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val animatedRatio by animateFloatAsState(
        targetValue = levelRatio.coerceIn(0f, 1f),
        label = "meter_fill"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val totalWidth = size.width
        val dashWidth = 6.dp.toPx()
        val gapWidth = 3.dp.toPx()
        val step = dashWidth + gapWidth

        val fillWidth = totalWidth * animatedRatio

        var currentX = 0f
        while (currentX < totalWidth) {
            val drawWidth = (dashWidth).coerceAtMost(totalWidth - currentX)
            val isFilled = (currentX + drawWidth / 2f) <= fillWidth

            drawRect(
                color = if (isFilled) TealAccent else MeterTrack,
                topLeft = Offset(currentX, 0f),
                size = Size(drawWidth, size.height)
            )
            currentX += step
        }
    }
}

/**
 * Main Record Button from HTML:
 * outer: 64px x 64px, border-radius 50%, border 2px solid #E8EAED, background transparent
 * inner: 44px x 44px, border-radius 50%, background #EF4444
 */
@Composable
fun StudioRecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(2.dp, TextPrimary, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .testTag("record_button"),
            contentAlignment = Alignment.Center
        ) {
            if (!isRecording) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SignalRed)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SignalRed)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isRecording) "Tap to stop" else "Tap to record",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

/**
 * Segmented Control:
 * background: #0E1013; border: 1px solid #262B33; border-radius: 8px; padding: 2px
 * button: height: 28px; padding: 0 12px; border-radius: 6px; color: #9AA1AB; font: 500 12px
 * button.on: background: #2DD4BF; color: #06231F
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(DarkSegmentBg, RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) TealAccent else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp)
                    .testTag("seg_${option}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) DarkTealOnAccent else TextSecondary
                )
            }
        }
    }
}

/**
 * Toggle Switch from HTML:
 * width: 40px; height: 24px; border-radius: 12px; border: 0; background: #2DD4BF; position: relative
 * ::after { top: 3px; right: 3px; width: 18px; height: 18px; border-radius: 50%; background: #06231F }
 */
@Composable
fun DesignToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 18f else 2f,
        label = "switch_thumb"
    )

    Box(
        modifier = modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) TealAccent else DarkBorder)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) DarkTealOnAccent else TextSecondary)
        )
    }
}
