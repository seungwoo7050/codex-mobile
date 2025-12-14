package com.codexpong.mobile.ui.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.codexpong.mobile.R

/**
 * 헬스 체크 결과와 설정 버튼을 보여주는 화면.
 */
@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    onOpenSettings: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.title_health),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "${stringResource(id = R.string.label_base_url)}: ${uiState.baseUrl}")
                Text(text = stringResource(id = R.string.label_health_status), fontWeight = FontWeight.SemiBold)
                when {
                    uiState.isLoading -> Text(text = stringResource(id = R.string.label_loading))
                    uiState.errorMessage != null -> Text(text = "${stringResource(id = R.string.label_error)}: ${uiState.errorMessage}")
                    uiState.healthText.isNotBlank() -> Text(text = uiState.healthText)
                    else -> Text(text = "응답 없음")
                }
            }
        }
        Button(onClick = { viewModel.refreshHealth() }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.action_refresh))
        }
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
            Text(text = stringResource(id = R.string.title_settings))
        }
        Button(onClick = onNavigateProfile, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
            Text(text = stringResource(id = R.string.action_back_to_profile))
        }
    }
}
