package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

@Composable
fun TableView(
    matches: List<Match>, 
    participants: List<Participant>, 
    resultsMap: Map<String, MatchScore>,
    confirmedIds: List<String>,
    scores: Map<String, Int>,
    ranks: Map<String, Int>,
    matchHistoryRanks: Map<String, Map<String, Int>>,
    pinnedParticipantCategories: SnapshotStateMap<String, Int>,
    pinnedParticipantIds: SnapshotStateList<String>,
    pinnedCategories: Map<Int, PinCategory>,
    comparisonParticipantId: String?,
    showAddButton: Boolean,
    isLiveRanking: Boolean,
    onEditResult: (Match) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onToggleComparison: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val isDragged by scrollState.interactionSource.collectIsDraggedAsState()
    val density = LocalDensity.current
    val addButtonWidthPx = with(density) { 100.dp.toPx() }

    val compP = participants.find { it.id == comparisonParticipantId }
    val scrollableParticipants = participants.filter { it.id != comparisonParticipantId }

    LaunchedEffect(participants.size, showAddButton) {
        if (showAddButton) {
            scrollState.scrollTo(addButtonWidthPx.toInt())
        } else {
            scrollState.scrollTo(0)
        }
    }

    LaunchedEffect(scrollState.value, isDragged, showAddButton) {
        if (!isDragged && showAddButton) {
            val current = scrollState.value.toFloat()
            if (current < addButtonWidthPx) {
                if (current < addButtonWidthPx * 0.3f) {
                    scrollState.animateScrollTo(0)
                } else {
                    scrollState.animateScrollTo(addButtonWidthPx.toInt())
                }
            }
        }
    }

    val lastMatchIdsByGroup = remember(matches) { matches.groupBy { it.group }.mapValues { it.value.last().id }.values.toSet() }
    
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.background(Color(0xFF121212)).padding(vertical = 4.dp)) {
            Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) { Text("Partido", color = Color.Gray, fontSize = 10.sp) }
            Box(modifier = Modifier.width(60.dp), contentAlignment = Alignment.Center) { Text("Resultado", color = Color.Gray, fontSize = 10.sp) }
            
            compP?.let { p ->
                ParticipantColumnHeader(
                    p = p, 
                    isPinned = false, 
                    categoryColor = Gold,
                    density = density, 
                    pinnedParticipantCategories = pinnedParticipantCategories,
                    pinnedParticipantIds = pinnedParticipantIds,
                    isComparison = true,
                    onToggleComparison = onToggleComparison,
                    onRemoveParticipant = onRemoveParticipant
                )
            }

            Row(
                modifier = Modifier.horizontalScroll(scrollState)
            ) {
                    if (showAddButton) {
                        Column(
                            modifier = Modifier
                                .width(100.dp)
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
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.Gray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, null, tint = Gold, modifier = Modifier.size(16.dp))
                            }
                            Text("Añadir", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    scrollableParticipants.forEach { p ->
                        key(p.id) {
                            val categoryId = pinnedParticipantCategories[p.id]
                            val isPinned = categoryId != null
                            val categoryColor = if (isPinned) pinnedCategories[categoryId]?.color ?: Gold else Gold
                            val isReorderable = isPinned
                            var offsetY by remember { mutableFloatStateOf(0f) }
                            
                            val fixedPartWidth = 150.dp + (if (compP != null) 80.dp else 0.dp)
                            val leftBoundPx = with(density) { fixedPartWidth.toPx() }

                            ParticipantColumnHeader(
                                p = p,
                                isPinned = isPinned,
                                categoryColor = categoryColor,
                                draggingId = draggingId,
                                dragOffset = dragOffset,
                                offsetY = offsetY,
                                isReorderable = isReorderable,
                                density = density,
                                pinnedParticipantCategories = pinnedParticipantCategories,
                                pinnedParticipantIds = pinnedParticipantIds,
                                isComparison = false,
                                onSetDraggingId = { draggingId = it },
                                onSetDragOffset = { dragOffset = it },
                                onSetOffsetY = { offsetY = it },
                                onToggleComparison = onToggleComparison,
                                onRemoveParticipant = onRemoveParticipant,
                                scrollState = scrollState,
                                leftBoundPx = leftBoundPx
                            )
                        }
                    }
                }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            matches.forEach { match ->
                item(key = match.id) {
                    val actual = getEffectiveScore(match, resultsMap)
                    val isConfirmed = confirmedIds.contains(match.id)
                    val isLive = match.started && match.isActive
                    val isSimulated = resultsMap.containsKey(match.id) && !isConfirmed
                    
                    // Log para depurar color en UI
                    if (isLive) {
                        Log.d("TableView", "Match ${match.id} IS LIVE. started=${match.started}, isActive=${match.isActive}")
                    }

                    val canEdit = !isConfirmed && !match.finished

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp).background(Color(0xFF121212).copy(alpha = 0.5f)), verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.width(90.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text(match.homeFlag, fontSize = 14.sp); Text(" vs ", color = Color.Gray, fontSize = 10.sp); Text(match.awayFlag, fontSize = 14.sp)
                        }
                        Box(modifier = Modifier.width(60.dp).clickable(enabled = canEdit) { onEditResult(match) }, contentAlignment = Alignment.Center) {
                            Surface(
                                color = when { 
                                    isConfirmed -> Color(0xFF004D40)
                                    isSimulated -> Color(0xFFFF9800).copy(alpha = 0.2f)
                                    isLive -> Color(0xFFE91E63).copy(alpha = 0.2f)
                                    else -> Color(0xFF333333) 
                                }, 
                                shape = RoundedCornerShape(4.dp), 
                                border = when {
                                    isSimulated -> BorderStroke(1.dp, Color(0xFFFF9800))
                                    isLive -> BorderStroke(1.dp, Color(0xFFE91E63))
                                    else -> null
                                }
                            ) {
                                Text(
                                    text = actual?.let { "${it.home}-${it.away}" } ?: "-", 
                                    color = when { 
                                        isConfirmed -> Color(0xFF4CAF50)
                                        isSimulated -> Color(0xFFFF9800)
                                        isLive -> Color(0xFFE91E63)
                                        else -> Color.White 
                                    }, 
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 11.sp
                                )
                            }
                        }

                        compP?.let { p ->
                            PredictionCell(p, match, actual, matchHistoryRanks)
                        }

                        Row(modifier = Modifier.horizontalScroll(scrollState)) {
                            if (showAddButton) {
                                Box(modifier = Modifier.width(100.dp))
                            }
                            scrollableParticipants.forEach { p ->
                                PredictionCell(p, match, actual, matchHistoryRanks)
                            }
                        }
                    }
                }
                if (lastMatchIdsByGroup.contains(match.id)) {
                    item(key = "winner_${match.group}") {
                        val actualWinner = remember(resultsMap.size, resultsMap.values.toList(), match.group) { getGroupWinner(match.group, matches, resultsMap) }
                        val groupMatches = matches.filter { it.group == match.group }
                        val isGroupFinished = groupMatches.all { it.id in resultsMap }
                        val showWinnerResult = isGroupFinished || isLiveRanking

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).background(Gold.copy(alpha = 0.1f)), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp)); Text(actualWinner?.second ?: "🏳️", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp)); Text("Ganador ${match.group}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            compP?.let { p ->
                                GroupWinnerPredictionCell(p, match.group, actualWinner?.first, showWinnerResult)
                            }

                            Row(modifier = Modifier.horizontalScroll(scrollState)) { 
                                if (showAddButton) {
                                    Box(modifier = Modifier.width(100.dp))
                                }
                                scrollableParticipants.forEach { p ->
                                    GroupWinnerPredictionCell(p, match.group, actualWinner?.first, showWinnerResult)
                                }
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.background(Color(0xFF121212))) {
            Row(modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) { Text("TOTAL PUNTOS", color = Color.Gray, fontSize = if (isLandscape) 8.sp else 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                
                compP?.let { p ->
                    Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) { Text((scores[p.id] ?: 0).toString(), color = Color.White, fontSize = if (isLandscape) 11.sp else 12.sp, fontWeight = FontWeight.Bold) }
                }

                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    if (showAddButton) {
                        Box(modifier = Modifier.width(100.dp))
                    }
                    scrollableParticipants.forEach { p ->
                        Box(modifier = Modifier.width(80.dp), contentAlignment = Alignment.Center) { Text((scores[p.id] ?: 0).toString(), color = Color.White, fontSize = if (isLandscape) 11.sp else 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Row(modifier = Modifier.padding(vertical = if (isLandscape) 0.dp else 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) { Text("POSICIÓN", color = Color.Gray, fontSize = if (isLandscape) 8.sp else 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp)) }
                
                compP?.let { p ->
                    RankBadge(ranks[p.id] ?: 1, p.id.startsWith("loaded_"))
                }

                Row(modifier = Modifier.horizontalScroll(scrollState)) {
                    if (showAddButton) {
                        Box(modifier = Modifier.width(100.dp))
                    }
                    scrollableParticipants.forEach { p ->
                        RankBadge(ranks[p.id] ?: 1, p.id.startsWith("loaded_"))
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = if (isLandscape) 1.dp else 2.dp), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
            LegendItem(2, "Marcador Exacto"); LegendItem(2, "Ganador Grupo"); LegendItem(1, "Resultado Correcto"); LegendItem(0, "Resultado Incorrecto")
        }
    }
}
