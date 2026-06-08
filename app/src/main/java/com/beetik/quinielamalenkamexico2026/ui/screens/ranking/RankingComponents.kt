package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.geometry.Offset
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FilterDropdown(selected: String, dynamicCategories: List<String>, hasAdded: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(dynamicCategories, hasAdded) {
        val base = mutableListOf("Todas", "Top 10", "Top 5")
        if (hasAdded) base.add("Añadidas")
        base + dynamicCategories
    }
    
    Box {
        Row(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFF1E1E1E)).clickable { expanded = true }.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (selected == "Quinielas Añadidas") "Añadidas" else selected, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Gold, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF1E1E1E))) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option, fontSize = 12.sp, color = if(option == selected || (option == "Añadidas" && selected == "Quinielas Añadidas")) Gold else Color.White) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun ViewTabSmall(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(if (selected) Gold else Color.Transparent).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 2.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
fun LegendItem(pts: Int, label: String) { Row(verticalAlignment = Alignment.CenterVertically) { PointTagSmall(pts); Text(" $label", color = Color.Gray, fontSize = 8.sp) } }

@Composable
fun PointTagSmall(points: Int) {
    val color = getPointColor(points)
    Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(3.dp)) {
        Text("+$points", color = color, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp), fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PredictionCell(
    p: Participant,
    match: Match,
    actual: MatchScore?,
    matchHistoryRanks: Map<String, Map<String, Int>>
) {
    val pred = p.predictions[match.id] ?: (0 to 0)
    val pts = if (actual != null) calculatePoints(pred, actual) else 0
    
    val historicalRank = matchHistoryRanks[match.id]?.get(p.id)
    val cellBg = when (historicalRank) {
        1 -> Gold.copy(alpha = 0.15f)
        2 -> Color(0xFF2196F3).copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .width(80.dp)
            .background(cellBg)
            .padding(vertical = 2.dp), 
        horizontalArrangement = Arrangement.Center, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${pred.first}-${pred.second}", color = Color.White, fontSize = 11.sp)
        if (actual != null) {
            Spacer(modifier = Modifier.width(2.dp))
            PointTagSmall(pts)
        }
    }
}

@Composable
fun GroupWinnerPredictionCell(
    p: Participant,
    groupName: String,
    actualWinnerName: String?,
    isGroupFinished: Boolean
) {
    val teamName = p.groupWinnerPredictions[groupName] ?: "-"
    val flag = if (teamName == "-") "-" else MatchRepository.getFlag(teamName)
    val isCorrect = isGroupFinished && actualWinnerName != null && teamName == actualWinnerName
    
    Box(
        modifier = Modifier
            .width(80.dp)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = flag, fontSize = 16.sp)
            if (isCorrect) {
                Spacer(modifier = Modifier.width(2.dp))
                PointTagSmall(2)
            } else if (isGroupFinished && teamName != "-") {
                Spacer(modifier = Modifier.width(2.dp))
                PointTagSmall(0)
            }
        }
    }
}

