package com.beetik.quinielamalenkamexico2026.ui.screens.ranking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beetik.quinielamalenkamexico2026.model.Participant
import com.beetik.quinielamalenkamexico2026.ui.theme.Gold

@Composable
fun GlobalRankingView(
    participants: List<Participant>,
    scores: Map<String, Int>,
    ranks: Map<String, Int>
) {
    val sortedParticipants = participants.sortedBy { ranks[it.id] ?: 999 }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("RANKING GLOBAL", color = Gold, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sortedParticipants) { p ->
                val rank = ranks[p.id] ?: 1
                val score = scores[p.id] ?: 0
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when(rank) {
                                1 -> Gold
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> Color(0xFF333333)
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("$rank", color = if (rank <= 3) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.quinielaName, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(p.ownerName, color = Color.Gray, fontSize = 12.sp)
                        }
                        
                        Text("$score pts", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
