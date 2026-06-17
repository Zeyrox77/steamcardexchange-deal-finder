package net.steamcardexchange.dealfinder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.steamcardexchange.dealfinder.data.GameDeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealFinderScreen(
    onOpenDeal: (String) -> Unit,
    viewModel: DealFinderViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Steam Card Deal Finder") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = viewModel::fetchDeals,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Fetching...")
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Fetch Top 50 Deals")
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = state.errorMessage ?: state.statusMessage,
                color = if (state.errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodyMedium
            )

            AnimatedVisibility(visible = state.isLoading) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = state.deals, key = { it.appId }) { deal ->
                    DealRow(deal = deal, onClick = { onOpenDeal(deal.url) })
                }
            }
        }
    }
}

@Composable
private fun DealRow(
    deal: GameDeal,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CreditBadge(credits = deal.totalCredits)

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${deal.cardsInSet} cards \u2022 ${deal.setsAvailable} sets available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open listing",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CreditBadge(credits: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(56.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$credits",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "credits",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 9.sp
            )
        }
    }
}
