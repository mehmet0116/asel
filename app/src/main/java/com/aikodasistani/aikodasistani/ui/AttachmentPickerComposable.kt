package com.aikodasistani.aikodasistani.ui

import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalIndication
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aikodasistani.aikodasistani.R

@Composable
fun AttachmentOptionCard(iconRes: Int, label: String, subtitle: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = interactionSource.collectIsPressedAsState().value
    val elevationDp = animateDpAsState(if (isPressed) 12.dp else 6.dp)
    val indication = LocalIndication.current

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevationDp.value),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics { /* Keep for accessibility grouping */ }
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    role = Role.Button,
                    onClick = onClick
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular icon background
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color = color.copy(alpha = 0.18f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

fun createAttachmentComposeView(context: Context, onCamera: () -> Unit, onGallery: () -> Unit, onFile: () -> Unit, onVideo: () -> Unit, onUrl: () -> Unit): ComposeView {
    val composeView = ComposeView(context)
    composeView.setContent {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {

            AttachmentOptionCard(
                iconRes = R.drawable.ic_camera,
                label = "Fotoğraf Çek",
                subtitle = "Kamera ile yeni fotoğraf çek",
                color = Color(0xFFB3E5FC),
                onClick = onCamera
            )

            AttachmentOptionCard(
                iconRes = R.drawable.ic_gallery,
                label = "Galeriden Seç",
                subtitle = "Galerideki fotoğrafları seç",
                color = Color(0xFFC8E6C9),
                onClick = onGallery
            )

            AttachmentOptionCard(
                iconRes = R.drawable.ic_file,
                label = "Dosya Seç",
                subtitle = "PDF, ZIP ve diğer dosyaları seç",
                color = Color(0xFFFFF9C4),
                onClick = onFile
            )

            AttachmentOptionCard(
                iconRes = R.drawable.ic_video,
                label = "Video Seç",
                subtitle = "Video dosyası seç ve analiz et",
                color = Color(0xFFF8BBD0),
                onClick = onVideo
            )

            AttachmentOptionCard(
                iconRes = R.drawable.ic_url,
                label = "URL'den İçerik Al",
                subtitle = "Web sitesinden içerik çek",
                color = Color(0xFFB2DFDB),
                onClick = onUrl
            )
        }
    }
    return composeView
}
