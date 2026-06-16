package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.beetik.quinielamalenkamexico2026.data.MatchRepository
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchScore
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.model.PinCategory
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CardsView(
    allMatches: List<Match>,
    dayMatches: List<Match>, 
    participants: List<Participant>,
    resultsMap: Map<String, MatchScore>, 
    scores: Map<String, Int>, 
    ranks: Map<String, Int>,
    confirmedIds: List<String>,
    pinnedParticipantCategories: SnapshotStateMap<String, Int>,
    pinnedParticipantIds: SnapshotStateList<String>,
    pinnedCategories: Map<Int, PinCategory>,
    availableDates: List<String>,
    selectedDate: String,
    datesWithSimulations: Set<String>,
    comparisonParticipantId: String?,
    showOnlyDayPoints: Boolean,
    showAddButton: Boolean,
    isLiveRanking: Boolean,
    onSimularMatch: (Match) -> Unit,
    onClearSimulation: (Match) -> Unit,
    onDateSelected: (String) -> Unit,
    onToggleDayPoints: (Boolean) -> Unit,
    onAddParticipant: () -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onToggleComparison: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val compP = participants.find { it.id == comparisonParticipantId }
    val scrollableParticipants = participants.filter { it.id != comparisonParticipantId }

    // Pre-calcular ganadores de grupo y su estado para las tarjetas
    val todayGroups = remember(dayMatches) { dayMatches.map { it.group }.distinct() }
    val currentWinners = remember(allMatches, resultsMap.size, resultsMap.values.toList(), todayGroups) {
        todayGroups.associateWith { getGroupWinner(it, allMatches, resultsMap) }
    }
    val groupPointsActive = remember(allMatches, resultsMap.size, isLiveRanking, todayGroups) {
        todayGroups.associateWith { gName ->
            val gM = allMatches.filter { it.group == gName }
            val hasResults = gM.any { it.id in resultsMap }
            val isFinished = gM.all { it.id in resultsMap }
            hasResults && (isFinished || isLiveRanking)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(bottom = 8.dp)) {
        // Selector de Fecha
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableDates) { date ->
                val isSelected = date == selectedDate
                val hasSimulation = datesWithSimulations.contains(date)
                
                // Nuevos estados para el selector
                val matchesOfDate = remember(allMatches, date) { allMatches.filter { it.date == date } }
                val hasLive = remember(matchesOfDate) { matchesOfDate.any { it.started && it.isActive } }
                val allFinished = remember(matchesOfDate) { matchesOfDate.isNotEmpty() && matchesOfDate.all { it.finished && !it.isActive } }

                Surface(
                    onClick = { onDateSelected(date) },
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isSelected -> Gold
                        allFinished -> Color(0xFF004D40) // Fondo verde si todo terminó
                        else -> Color(0xFF1E1E1E)
                    },
                    border = when {
                        isSelected -> null
                        hasLive -> BorderStroke(2.dp, Color(0xFFE91E63)) // Borde rosa si hay vivo
                        hasSimulation -> BorderStroke(2.dp, Color(0xFFFF9800)) // Borde naranja si hay sim
                        else -> BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                    }
                ) {
                    Text(
                        text = date,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        
        // Fila de Partidos
        LazyRow(
            modifier = Modifier.fillMaxWidth().height(205.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dayMatches, key = { it.id }) { match ->
                val actualInMap = resultsMap[match.id]
                val effectiveScore = actualInMap ?: if (match.started) match.realHomeScore?.let { h -> match.realAwayScore?.let { a -> MatchScore(h, a) } } else null
                
                val stats = remember(effectiveScore, participants) {
                    if (effectiveScore != null) {
                        val counts = mutableMapOf(2 to 0, 1 to 0, 0 to 0)
                        participants.forEach { p ->
                            val pred = p.predictions[match.id]
                            if (pred != null) {
                                val pts = calculatePoints(pred, effectiveScore)
                                counts[pts] = (counts[pts] ?: 0) + 1
                            } else {
                                counts[0] = (counts[0] ?: 0) + 1
                            }
                        }
                        counts
                    } else null
                }

                MatchCard(
                    match = match,
                    actualInMap = actualInMap,
                    isConfirmed = confirmedIds.contains(match.id),
                    onSimularMatch = { onSimularMatch(match) },
                    onClearSimulation = { onClearSimulation(match) },
                    points2 = stats?.get(2) ?: 0,
                    points1 = stats?.get(1) ?: 0,
                    points0 = stats?.get(0) ?: 0,
                    showStats = stats != null
                )
            }

            // Ganadores de los grupos del día
            item {
                Card(
                    modifier = Modifier.width(130.dp).fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
                    border = BorderStroke(1.dp, Gold.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.EmojiEvents, null, tint = Gold, modifier = Modifier.size(24.dp))
                        Text("LÍDERES HOY", color = Gold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        todayGroups.sorted().forEach { groupName ->
                            val winner = currentWinners[groupName]
                            val predCount = participants.count { it.groupWinnerPredictions[groupName] == winner?.first }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(groupName.replace("Grupo ", ""), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(winner?.second ?: "🏳️", fontSize = 20.sp)
                                    }
                                }
                                Text("($predCount)", color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Participantes y Pronósticos",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showOnlyDayPoints) "Solo hoy" else "Acumulado",
                    color = if (showOnlyDayPoints) Gold else Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Switch(
                    checked = showOnlyDayPoints,
                    onCheckedChange = onToggleDayPoints,
                    modifier = Modifier.scale(0.5f).height(20.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Gold,
                        checkedTrackColor = Gold.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }

        // Fila de Participantes
        val participantsLazyListState = rememberLazyListState()
        val isDragged by participantsLazyListState.interactionSource.collectIsDraggedAsState()
        var draggingId by remember { mutableStateOf<String?>(null) }
        val density = LocalDensity.current
        val addButtonWidthPx = with(density) { 130.dp.toPx() }

        LaunchedEffect(participants.size, showAddButton) {
            if (showAddButton) participantsLazyListState.scrollToItem(1)
            else participantsLazyListState.scrollToItem(0)
        }

        LaunchedEffect(participantsLazyListState.firstVisibleItemScrollOffset, participantsLazyListState.firstVisibleItemIndex, isDragged, showAddButton, draggingId) {
            if (!isDragged && showAddButton && draggingId == null) {
                val index = participantsLazyListState.firstVisibleItemIndex
                val offset = participantsLazyListState.firstVisibleItemScrollOffset.toFloat()
                if (index == 0 && offset < addButtonWidthPx * 0.42f) {
                    participantsLazyListState.animateScrollToItem(0)
                } else if (index == 0) {
                    participantsLazyListState.animateScrollToItem(1)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(280.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fija / Sticky Card
            compP?.let { p ->
                Box(modifier = Modifier.padding(start = 16.dp, end = 8.dp)) {
                    ParticipantFullCard(
                        p = p,
                        dayMatches = dayMatches,
                        resultsMap = resultsMap,
                        scores = scores,
                        ranks = ranks,
                        currentWinners = currentWinners,
                        groupPointsActive = groupPointsActive,
                        pinnedParticipantCategories = pinnedParticipantCategories,
                        pinnedParticipantIds = pinnedParticipantIds,
                        pinnedCategories = pinnedCategories,
                        scope = scope,
                        isComparison = true,
                        stickyCardPresent = true,
                        lazyListState = participantsLazyListState,
                        onRemoveParticipant = onRemoveParticipant,
                        onToggleComparison = onToggleComparison,
                        draggingId = draggingId,
                        onSetDraggingId = { draggingId = it }
                    )
                }
            }

            LazyRow(
                state = participantsLazyListState,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = if (compP == null) 16.dp else 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showAddButton) {
                    item {
                        AddParticipantCard(onAddParticipant)
                    }
                }
                items(scrollableParticipants, key = { it.id }) { p ->
                    ParticipantFullCard(
                        p = p,
                        dayMatches = dayMatches,
                        resultsMap = resultsMap,
                        scores = scores,
                        ranks = ranks,
                        currentWinners = currentWinners,
                        groupPointsActive = groupPointsActive,
                        pinnedParticipantCategories = pinnedParticipantCategories,
                        pinnedParticipantIds = pinnedParticipantIds,
                        pinnedCategories = pinnedCategories,
                        scope = scope,
                        isComparison = false,
                        stickyCardPresent = compP != null,
                        lazyListState = participantsLazyListState,
                        onRemoveParticipant = onRemoveParticipant,
                        onToggleComparison = onToggleComparison,
                        draggingId = draggingId,
                        onSetDraggingId = { draggingId = it }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    actualInMap: MatchScore?,
    isConfirmed: Boolean,
    onSimularMatch: () -> Unit,
    onClearSimulation: () -> Unit,
    points2: Int = 0,
    points1: Int = 0,
    points0: Int = 0,
    showStats: Boolean = false
) {
    val isLive = match.started && match.isActive
    val isFinished = match.finished
    val isSimulated = actualInMap != null && !isConfirmed
    
    val effectiveScore = actualInMap ?: if (match.started) match.realHomeScore?.let { h -> match.realAwayScore?.let { a -> MatchScore(h, a) } } else null

    val canSimulate = !isConfirmed && !isFinished

    val scoreColor = when {
        isConfirmed -> Color(0xFF4CAF50)
        isSimulated -> Color(0xFFFF9800)
        isLive -> Color(0xFFE91E63)
        else -> Color.White
    }

    Card(
        modifier = Modifier.width(130.dp).fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        border = when {
            isConfirmed -> BorderStroke(2.dp, Color(0xFF4CAF50))
            isSimulated -> BorderStroke(2.dp, Color(0xFFFF9800))
            isLive -> BorderStroke(2.dp, Color(0xFFE91E63))
            else -> null
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(match.group.uppercase(), color = Gold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                if (isLive) {
                    Surface(color = Color(0xFFE91E63), shape = RoundedCornerShape(2.dp)) {
                        Text("VIVO", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(match.homeFlag, fontSize = 24.sp)
                Text(" VS ", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(match.awayFlag, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = when {
                    isConfirmed -> "Finalizado"
                    isSimulated -> "Simulado"
                    isLive -> "En Vivo"
                    else -> "Programado"
                }, 
                color = Color.Gray, 
                fontSize = 8.sp
            )
            Text(effectiveScore?.let { "${it.home}-${it.away}" } ?: "-", color = scoreColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)

            if (showStats) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatLabel(2, points2)
                    StatLabel(1, points1)
                    StatLabel(0, points0)
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text("${match.time} hrs", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.weight(1f))
            
            if (isSimulated) {
                TextButton(
                    onClick = onClearSimulation,
                    modifier = Modifier.fillMaxWidth().height(28.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (isLive) "VOLVER A VIVO" else "BORRAR SIM",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gold
                    )
                }
            }

            Button(
                onClick = onSimularMatch, 
                enabled = canSimulate,
                modifier = Modifier.fillMaxWidth().height(28.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConfirmed || isFinished) Color.Gray else Color(0xFFFF9800),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color.Gray
                )
            ) { 
                Text(if (canSimulate) "Simular" else "Finalizado", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun StatLabel(points: Int, count: Int) {
    val color = getPointColor(points)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = count.toString(), color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddParticipantCard(onAddParticipant: () -> Unit) {
    Card(
        modifier = Modifier
            .width(130.dp)
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
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Add, null, tint = Gold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text("Cargar\nQuiniela", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ParticipantFullCard(
    p: Participant,
    dayMatches: List<Match>,
    resultsMap: Map<String, MatchScore>,
    scores: Map<String, Int>,
    ranks: Map<String, Int>,
    currentWinners: Map<String, Pair<String, String>?>,
    groupPointsActive: Map<String, Boolean>,
    pinnedParticipantCategories: SnapshotStateMap<String, Int>,
    pinnedParticipantIds: SnapshotStateList<String>,
    pinnedCategories: Map<Int, PinCategory>,
    scope: kotlinx.coroutines.CoroutineScope,
    isComparison: Boolean,
    stickyCardPresent: Boolean,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    onRemoveParticipant: (String) -> Unit,
    onToggleComparison: (String) -> Unit,
    draggingId: String?,
    onSetDraggingId: (String?) -> Unit
) {
    val categoryId = pinnedParticipantCategories[p.id]
    val isPinned = categoryId != null
    val categoryColor = if (isPinned) pinnedCategories[categoryId]?.color ?: Gold else Gold
    val isLoaded = p.id.startsWith("loaded_")
    
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    // Global position to detect edge proximity
    var cardPositionX by remember { mutableFloatStateOf(0f) }

    // Logic for auto-scroll during drag
    var autoScrollJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var lastFingerX by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .onGloballyPositioned { cardPositionX = it.positionInWindow().x }
            .offset { IntOffset(dragOffsetX.roundToInt(), offsetY.roundToInt()) }
            .zIndex(if (draggingId == p.id || offsetY != 0f) 10f else 1f)
            .graphicsLayer {
                if (draggingId == p.id) {
                    scaleX = 1.1f
                    scaleY = 1.1f
                    shadowElevation = 8f
                }
            }
            .pointerInput(p.id, isPinned, isComparison) {
                if (isPinned && !isComparison) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onSetDraggingId(p.id) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            lastFingerX = cardPositionX + change.position.x
                            
                            // Swap logic
                            val currentIdx = pinnedParticipantIds.indexOf(p.id)
                            if (currentIdx != -1) {
                                val step = with(density) { 150.dp.toPx() } // width (140) + spacing (10)
                                if (dragOffsetX > step / 2 && currentIdx < pinnedParticipantIds.size - 1) {
                                    val movedItem = pinnedParticipantIds.removeAt(currentIdx)
                                    pinnedParticipantIds.add(currentIdx + 1, movedItem)
                                    dragOffsetX -= step
                                } else if (dragOffsetX < -step / 2 && currentIdx > 0) {
                                    val movedItem = pinnedParticipantIds.removeAt(currentIdx)
                                    pinnedParticipantIds.add(currentIdx - 1, movedItem)
                                    dragOffsetX += step
                                }
                            }

                            // Auto-scroll logic based on global position (60px threshold)
                            val isNearRightEdge = lastFingerX > (screenWidthPx - 60f)
                            val stickyPaddingPx = if (stickyCardPresent) with(density) { (16 + 140 + 8).dp.toPx() } else 0f
                            val isNearLeftEdge = lastFingerX < (stickyPaddingPx + 60f)

                            if (isNearRightEdge && currentIdx < pinnedParticipantIds.size - 1) {
                                if (autoScrollJob == null || !autoScrollJob!!.isActive) {
                                    autoScrollJob = scope.launch {
                                        while (true) {
                                            val cIdx = pinnedParticipantIds.indexOf(p.id)
                                            if (lastFingerX > (screenWidthPx - 60f) && cIdx < pinnedParticipantIds.size - 1) {
                                                val scrollAmount = 15f
                                                lazyListState.scrollBy(scrollAmount)
                                                dragOffsetX -= scrollAmount
                                                delay(16)
                                            } else {
                                                break
                                            }
                                        }
                                    }
                                }
                            } else if (isNearLeftEdge && currentIdx > 0) {
                                if (autoScrollJob == null || !autoScrollJob!!.isActive) {
                                    autoScrollJob = scope.launch {
                                        while (true) {
                                            val cIdx = pinnedParticipantIds.indexOf(p.id)
                                            if (lastFingerX < (stickyPaddingPx + 60f) && cIdx > 0) {
                                                val scrollAmount = 15f
                                                lazyListState.scrollBy(-scrollAmount)
                                                dragOffsetX += scrollAmount
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
                        },
                        onDragEnd = { 
                            onSetDraggingId(null)
                            dragOffsetX = 0f
                            autoScrollJob?.cancel()
                        },
                        onDragCancel = { 
                            onSetDraggingId(null)
                            dragOffsetX = 0f
                            autoScrollJob?.cancel()
                        }
                    )
                }
            }
            .pointerInput(p.id, isPinned, isLoaded, isComparison) {
                var tapCount = 0
                var tapJob: kotlinx.coroutines.Job? = null
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                            val down = event.changes.first()
                            val up = waitForUpOrCancellation()
                            if (up != null && (up.position - down.position).getDistance() < 15f) {
                                tapCount++
                                tapJob?.cancel()
                                tapJob = scope.launch {
                                    delay(220)
                                    if (tapCount == 1 && isPinned) {
                                        pinnedParticipantCategories.remove(p.id)
                                        pinnedParticipantIds.remove(p.id)
                                    } else if (tapCount == 1 && (p.isUser || isLoaded)) {
                                        onToggleComparison(p.id)
                                    } else {
                                        when (tapCount) {
                                            1 -> {
                                                pinnedParticipantCategories[p.id] = 1
                                                pinnedParticipantIds.add(p.id)
                                            }
                                            2 -> { pinnedParticipantCategories[p.id] = 2; if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id) }
                                            3 -> { pinnedParticipantCategories[p.id] = 3; if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id) }
                                            4 -> { pinnedParticipantCategories[p.id] = 4; if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id) }
                                            5 -> { pinnedParticipantCategories[p.id] = 5; if (!pinnedParticipantIds.contains(p.id)) pinnedParticipantIds.add(p.id) }
                                        }
                                    }
                                    tapCount = 0
                                }
                            } else {
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
        border = if (p.isUser || isPinned) BorderStroke(1.5.dp, categoryColor) else if (isLoaded) BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // Posición y Nombre
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val r = ranks[p.id] ?: 1
                val bgColor = when {
                    r == 1 -> Gold
                    r == 2 -> Color(0xFFC0C0C0)
                    isLoaded -> Color(0xFF2196F3).copy(alpha = 0.5f)
                    else -> Color(0xFF333333)
                }
                Surface(color = bgColor, shape = CircleShape, modifier = Modifier.size(18.dp)) { 
                    Box(contentAlignment = Alignment.Center) { 
                        Text("$r", color = if (r <= 2 && !isLoaded) Color.Black else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold) 
                    } 
                }
                
                Text("${scores[p.id] ?: 0} pts", color = if (isLoaded) Color(0xFF2196F3) else Gold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Ganadores de Grupo para hoy
            val todayGroups = remember(dayMatches) { dayMatches.map { it.group }.distinct() }
            if (todayGroups.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    todayGroups.sorted().forEach { group ->
                        val predictedTeam = p.groupWinnerPredictions[group]
                        if (predictedTeam != null) {
                            val currentWinner = currentWinners[group]
                            val isActive = groupPointsActive[group] ?: false
                            val isCorrect = isActive && currentWinner?.first == predictedTeam
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(MatchRepository.getFlag(predictedTeam), fontSize = 12.sp)
                                if (isCorrect) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF4CAF50))
                                    )
                                } else if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(Color.Gray.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Box(
                modifier = Modifier
                    .size(36.dp)
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
            ) { Text(p.ownerName.first().toString(), fontSize = 16.sp, color = Color.White) }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            val textColor = if (p.isUser || isPinned) categoryColor else if (isLoaded) Color(0xFF2196F3) else Color.White
            Text(text = p.quinielaName, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = "(${p.ownerName})", color = textColor.copy(alpha = 0.7f), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1)

            Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.2f))

            // Pronósticos del día
            val predictionsScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(predictionsScrollState), 
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                dayMatches.forEach { match ->
                    val pred = p.predictions[match.id]
                    val actual = resultsMap[match.id]
                    val isLive = match.started && match.isActive
                    
                    val effectiveActual = actual ?: if (isLive) match.realHomeScore?.let { h -> match.realAwayScore?.let { a -> MatchScore(h, a) } } else null
                    
                    val pts = if (effectiveActual != null && pred != null) calculatePoints(pred, effectiveActual) else null
                    val pointColor = if (pts != null) getPointColor(pts) else Color.Gray
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${match.homeFlag}${match.awayFlag}", fontSize = 12.sp)
                        
                        if (pts != null) {
                            Surface(
                                color = pointColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "+$pts",
                                    color = pointColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            // Spacer to maintain alignment when there are no points yet
                            Box(modifier = Modifier.width(1.dp)) 
                        }

                        Text(
                            text = pred?.let { "${it.first}-${it.second}" } ?: "-",
                            color = if (pred != null) Gold else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (isPinned) {
                val catName = pinnedCategories[categoryId]?.name ?: "FIJADO"
                Text(catName.uppercase(), color = categoryColor, fontSize = 6.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
