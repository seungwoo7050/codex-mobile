package com.codexpong.mobile.ui.replay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codexpong.mobile.R
import com.codexpong.mobile.data.replay.model.ReplaySummaryResponse

/**
 * 리플레이 목록 화면.
 */
@Composable
fun ReplayListScreen(
    viewModel: ReplayListViewModel,
    onNavigateDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.items.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
            viewModel.loadPage(uiState.page)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_replays),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (uiState.isLoading) {
            Text(text = stringResource(id = R.string.label_loading))
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            Button(onClick = { viewModel.loadPage(uiState.page) }) {
                Text(text = stringResource(id = R.string.action_refresh))
            }
        }
        if (uiState.isEmpty && uiState.errorMessage == null && !uiState.isLoading) {
            Text(text = stringResource(id = R.string.label_empty_replays))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.items) { replay ->
                ReplayItemCard(replay = replay, onClick = {
                    replay.replayId?.let { onNavigateDetail(it) }
                })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.loadPreviousPage() },
                enabled = !uiState.isLoading && uiState.page > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.action_previous_page))
            }
            Button(
                onClick = { viewModel.loadNextPage() },
                enabled = !uiState.isLoading && (uiState.totalPages == 0 || uiState.page + 1 < uiState.totalPages),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(id = R.string.action_next_page))
            }
        }
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.action_back_to_profile))
        }
    }
}

@Composable
private fun ReplayItemCard(replay: ReplaySummaryResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "ID: ${replay.replayId ?: "-"}", fontWeight = FontWeight.Bold)
            Text(text = stringResource(id = R.string.label_match_type, replay.matchType ?: "-"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.label_score_you, replay.myScore ?: 0))
                Text(text = stringResource(id = R.string.label_score_opponent, replay.opponentScore ?: 0))
            }
            Text(text = stringResource(id = R.string.label_opponent, replay.opponentNickname ?: ""))
            replay.createdAt?.let {
                Text(text = stringResource(id = R.string.label_created_at, it))
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
