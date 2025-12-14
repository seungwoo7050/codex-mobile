package com.codexpong.mobile.ui.replay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/**
 * 리플레이 상세 화면.
 */
@Composable
fun ReplayDetailScreen(
    replayId: Long,
    viewModel: ReplayDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(replayId) {
        if (uiState.detail == null || uiState.replayId != replayId) {
            viewModel.load(replayId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_replay_detail, replayId),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (uiState.isLoading) {
            Text(text = stringResource(id = R.string.label_loading))
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            Button(onClick = { viewModel.load(replayId) }) {
                Text(text = stringResource(id = R.string.action_refresh))
            }
        }
        uiState.detail?.let { detail ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val summary = detail.summary
                    Text(text = stringResource(id = R.string.label_replay_checksum, detail.checksum ?: "-"))
                    Text(text = stringResource(id = R.string.label_replay_download_path, detail.downloadPath ?: "-"))
                    summary?.let {
                        Text(text = stringResource(id = R.string.label_match_type, it.matchType ?: "-"))
                        Text(text = stringResource(id = R.string.label_opponent, it.opponentNickname ?: ""))
                        Text(text = stringResource(id = R.string.label_score_summary, it.myScore ?: 0, it.opponentScore ?: 0))
                        it.createdAt?.let { created ->
                            Text(text = stringResource(id = R.string.label_created_at, created))
                        }
                        it.eventFormat?.let { format ->
                            Text(text = stringResource(id = R.string.label_event_format, format))
                        }
                    }
                }
            }
        }
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.action_back_to_replays))
        }
    }
}
