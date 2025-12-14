package com.codexpong.mobile.ui.job

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
 * 잡 상세 화면.
 */
@Composable
fun JobDetailScreen(
    jobId: Long,
    viewModel: JobDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(jobId) {
        if (uiState.job == null || uiState.jobId != jobId) {
            viewModel.load(jobId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.title_job_detail, jobId),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (uiState.isLoading) {
            Text(text = stringResource(id = R.string.label_loading))
        }
        if (!uiState.errorMessage.isNullOrBlank()) {
            Text(text = uiState.errorMessage ?: "", color = MaterialTheme.colorScheme.error)
            Button(onClick = { viewModel.load(jobId) }) {
                Text(text = stringResource(id = R.string.action_refresh))
            }
        }
        uiState.job?.let { job ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = stringResource(id = R.string.label_job_id, job.jobId ?: 0L), fontWeight = FontWeight.Bold)
                    Text(text = stringResource(id = R.string.label_job_type, job.jobType ?: "-"))
                    Text(text = stringResource(id = R.string.label_job_status, job.status ?: "-"))
                    job.progress?.let { progress ->
                        Text(text = stringResource(id = R.string.label_job_progress, progress))
                    }
                    job.targetReplayId?.let { target ->
                        Text(text = stringResource(id = R.string.label_job_target_replay, target))
                    }
                    job.createdAt?.let { created ->
                        Text(text = stringResource(id = R.string.label_job_created_at, created))
                    }
                    job.startedAt?.let { started ->
                        Text(text = stringResource(id = R.string.label_job_started_at, started))
                    }
                    job.endedAt?.let { ended ->
                        Text(text = stringResource(id = R.string.label_job_ended_at, ended))
                    }
                    job.errorCode?.let { code ->
                        Text(text = stringResource(id = R.string.label_job_error_code, code))
                    }
                    job.errorMessage?.let { msg ->
                        Text(text = stringResource(id = R.string.label_job_error_message, msg))
                    }
                    job.resultUri?.let { uri ->
                        Text(text = stringResource(id = R.string.label_job_result_uri, uri))
                    }
                    job.downloadUrl?.let { url ->
                        Text(text = stringResource(id = R.string.label_job_download_url, url))
                    }
                }
            }
        }
        Button(onClick = onNavigateBack, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.action_back_to_jobs))
        }
    }
}
