package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CardsView(
    match: Match, 
    participants: List<Participant>, 
    resultsMap: Map<String, MatchScore>, 
    scores: Map<String, Int>, 
    ranks: Map<String, Int>,
    pinnedParticipantCategories: SnapshotStateMap<String, Int>,
    pinnedParticipantIds: SnapshotStateList<String>,
    pinnedCategories: Map<Int, PinCategory>,
    showAddButton: Boolean,
    isLiveRanking: Boolean,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit
) {
    val actual = resultsMap[match.id]
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val lazyListState = rememberLazyListState()
    val isDragged by lazyListState.interactionSource.collectIsDraggedAsState()
    val density = LocalDensity.current
    val addButtonWidthPx = with(density) { 140.dp.toPx() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(participants.size, showAddButton) {
        if (showAddButton) {
            lazyListState.scrollToItem(1)
        } else {
            lazyListState.scrollToItem(0)
        }
    }

    LaunchedEffect(lazyListState.firstVisibleItemScrollOffset, lazyListState.firstVisibleItemIndex, isDragged, showAddButton) {
        if (!isDragged && showAddButton) {
            val index = lazyListState.firstVisibleItemIndex
            val offset = lazyListState.firstVisibleItemScrollOffset.toFloat()
            
            if (index == 0) {
                if (offset < addButtonWidthPx * 0.42f) {
                    lazyListState.animateScrollToItem(0)
                } else {
                    lazyListState.animateScrollToItem(1)
                }
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Card(modifier = Modifier.width(120.dp).fillMaxHeight(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))) {
            Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("PARTIDO", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(match.homeFlag, fontSize = 24.sp); Text("VS", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(match.awayFlag, fontSize = 24.sp)
                Text("Resultado", color = Color.Gray, fontSize = 9.sp); Text(actual?.let{"${it.home}-${it.away}"}?:"-", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f)); Button(onClick = {}, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(0.dp)) { Text("Ver", fontSize = 10.sp) }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        LazyRow(state = lazyListState, modifier = Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showAddButton) {
                item {
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    val up = waitForUpOrCancellation()
                                    if (up != null) {
                                        onAddParticipant()
                                        up.consume()
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E).copy(alpha = 0.5f)), 
                        border = BorderStroke(1.dp, Gold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Add, null, tint = Gold, modifier = Modifier.size(32.dp))
                            Text("Cargar Quiniela", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            items(participants, key = { it.id }) { p ->
                val categoryId = pinnedParticipantCategories[p.id]
                val isPinned = categoryId != null
                val categoryColor = if (isPinned) pinnedCategories[categoryId]?.color ?: Gold else Gold
                
                val isDragging = draggingId == p.id
                val isLoaded = p.id.startsWith("loaded_")
                var offsetY by remember { mutableFloatStateOf(0f) }

                Card(
                    modifier = Modifier
                        .width(140.dp)
                        .fillMaxHeight()
                        .offset { IntOffset(0, offsetY.roundToInt()) }
                        .zIndex(if (isDragging || offsetY != 0f) 10f else 1f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffset else 0f
                            if (isDragging) {
                                scaleX = 1.1f
                                scaleY = 1.1f
                                shadowElevation = 12f
                                alpha = 0.8f
                            }
                        }
                        .pointerInput(p.id, isPinned, isLoaded) {
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
                                                delay(220) // Threshold for next tap
                                                if (!p.isUser && !isLoaded) {
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
                                            // Movement detected, reset sequence
                                            tapCount = 0
                                            tapJob?.cancel()
                                        }
                                    }
                                }
                            }
                        }
                        .pointerInput(p.id) {
                            if (isLoaded) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { change, dragAmount ->
                                        change.consume()
                                        offsetY += dragAmount
                                        if (kotlin.math.abs(offsetY) > 180f) {
                                            onRemoveParticipant(p.id)
                                        }
                                    },
                                    onDragEnd = { offsetY = 0f },
                                    onDragCancel = { offsetY = 0f }
                                )
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), 
                    border = if (p.isUser || isPinned) BorderStroke(2.dp, categoryColor) else if (isLoaded) BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f)) else null
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val r = ranks[p.id] ?: 1
                        val bgColor = when {
                            r == 1 -> Gold
                            r == 2 -> Color(0xFFC0C0C0)
                            isLoaded -> Color(0xFF2196F3).copy(alpha = 0.5f)
                            else -> Color(0xFF333333)
                        }
                        Surface(color = bgColor, shape = CircleShape, modifier = Modifier.size(20.dp).align(Alignment.Start)) { 
                            Box(contentAlignment = Alignment.Center) { 
                                Text("$r", color = if (r <= 2 && !isLoaded) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) 
                            } 
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPinned -> categoryColor.copy(alpha = 0.2f)
                                        p.isUser || isLoaded -> Color.Transparent
                                        else -> Color.Gray
                                    }
                                )
                                .let { if (isPinned) it.border(1.dp, categoryColor, CircleShape) else it }, 
                            contentAlignment = Alignment.Center
                        ) { Text(p.ownerName.first().toString(), fontSize = 18.sp, color = Color.White) }
                        
                        val textColor = if (p.isUser || isPinned) categoryColor else if (isLoaded) Color(0xFF2196F3) else Color.White
                        Text(
                            text = p.quinielaName, 
                            color = textColor, 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "(${p.ownerName})", 
                            color = textColor.copy(alpha = 0.8f), 
                            fontSize = 10.sp, 
                            textAlign = TextAlign.Center
                        )

                        if (isPinned) {
                            val catName = pinnedCategories[categoryId]?.name ?: "FIJADO"
                            Text(catName.uppercase(), color = categoryColor, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        if (isLoaded) Text("SIMULACIÓN", color = Color(0xFF2196F3), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(8.dp)); Text("${scores[p.id] ?: 0} pts", color = if (isLoaded) Color(0xFF2196F3) else Gold, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
