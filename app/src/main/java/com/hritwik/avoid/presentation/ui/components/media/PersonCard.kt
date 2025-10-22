package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem

@Composable
fun PersonCard(
    person: Person,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onClick: (Person) -> Unit = {}
) {
    Column(
        modifier = modifier.width(calculateRoundedValue(96).sdp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f/3f)
                .clickable (
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(person) }
                ),
            shape = RoundedCornerShape(calculateRoundedValue(12).sdp),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val imageUrl = remember(serverUrl, person.id, person.primaryImageTag) {
                    person.getImageUrl(serverUrl)
                }
                if (imageUrl != null) {
                    NetworkImage(
                        data = imageUrl,
                        contentDescription = person.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    EmptyItem(
                        image = R.drawable.person
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(top = calculateRoundedValue(4).sdp)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            person.role?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}