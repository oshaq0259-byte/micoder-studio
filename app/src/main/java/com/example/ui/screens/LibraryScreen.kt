package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.model.MicProfile
import com.example.model.RecordingEntity
import com.example.model.RecordingMode
import com.example.ui.StudioViewModel
import com.example.ui.dialogs.ExportDownloadDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCardBg
import com.example.ui.theme.DarkIconBox
import com.example.ui.theme.DarkSegmentBg
import com.example.ui.theme.DarkTealOnAccent
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.SignalRed
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TextChipUnselected
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LibraryScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val recordings by viewModel.recordings.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val renameTarget by viewModel.renameTarget.collectAsState()
    val deleteTarget by viewModel.deleteTarget.collectAsState()
    val exportTarget by viewModel.exportTarget.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var renameText by remember { mutableStateOf("") }

    val filteredRecordings = remember(recordings, searchQuery) {
        if (searchQuery.isBlank()) {
            recordings
        } else {
            recordings.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 80.dp)
            .testTag("library_screen")
    ) {
        // Header (.h: height 32px)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recordings",
                fontFamily = InterFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = TextPrimary
            )

            // Search Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkCardBg)
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .clickable { isSearchActive = !isSearchActive }
                    .testTag("search_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = "Search",
                    tint = TextChipUnselected,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Search Input Bar
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter recordings...", fontFamily = InterFontFamily, fontSize = 12.sp, color = TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealAccent,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = TealAccent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(50.dp)
                    .testTag("search_input")
            )
        }

        // Active Playback Deck (When a track is playing)
        AnimatedVisibility(visible = playbackState.currentTrack != null) {
            playbackState.currentTrack?.let { currentTrack ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardBg)
                        .border(1.dp, TealAccent, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack.title,
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1
                            )
                            val mic = MicProfile.fromId(currentTrack.micProfileId)
                            val mode = RecordingMode.fromId(currentTrack.modeProfileId)
                            Text(
                                text = "${mic.displayName} / ${mode.displayName}",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Speed Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                                .background(DarkSegmentBg)
                                .clickable {
                                    val nextSpeed = when (playbackState.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        else -> 1.0f
                                    }
                                    viewModel.setPlaybackSpeed(nextSpeed)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${playbackState.playbackSpeed}x",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TealAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scrubber
                    val maxPos = playbackState.durationMs.coerceAtLeast(1L).toFloat()
                    val curPos = playbackState.currentPositionMs.coerceIn(0L, playbackState.durationMs).toFloat()
                    Slider(
                        value = curPos,
                        onValueChange = { viewModel.seekTo(it.toLong()) },
                        valueRange = 0f..maxPos,
                        colors = SliderDefaults.colors(
                            thumbColor = TealAccent,
                            activeTrackColor = TealAccent,
                            inactiveTrackColor = DarkBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(20.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTimeSimple(playbackState.currentPositionMs),
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = formatTimeSimple(playbackState.durationMs),
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Recordings Card
        if (recordings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkIconBox),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No recordings yet",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Start recording in the Studio tab",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DarkCardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(filteredRecordings, key = { it.id }) { item ->
                        val isPlaying = playbackState.currentTrack?.id == item.id && playbackState.isPlaying
                        val isSelected = playbackState.currentTrack?.id == item.id

                        LibraryRowItem(
                            item = item,
                            isPlaying = isPlaying,
                            isSelected = isSelected,
                            onPlayClick = { viewModel.playTrack(item) },
                            onRenameClick = {
                                renameText = item.title
                                viewModel.requestRename(item)
                            },
                            onExportClick = { viewModel.requestExport(item) },
                            onDeleteClick = { viewModel.requestDelete(item) }
                        )
                    }
                }
            }
        }
    }

    // Export & Download Dialog
    exportTarget?.let { target ->
        ExportDownloadDialog(
            recording = target,
            isExporting = isExporting,
            onDismiss = { viewModel.dismissExport() },
            onShareToCloud = { format -> viewModel.exportAndShare(target, format) },
            onSaveToDownloads = { format -> viewModel.exportAndDownload(target, format) }
        )
    }

    // Rename Dialog
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRename() },
            title = {
                Text(
                    text = "Rename Recording",
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = TealAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmRename(renameText) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Save", color = DarkTealOnAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissRename() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCardBg
        )
    }

    // Delete Confirmation Dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDelete() },
            title = {
                Text(
                    text = "Delete Recording?",
                    fontFamily = InterFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SignalRed
                )
            },
            text = {
                Text(
                    text = "Delete '${target.title}'? This permanently removes the audio file from the device.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalRed)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissDelete() }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCardBg
        )
    }
}

@Composable
fun LibraryRowItem(
    item: RecordingEntity,
    isPlaying: Boolean,
    isSelected: Boolean,
    onPlayClick: () -> Unit,
    onRenameClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val mic = MicProfile.fromId(item.micProfileId)
    val mode = RecordingMode.fromId(item.modeProfileId)
    val relativeDate = formatRelativeDate(item.createdAt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 0.5.dp,
                color = DarkBorder
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("row_${item.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Button: 36x36px circle, bg #20252C, icon #2DD4BF
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkIconBox)
                    .clickable(onClick = onPlayClick)
                    .testTag("play_btn_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TealAccent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${mic.displayName} / ${mode.displayName}",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            // Right: Duration & Date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTimeSimple(item.durationMs),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = relativeDate,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Action Toolbar (Download & Share Export, Rename, Delete)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download to phone button
            IconButton(
                onClick = onExportClick,
                modifier = Modifier.size(28.dp).testTag("download_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = "Download to phone",
                    tint = TealAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Share to cloud button
            IconButton(
                onClick = onExportClick,
                modifier = Modifier.size(28.dp).testTag("share_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudUpload,
                    contentDescription = "Share to Cloud",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Rename button
            IconButton(
                onClick = onRenameClick,
                modifier = Modifier.size(28.dp).testTag("rename_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Rename",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(28.dp).testTag("delete_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = SignalRed,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

fun formatTimeSimple(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val oneDay = 24 * 3600 * 1000L
    return when {
        diff < oneDay -> "Today"
        diff < 2 * oneDay -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
