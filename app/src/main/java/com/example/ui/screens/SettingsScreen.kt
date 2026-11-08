package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioViewModel
import com.example.ui.components.DesignToggleSwitch
import com.example.ui.components.SectionLabel
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 80.dp)
            .testTag("settings_screen")
    ) {
        // Header (.h: height 32px)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                fontFamily = InterFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = TextPrimary
            )
        }

        // Section: Quality
        SectionLabel(text = "Quality")

        // Quality Card (.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardBg)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        ) {
            // Row 1: Sample Rate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sample rate",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val sampleRateOptions = listOf("44.1", "48", "96")
                val sampleRateIndex = when (settings.sampleRate) {
                    44100 -> 0
                    96000 -> 2
                    else -> 1
                }
                SegmentedControl(
                    options = sampleRateOptions,
                    selectedIndex = sampleRateIndex,
                    onSelect = { idx ->
                        val targetRate = when (idx) {
                            0 -> 44100
                            2 -> 96000
                            else -> 48000
                        }
                        viewModel.updateSettings(settings.copy(sampleRate = targetRate))
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorder))

            // Row 2: Bit depth
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bit depth",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val bitDepthOptions = listOf("16", "24", "32")
                val bitDepthIndex = when (settings.bitDepth) {
                    16 -> 0
                    32 -> 2
                    else -> 1
                }
                SegmentedControl(
                    options = bitDepthOptions,
                    selectedIndex = bitDepthIndex,
                    onSelect = { idx ->
                        val targetDepth = when (idx) {
                            0 -> 16
                            2 -> 32
                            else -> 24
                        }
                        viewModel.updateSettings(settings.copy(bitDepth = targetDepth))
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorder))

            // Row 3: Format
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Format",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val formatOptions = listOf("WAV", "AAC", "MP3")
                val formatIndex = when (settings.audioFormat.uppercase()) {
                    "AAC" -> 1
                    "MP3" -> 2
                    else -> 0
                }
                SegmentedControl(
                    options = formatOptions,
                    selectedIndex = formatIndex,
                    onSelect = { idx ->
                        val targetFmt = when (idx) {
                            1 -> "AAC"
                            2 -> "MP3"
                            else -> "WAV"
                        }
                        viewModel.updateSettings(settings.copy(audioFormat = targetFmt))
                    }
                )
            }
        }

        // Section: Noise
        SectionLabel(text = "Noise")

        // Noise Card (.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardBg)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        ) {
            // Row 1: Noise reduction
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Noise reduction",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                val noiseOptions = listOf("Low", "Medium", "High")
                val noiseIndex = when (settings.noiseReductionLevel.lowercase()) {
                    "low" -> 0
                    "medium" -> 1
                    else -> 2
                }
                SegmentedControl(
                    options = noiseOptions,
                    selectedIndex = noiseIndex,
                    onSelect = { idx ->
                        val targetLevel = when (idx) {
                            0 -> "Low"
                            1 -> "Medium"
                            else -> "High"
                        }
                        viewModel.updateSettings(settings.copy(noiseReductionLevel = targetLevel))
                    }
                )
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorder))

            // Row 2: Adaptive noise gate
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Adaptive noise gate",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cuts room sound between words",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                DesignToggleSwitch(
                    checked = settings.adaptiveNoiseGate,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(adaptiveNoiseGate = it)) },
                    modifier = Modifier.testTag("switch_noise_gate")
                )
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorder))

            // Row 3: Voice enhance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice enhance",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Clarity, presence and warmth",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                DesignToggleSwitch(
                    checked = settings.voiceEnhance,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(voiceEnhance = it)) },
                    modifier = Modifier.testTag("switch_voice_enhance")
                )
            }

            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(DarkBorder))

            // Row 4: Silent input alert
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Silent input alert",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Warns if mic hears nothing",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                DesignToggleSwitch(
                    checked = settings.silentInputAlert,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(silentInputAlert = it)) },
                    modifier = Modifier.testTag("switch_silent_alert")
                )
            }
        }
    }
}
