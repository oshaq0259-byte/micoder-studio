package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.ExportAudioFormat
import com.example.model.RecordingEntity
import com.example.ui.components.SectionLabel
import com.example.ui.components.SegmentedControl
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkIconBox
import com.example.ui.theme.DarkTealOnAccent
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun ExportDownloadDialog(
    recording: RecordingEntity,
    isExporting: Boolean,
    onDismiss: () -> Unit,
    onShareToCloud: (ExportAudioFormat) -> Unit,
    onSaveToDownloads: (ExportAudioFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportAudioFormat.WAV) }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(14.dp)),
            color = DarkCardBg
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .testTag("export_dialog")
            ) {
                // Header
                Text(
                    text = "Export & Download",
                    fontFamily = InterFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${recording.title} (${recording.sampleRate / 1000} kHz / ${recording.bitDepth}-bit)",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                // Format Picker Section
                SectionLabel(text = "Target Format")

                val formatOptions = listOf("WAV", "AAC", "MP3")
                val formatIndex = when (selectedFormat) {
                    ExportAudioFormat.WAV -> 0
                    ExportAudioFormat.AAC -> 1
                    ExportAudioFormat.MP3 -> 2
                }

                SegmentedControl(
                    options = formatOptions,
                    selectedIndex = formatIndex,
                    onSelect = { idx ->
                        selectedFormat = when (idx) {
                            1 -> ExportAudioFormat.AAC
                            2 -> ExportAudioFormat.MP3
                            else -> ExportAudioFormat.WAV
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Format Description
                val desc = when (selectedFormat) {
                    ExportAudioFormat.WAV -> "Lossless 48 kHz PCM • Maximum studio audio fidelity"
                    ExportAudioFormat.AAC -> "High-Bitrate 256 kbps .m4a • Universal podcast & web"
                    ExportAudioFormat.MP3 -> "Compressed 192 kbps MP3 • Legacy & lightweight"
                }

                Text(
                    text = desc,
                    fontFamily = InterFontFamily,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // In-progress indicator
                if (isExporting) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = TealAccent,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Processing audio stream...",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = TealAccent
                        )
                    }
                } else {
                    // Action 1: Save to Public Downloads
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TealAccent)
                            .clickable { onSaveToDownloads(selectedFormat) }
                            .padding(horizontal = 14.dp)
                            .testTag("btn_save_downloads"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = DarkTealOnAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save to Downloads Folder",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkTealOnAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action 2: Share to Cloud & Apps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkIconBox)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { onShareToCloud(selectedFormat) }
                            .padding(horizontal = 14.dp)
                            .testTag("btn_share_cloud"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                tint = TealAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Share to Cloud & Apps",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(vertical = 6.dp, horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