@Composable
fun RankBadge(rank: Int, isLoaded: Boolean) {
    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) {
        val bgColor = when {
            rank == 1 -> Gold
            rank == 2 -> Color(0xFFC0C0C0)
            isLoaded -> Color(0xFF2196F3).copy(alpha = 0.5f)
            else -> Color(0xFF333333)
        }
        Surface(color = bgColor, shape = RoundedCornerShape(3.dp)) { 
            Text("${rank}°", color = if (rank <= 2 && !isLoaded) Color.Black else Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp), fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun ParticipantColumnHeader(
    p: Participant,
    isPinned: Boolean,
    categoryColor: Color,
    density: androidx.compose.ui.unit.Density,
    pinnedParticipantCategories: SnapshotStateMap<String, Int>,
    pinnedParticipantIds: SnapshotStateList<String>,
    isComparison: Boolean,
    onToggleComparison: (String) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    draggingId: String? = null,
    dragOffset: Float = 0f,
    offsetY: Float = 0f,
    isReorderable: Boolean = false,
    onSetDraggingId: (String?) -> Unit = {},
    onSetDragOffset: (Float) -> Unit = {},
    onSetOffsetY: (Float) -> Unit = {},
    scrollState: androidx.compose.foundation.ScrollState? = null,
    leftBoundPx: Float = 0f
) {
    val currentDragOffset by rememberUpdatedState(dragOffset)
    val currentOffsetY by rememberUpdatedState(offsetY)
    val scope = rememberCoroutineScope()
    
    var headerPositionX by remember { mutableFloatStateOf(0f) }
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    var autoScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastFingerX by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .width(80.dp)
            .onGloballyPositioned { headerPositionX = it.positionInWindow().x }
            .offset { IntOffset(0, currentOffsetY.roundToInt()) }
            .zIndex(if (draggingId == p.id || currentOffsetY != 0f) 10f else 1f)
            .graphicsLayer {
                translationX = if (draggingId == p.id) currentDragOffset else 0f
                if (draggingId == p.id) {
                    scaleX = 1.15f
                    scaleY = 1.15f
                    shadowElevation = 12f
                    alpha = 0.85f
                }
            }
            .pointerInput(p.id, isPinned, isComparison) {
                if (isPinned && !isComparison) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { 
                            onSetDraggingId(p.id)
                            onSetDragOffset(0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newDragOffset = currentDragOffset + dragAmount.x
                            onSetDragOffset(newDragOffset)
                            
                            val currentIdx = pinnedParticipantIds.indexOf(p.id)
                            if (currentIdx != -1) {
                                val step = with(density) { 80.dp.toPx() }
                                if (newDragOffset > step / 2 && currentIdx < pinnedParticipantIds.size - 1) {
                                    val movedItem = pinnedParticipantIds.removeAt(currentIdx)
                                    pinnedParticipantIds.add(currentIdx + 1, movedItem)
                                    onSetDragOffset(newDragOffset - step)
                                } else if (newDragOffset < -step / 2 && currentIdx > 0) {
                                    val movedItem = pinnedParticipantIds.removeAt(currentIdx)
                                    pinnedParticipantIds.add(currentIdx - 1, movedItem)
                                    onSetDragOffset(newDragOffset + step)
                                }
                            }

                            // Auto-scroll logic for Table
                            if (scrollState != null) {
                                lastFingerX = headerPositionX + change.position.x
                                val edgeThreshold = 60f 
                                if (lastFingerX > (screenWidthPx - edgeThreshold) && currentIdx < pinnedParticipantIds.size - 1) {
                                    if (autoScrollJob == null || !autoScrollJob!!.isActive) {
                                        autoScrollJob = scope.launch {
                                            while (true) {
                                                val cIdx = pinnedParticipantIds.indexOf(p.id)
                                                if (lastFingerX > (screenWidthPx - edgeThreshold) && cIdx < pinnedParticipantIds.size - 1) {
                                                    scrollState.scrollBy(15f)
                                                    // Note: dragOffset (translationX) is automatically handled by pointer system 
                                                    // but we might need to adjust it if the header anchor moves.
                                                    // In ColumnHeader, we use translationX.
                                                    onSetDragOffset(currentDragOffset + 15f)
                                                    delay(16)
                                                } else {
                                                    break
                                                }
                                            }
                                        }
                                    }
                                } else if (lastFingerX < (leftBoundPx + edgeThreshold) && currentIdx > 0) {
                                    if (autoScrollJob == null || !autoScrollJob!!.isActive) {
                                        autoScrollJob = scope.launch {
                                            while (true) {
                                                val cIdx = pinnedParticipantIds.indexOf(p.id)
                                                if (lastFingerX < (leftBoundPx + edgeThreshold) && cIdx > 0) {
                                                    scrollState.scrollBy(-15f)
                                                    onSetDragOffset(currentDragOffset - 15f)
                                                    delay(16)
                                                } else {
                                                    break
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    autoScrollJob?.cancel()
                                }
                            }
                        },
                        onDragEnd = { 
                            onSetDraggingId(null)
                            onSetDragOffset(0f)
                            autoScrollJob?.cancel()
                        },
                        onDragCancel = { 
                            onSetDraggingId(null)
                            onSetDragOffset(0f)
                            autoScrollJob?.cancel()
                        }
                    )
                }
            }
            .pointerInput(p.id, isPinned, isComparison) {
                var tapCount = 0
                var tapJob: kotlinx.coroutines.Job? = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            val down = event.changes.first()
                            
                            // Wait for release to confirm it's a tap and not a scroll
                            val up = waitForUpOrCancellation()
                            if (up != null && (up.position - down.position).getDistance() < 15f) {
                                tapCount++
                                tapJob?.cancel()
                                tapJob = scope.launch {
                                    delay(350) // Threshold for the next tap in the sequence
                                    if (p.isUser || p.id.startsWith("loaded_")) {
                                        if (tapCount == 1) onToggleComparison(p.id)
                                    } else {
                                        when (tapCount) {
                                            1 -> {
                                                if (isPinned) {
                                                    pinnedParticipantCategories.remove(p.id)
                                                    pinnedParticipantIds.remove(p.id)
                                                } else {
                                                    pinnedParticipantCategories[p.id] = 1
                                                    pinnedParticipantIds.add(p.id)
                                                }
                                            }
                                            2 -> {
                                                pinnedParticipantCategories[p.id] = 2
                                                if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id)
                                            }
                                            3 -> {
                                                pinnedParticipantCategories[p.id] = 3
                                                if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id)
                                            }
                                            4 -> {
                                                pinnedParticipantCategories[p.id] = 4
                                                if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id)
                                            }
                                            5 -> {
                                                pinnedParticipantCategories[p.id] = 5
                                                if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id)
                                            }
                                        }
                                    }
                                    tapCount = 0
                                }
                            } else {
                                // It was a scroll/drag
                                tapCount = 0
                                tapJob?.cancel()
                            }
                        }
                    }
                }
            }
            .pointerInput(p.id) {
                if (p.id.startsWith("loaded_")) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            onSetOffsetY(currentOffsetY + dragAmount)
                            if (kotlin.math.abs(currentOffsetY + dragAmount) > 150f) {
                                onRemoveParticipant(p.id)
                            }
                        },
                        onDragEnd = { onSetOffsetY(0f) },
                        onDragCancel = { onSetOffsetY(0f) }
                    )
                }
            }
            .pointerInput(p.id, isReorderable) {
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isComparison -> Gold.copy(alpha = 0.6f)
                        isPinned -> categoryColor.copy(alpha = 0.3f)
                        p.isUser || p.id.startsWith("loaded_") -> Color.Transparent
                        else -> Color.Gray
                    }
                ), 
            contentAlignment = Alignment.Center
        ) { 
            Text(p.ownerName.first().toString(), color = Color.White, fontSize = 10.sp) 
            if (isPinned || isComparison) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(if (isComparison) 2.dp else 1.dp, categoryColor, CircleShape)
                )
            }
        }
        val textColor = if (p.isUser || isComparison) Gold else if (isPinned) categoryColor else if (p.id.startsWith("loaded_")) Color(0xFF2196F3) else Color.White
        Text(
            text = p.quinielaName, 
            color = textColor, 
            fontSize = 9.sp, 
            textAlign = TextAlign.Center,
            fontWeight = if (isPinned || isComparison) FontWeight.Bold else FontWeight.Normal,
            lineHeight = 10.sp
        )
        Text(
            text = "(${p.ownerName})", 
            color = textColor.copy(alpha = 0.7f), 
            fontSize = 7.sp, 
            textAlign = TextAlign.Center,
            lineHeight = 8.sp
        )
    }
}
