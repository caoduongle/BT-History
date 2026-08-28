package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.EventEntity
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoGreen
import com.example.ui.theme.BentoGreenBg
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPurpleLight
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoRed
import com.example.ui.theme.BentoRedBg
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoSurfaceVariant
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary
import com.example.util.LocationHelper
import com.example.util.TimeFormatter

@Composable
fun TimelineItem(
    event: EventEntity,
    deviceName: String,
    isFirst: Boolean,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isConnect = event.eventType.equals("CONNECT", ignoreCase = true)
    val color = if (isConnect) BentoGreen else BentoRed
    val bgColor = if (isConnect) BentoGreenBg else BentoRedBg
    val hasLocation = event.latitude != null && event.longitude != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("timeline_item_${event.id}"),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline Spine & Dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            // Top connecting line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(12.dp)
                    .background(if (isFirst) Color.Transparent else BentoOutline)
            )

            // Timeline Node Circle
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnect) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                    contentDescription = event.eventType,
                    tint = color,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Bottom connecting line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isLast) 20.dp else 120.dp)
                    .background(if (isLast) Color.Transparent else BentoOutline)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Bento Event Card
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(BentoSurfaceVariant)
                .border(
                    1.dp,
                    if (event.isUnexpectedDisconnect) BentoAmber.copy(alpha = 0.5f) else BentoOutline,
                    RoundedCornerShape(18.dp)
                )
                .padding(16.dp)
        ) {
            // Header Row: Event Type & Exact Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isConnect) "KẾT NỐI" else "NGẮT KẾT NỐI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            fontSize = 11.sp
                        )
                    }

                    if (event.isUnexpectedDisconnect) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Ngắt kết nối bất ngờ",
                            tint = BentoAmber,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = TimeFormatter.formatRelativeTime(event.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Full Date & Time
            Text(
                text = "⏰ ${TimeFormatter.formatFullDateTime(event.timestamp)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = BentoTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location Info
            if (hasLocation) {
                Column {
                    Text(
                        text = "📍 ${TimeFormatter.formatCoordinates(event.latitude, event.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = BentoPurpleLight,
                        fontSize = 12.sp
                    )

                    if (!event.locationAddress.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = event.locationAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = BentoTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    if (event.accuracy != null) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Độ chính xác GPS: ±${event.accuracy.toInt()}m",
                            style = MaterialTheme.typography.labelSmall,
                            color = BentoTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Open Maps Action Button in Bento Purple Style
                    OutlinedButton(
                        onClick = {
                            LocationHelper.openLocationInMap(
                                context = context,
                                latitude = event.latitude,
                                longitude = event.longitude,
                                label = "$deviceName (${if (isConnect) "Kết nối" else "Ngắt"})"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("map_button_${event.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = BentoSurface,
                            contentColor = BentoPurpleLight
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(BentoOutline)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Xem bản đồ",
                            tint = BentoPurpleLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Xem vị trí trên bản đồ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BentoPurpleLight
                        )
                    }
                }
            } else {
                Text(
                    text = "📍 Không lấy được vị trí GPS tại thời điểm này",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

