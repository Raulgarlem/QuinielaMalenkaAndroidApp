package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.Match
import com.beetik.quinielamalenkamexico2026.model.MatchResult
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MatchesListView(
    filteredMatches: List<Match>,
    matchResults: Map<String, MatchResult>,
    sdfCDMX: SimpleDateFormat,
    sdfLocalTime: SimpleDateFormat,
    sdfLocalDisplayDate: SimpleDateFormat,
    headerContent: (@Composable () -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val listState = rememberLazyListState()

    // Auto-scroll logic: Live first, then next upcoming
    LaunchedEffect(filteredMatches) {
        if (filteredMatches.isNotEmpty()) {
            val liveIndex = filteredMatches.indexOfFirst { it.started && it.isActive }
            val nextUpcomingIndex = filteredMatches.indexOfFirst { !it.finished }
            
            val targetIndex = when {
                liveIndex != -1 -> liveIndex
                nextUpcomingIndex != -1 -> nextUpcomingIndex
                else -> -1
            }

            if (targetIndex != -1) {
                // Offset by 1 if there's a header
                val finalIndex = if (headerContent != null) targetIndex + 1 else targetIndex
                listState.animateScrollToItem(finalIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (headerContent != null) {
            item {
                headerContent()
            }
        }
        
        items(filteredMatches, key = { it.id }) { match ->
            val result = matchResults[match.id]
            
            // Pre-calculate date/time and points outside the complex item if possible, 
            // or just ensure the item itself is efficient.
            val matchDateTime = remember(match.date, match.time) {
                try {
                    sdfCDMX.parse("${match.date} ${match.time}")
                } catch (_: Exception) {
                    null
                }
            }
            
            val displayTime = remember(matchDateTime) { matchDateTime?.let { sdfLocalTime.format(it) } ?: match.time }
            val displayDate = remember(matchDateTime) { matchDateTime?.let { sdfLocalDisplayDate.format(it) } ?: match.date }

            val points = remember(match, result) {
                if (match.realHomeScore != null && match.realAwayScore != null && result != null) {
                    val hP = result.homeScore.toIntOrNull()
                    val aP = result.awayScore.toIntOrNull()
                    if (hP != null && aP != null) {
                        val actualScore = com.beetik.quinielamalenkamexico2026.model.MatchScore(match.realHomeScore, match.realAwayScore)
                        calculatePoints(hP to aP, actualScore).toString()
                    } else "-"
                } else "-"
            }

            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                InternalMatchItem(match, result, displayDate, displayTime, points, isLandscape)
            }
        }
    }
}

@Composable
private fun InternalMatchItem(
    match: Match, 
    prediction: MatchResult?, 
    displayDate: String, 
    displayTime: String,
    points: String,
    isLandscape: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(if (isLandscape) 8.dp else 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLive = match.started && match.isActive
                val isFinished = match.finished && !match.isActive
                
                Text(
                    text = "${match.group} • $displayDate",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isLandscape) 9.sp else 11.sp),
                    color = when {
                        isLive -> Color(0xFFE91E63)
                        isFinished -> Color(0xFF4CAF50)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                
                if (isLive) {
                    Surface(
                        color = Color(0xFFE91E63).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "EN VIVO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isLandscape) 8.sp else 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color(0xFFE91E63),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else if (isFinished) {
                    Surface(
                        color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "FINALIZADO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = if (isLandscape) 8.sp else 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                } else {
                    Text(
                        text = "$displayTime hrs",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isLandscape) 9.sp else 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.homeFlag, fontSize = if (isLandscape) 22.sp else 28.sp)
                    Text(
                        match.homeTeam,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isLandscape) 10.sp else 12.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    val isLive = match.started && match.isActive
                    val isFinished = match.finished && !match.isActive
                    
                    if (isLive || isFinished) {
                        Surface(
                            color = if (isLive) Color(0xFFE91E63).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (isLive) "EN VIVO" else "FINALIZADO",
                                style = if (isLandscape) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLive) Color(0xFFE91E63) else Color(0xFF4CAF50),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "VS",
                            style = if (isLandscape) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gold.copy(alpha = 0.7f)
                        )
                    }
                    
                    val realScoreText = if (match.started && match.realHomeScore != null && match.realAwayScore != null) {
                        "${match.realHomeScore} - ${match.realAwayScore}"
                    } else {
                        "-"
                    }
                    Text(
                        text = realScoreText,
                        style = if (isLandscape) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLive -> Color(0xFFE91E63)
                            isFinished -> Color(0xFF4CAF50)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.awayFlag, fontSize = if (isLandscape) 22.sp else 28.sp)
                    Text(
                        match.awayTeam,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isLandscape) 10.sp else 12.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = if (isLandscape) 6.dp else 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tu pronóstico: ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isLandscape) 9.sp else 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val scoreText = if (prediction != null && prediction.homeScore.isNotEmpty() && prediction.awayScore.isNotEmpty()) {
                        "${prediction.homeScore} - ${prediction.awayScore}"
                    } else {
                        "-  -"
                    }
                    val isLive = match.started && match.isActive
                    val isFinished = match.finished && !match.isActive
                    
                    Text(
                        text = scoreText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isLandscape) 11.sp else 14.sp),
                        color = if (isLive || isFinished) Color(0xFF4CAF50).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Puntos: ",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (isLandscape) 9.sp else 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val isLive = match.started && match.isActive
                    val isFinished = match.finished && !match.isActive
                    
                    Text(
                        text = points,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isLive -> Color(0xFFE91E63)
                            isFinished -> Color(0xFF4CAF50)
                            else -> Gold
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = if (isLandscape) 11.sp else 14.sp)
                    )
                }
            }
        }
    }
}
