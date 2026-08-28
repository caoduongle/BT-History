package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BentoAmber
import com.example.ui.theme.BentoAmberBg
import com.example.ui.theme.BentoOutline
import com.example.ui.theme.BentoPurpleLight
import com.example.ui.theme.BentoPurplePrimary
import com.example.ui.theme.BentoSurface
import com.example.ui.theme.BentoTextMuted
import com.example.ui.theme.BentoTextPrimary
import com.example.ui.theme.BentoTextSecondary

@Composable
fun WarningBanner(
    modifier: Modifier = Modifier,
    isCollapsible: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BentoSurface)
            .border(1.dp, BentoOutline, RoundedCornerShape(24.dp))
            .padding(20.dp)
            .testTag("warning_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(BentoAmberBg)
                        .border(1.dp, BentoAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Lưu ý quan trọng",
                        tint = BentoAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LƯU Ý VẬN HÀNH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = BentoPurpleLight
                    )
                    Text(
                        text = "Đặc tính kết nối & GPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = BentoTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notice bullet 1
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BentoPurpleLight)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Chỉ ghi nhận khi thiết bị còn bật nguồn và trong phạm vi Bluetooth. Không thể định vị khi thiết bị đã tắt nguồn hoặc cạn pin (khác biệt với hệ sinh thái AirTag).",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Notice bullet 2
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BentoPurpleLight)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Độ chính xác vị trí phụ thuộc vào cảm biến GPS của điện thoại tại thời điểm quét và có thể có sai số khi ở trong nhà / tầng hầm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BentoTextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Database privacy pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BentoSurface.copy(alpha = 0.5f))
                    .border(1.dp, BentoOutline, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = BentoTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lưu trữ SQLite / Room an toàn & 100% trên thiết bị",
                        style = MaterialTheme.typography.labelSmall,
                        color = BentoTextMuted
                    )
                }
            }
        }
    }
}

