package com.codexpong.mobile.ui.job

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
import androidx.compose.material3.OutlinedTextField
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
import com.codexpong.mobile.data.job.model.JobResponse

/**
 * 잡 목록 화면.
 */
@Composable
fun JobListScreen(
    viewModel: JobListViewModel,
    onNavigateDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (uiState.items.isEmpty() && !uiState.isLoading && uiState.errorMessage == null) {
            viewModel.load()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_jobs),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = uiState.statusFilter,
                onValueChange = viewModel::updateStatusFilter,
                label = { Text(text = stringResource(id = R.string.label_job_status_filter)) }
            )
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = uiState.typeFilter,
                onValueChange = viewModel::updateTypeFilter,
                label = { Text(text = stringResource(id = R.string.label_job_type_filter)) }
            )
            Button(onClick = { viewModel.applyFilters() }) {
                Text(text = stringResource(id = R.string.action_apply_filters))
            }
        }
        if (uiState.isLoading) {
            Text(text = stringResource(id = R.string.label_loading))
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            Button(onClick = { viewModel.load() }) {
                Text(text = stringResource(id = R.string.action_refresh))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.items) { job ->
                JobItemCard(job = job, onClick = {
                    job.jobId?.let { onNavigateDetail(it) }
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
private fun JobItemCard(job: JobResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = stringResource(id = R.string.label_job_id, job.jobId ?: 0L), fontWeight = FontWeight.Bold)
            Text(text = stringResource(id = R.string.label_job_type, job.jobType ?: "-"))
            Text(text = stringResource(id = R.string.label_job_status, job.status ?: "-"))
            job.progress?.let { progress ->
                Text(text = stringResource(id = R.string.label_job_progress, progress))
            }
            job.targetReplayId?.let { replayId ->
                Text(text = stringResource(id = R.string.label_job_target_replay, replayId))
            }
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}
