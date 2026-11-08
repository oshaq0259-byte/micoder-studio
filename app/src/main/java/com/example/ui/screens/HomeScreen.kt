package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.RecorderStatus
import com.example.model.MicProfile
import com.example.model.RecordingMode
import com.example.ui.StudioViewModel
import com.example.ui.components.DesignModeChip
import com.example.ui.components.MicSelectorItem
import com.example.ui.components.SectionLabel
import com.example.ui.components.SegmentedLevelMeter
import com.example.ui.components.StudioRecordButton
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorderState by viewModel.recorderState.collectAsState()
    val selectedMic by viewModel.selectedMic.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.startRecording()
        }
    }

    val isRecording = recorderState.status == RecorderStatus.RECORDING

    fun handleRecordClick() {
        if (isRecording) {
            viewModel.stopRecording()
        } else {
            val hasMicPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (hasMicPermission) {
                viewModel.startRecording()
            } else {
                permissionLauncher.launch(perms.toTypedArray())
            }
        }
    }

    val modeOptions = listOf(
        "Story Telling" to RecordingMode.STORY_TELLING,
        "Voice Record" to RecordingMode.VOICE_RECORD,
        "Voice Over" to RecordingMode.VOICE_OVER,
        "Podcast Talk" to RecordingMode.PODCAST_TALK,
        "Interview" to RecordingMode.INTERVIEW_MODE
    )

    // Calculate level ratio for the 10px segmented meter (-60dB to 0dB)
    val levelRatio = if (isRecording) {
        ((recorderState.peakDb + 60f) / 60f).coerceIn(0.08f, 1.0f)
    } else {
        0.0f
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 80.dp)
            .testTag("home_screen")
    ) {
        // Header (.h: height 32px)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Micoder Studio",
                fontFamily = InterFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = TextPrimary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusDotColor = when {
                    recorderState.isSilentWarning -> SignalRed
                    isRecording -> TealAccent
                    else -> TealAccent
                }
                val statusText = when {
                    recorderState.isSilentWarning -> "Silent input"
                    isRecording -> "Recording"
                    else -> "Input ok"
                }

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = statusText,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Section: Mic
        SectionLabel(text = "Mic")

        MicSelectorItem(
            profile = MicProfile.MIC_CLEAR,
            isSelected = selectedMic == MicProfile.MIC_CLEAR,
            onSelect = { viewModel.selectMic(MicProfile.MIC_CLEAR) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        MicSelectorItem(
            profile = MicProfile.MIC_STUDIO,
            isSelected = selectedMic == MicProfile.MIC_STUDIO,
            onSelect = { viewModel.selectMic(MicProfile.MIC_STUDIO) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        MicSelectorItem(
            profile = MicProfile.MIC_CRISP,
            isSelected = selectedMic == MicProfile.MIC_CRISP,
            onSelect = { viewModel.selectMic(MicProfile.MIC_CRISP) },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Section: Mode
        SectionLabel(text = "Mode")

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            modeOptions.forEach { (name, mode) ->
                DesignModeChip(
                    name = name,
                    isSelected = selectedMode == mode,
                    onSelect = { viewModel.selectMode(mode) }
                )
            }
        }

        // Level & Time Card (.card: margin-top: 20px; padding: 16px)
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardBg)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(16.dp)
                .testTag("recording_card")
        ) {
            // Timer and Spec Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = formatExactDuration(recorderState.elapsedMs),
                    fontFamily = InterFontFamily,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-1).sp,
                    color = TextPrimary
                )

                Text(
                    text = "${settings.sampleRate / 1000} kHz / ${settings.bitDepth}-bit",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Segmented Level Meter
            SegmentedLevelMeter(
                levelRatio = levelRatio,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // dB Scale Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("-60", "-30", "-12", "0 dB").forEach { label ->
                    Text(
                        text = label,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Record Button (.margin-top: 18px)
        Spacer(modifier = Modifier.height(18.dp))
        StudioRecordButton(
            isRecording = isRecording,
            onClick = { handleRecordClick() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

fun formatExactDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}
