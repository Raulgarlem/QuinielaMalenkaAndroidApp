package com.beetik.quinielamalenkamexico2026.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.beetik.quinielamalenkamexico2026.model.Match

@Composable
fun MatchCard(
    match: Match,
    homeScore: String,
    awayScore: String,
    onHomeScoreChange: (String) -> Unit,
    onAwayScoreChange: (String) -> Unit,
    onLastImeAction: (() -> Unit)? = null,
    isError: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    
    val formattedDate = remember(match.date) {
        val parts = match.date.split("-")
        if (parts.size == 3) {
            val day = parts[2].toInt().toString()
            val month = when (parts[1]) {
                "06" -> "Junio"
                "07" -> "Julio"
                else -> parts[1]
            }
            "$day de $month"
        } else {
            match.date
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isError) 2.dp else 1.dp,
            color = if (isError) {
                Color(0xFFFF9800) // Orange
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TeamDisplay(
                    flag = match.homeFlag,
                    teamName = match.homeTeam,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScoreInput(
                        value = homeScore,
                        onValueChange = onHomeScoreChange,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Next) }
                        )
                    )

                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ScoreInput(
                        value = awayScore,
                        onValueChange = onAwayScoreChange,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                if (onLastImeAction != null) {
                                    onLastImeAction()
                                    // Focus movement will be handled by the callback to avoid double jump
                                } else {
                                    focusManager.moveFocus(FocusDirection.Next)
                                }
                            }
                        )
                    )
                }

                TeamDisplay(
                    flag = match.awayFlag,
                    teamName = match.awayTeam,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }
        }
    }
}

@Composable
fun TeamDisplay(
    flag: String,
    teamName: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (!alignEnd) {
            Text(text = flag, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = teamName,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        if (alignEnd) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = flag, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun ScoreInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        keyboardType = KeyboardType.Number,
        imeAction = ImeAction.Next
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    LaunchedEffect(value) {
        if (value != textFieldValueState.text) {
            textFieldValueState = textFieldValueState.copy(text = value)
        }
    }

    BasicTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            if (newValue.text.length <= 2 && newValue.text.all { it.isDigit() }) {
                textFieldValueState = newValue
                if (newValue.text != value) {
                    onValueChange(newValue.text)
                }
            }
        },
        modifier = Modifier
            .width(45.dp)
            .height(45.dp)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    textFieldValueState = textFieldValueState.copy(
                        selection = TextRange(0, textFieldValueState.text.length)
                    )
                }
            }
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.extraSmall
            ),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                innerTextField()
            }
        }
    )
}
