package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    sdfLocalDisplayDate: SimpleDateFormat
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(filteredMatches) { match ->
            val result = matchResults[match.id]
            
            val matchDateTime = try {
                sdfCDMX.parse("${match.date} ${match.time}")
            } catch (_: Exception) {
                null
            }
            
            val displayTime = matchDateTime?.let { sdfLocalTime.format(it) } ?: match.time
            val displayDate = matchDateTime?.let { sdfLocalDisplayDate.format(it) } ?: match.date

            InternalMatchItem(match, result, displayDate, displayTime)
        }
    }
}

@Composable
private fun InternalMatchItem(match: Match, prediction: MatchResult?, displayDate: String, displayTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${match.group} • $displayDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$displayTime hrs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.homeFlag, fontSize = 28.sp)
                    Text(
                        match.homeTeam,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        "VS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gold.copy(alpha = 0.7f)
                    )
                    Text(
                        "-", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(match.awayFlag, fontSize = 28.sp)
                    Text(
                        match.awayTeam,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Tu pronóstico",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val scoreText = if (prediction != null && prediction.homeScore.isNotEmpty() && prediction.awayScore.isNotEmpty()) {
                        "${prediction.homeScore} - ${prediction.awayScore}"
                    } else {
                        "-  -"
                    }
                    Text(
                        text = scoreText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Puntos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-", 
                        fontWeight = FontWeight.Bold,
                        color = Gold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
