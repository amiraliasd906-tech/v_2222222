package com.divarsmartsearch.app.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.divarsmartsearch.app.domain.model.Listing
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onReject: () -> Unit,
    onBlockPhoneNumber: (String) -> Unit = {},
    onViewSellerReport: (String) -> Unit = {},
    onAskAi: () -> Unit = {},
    onCall: (String) -> Unit = {},
    // When true, the second action button renders as "بازگردانی" (restore)
    // instead of "رد کردن" (reject) — used on the History screen's
    // Rejected tab, where [onReject] is wired to actually undo the reject.
    // Re-rejecting an already-rejected listing left the card sitting there
    // with no visible change, which looked exactly like a broken button.
    isRestoreAction: Boolean = false,
    // Hides the bookmark/save button entirely — used on the History
    // screen's Saved tab, where pressing Save again was a no-op (the
    // listing's status was already "saved", so nothing visibly changed)
    // and looked like a broken button. There's nothing useful for that
    // button to do on a card that's already saved, so it's hidden instead.
    showSaveButton: Boolean = true,
    // Overrides the reject button's label when it isn't acting as restore
    // (e.g. "حذف از ذخیره‌شده‌ها" on the Saved tab, vs plain "رد کردن"
    // elsewhere), so the icon's purpose matches the tab it's shown in.
    rejectContentDescription: String = "رد کردن",
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "cardPressScale",
    )

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .animateContentSize(animationSpec = tween(220)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.Top,
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                // Bug fix, per explicit user request: this badge used to
                // show listing.ownerProbability, but every visible listing
                // now has that value fixed at 1.0 (the old graded
                // AI/heuristic agency stage was removed from FilterPipeline
                // entirely) — the badge showed the exact same "مالک" label
                // on every single card with zero information value, so it
                // was removed. Whether a listing is shown at all is now
                // decided purely by the person's own keyword filters.
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                StarRating(rating = listing.starRating)
                if (listing.isDuplicate) {
                    DuplicateBadge(modifier = Modifier.padding(start = 8.dp))
                }
            }

            // Bug fix: listing.description was already being scraped and
            // stored (see ListingIngestionService / HeadlessDivarScanner)
            // but nothing in this card ever displayed it — only the title,
            // star rating, price, and area were shown. From the outside
            // that looks exactly like "the app never reads the
            // description", regardless of whether the underlying scraping
            // itself is working. A short, truncated snippet is enough here;
            // the full text is one tap away on Divar's own page.
            listing.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            // New: this was already being recorded in listing_interactions
            // every time a listing got rejected (see
            // FilterPipeline.recordRejection / ListingRepositoryImpl) but
            // had no way to reach the UI — only ever shown here (Rejected
            // tab cards carry it; Results/Seen/Saved cards never do).
            listing.rejectionReason?.let { reason ->
                Text(
                    text = "دلیل رد: $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = listing.price?.let { formatToman(it) } ?: "قیمت نامشخص",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                listing.area?.let {
                    Text(
                        text = "${it.toInt()} متر",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            listing.pricePerMeter?.let {
                Text(
                    text = "${formatToman(it)} / متر",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            listing.pricePerMeterVsAreaAveragePercent?.let { percent ->
                val cheaper = percent < 0
                Text(
                    text = if (cheaper) {
                        "٪${"%.0f".format(-percent)} ارزان‌تر از میانگین منطقه"
                    } else {
                        "٪${"%.0f".format(percent)} گران‌تر از میانگین منطقه"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cheaper) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (!listing.neighborhood.isNullOrBlank()) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    Text(
                        text = listing.neighborhood,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (listing.detectedPhoneNumbers.isNotEmpty()) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "شماره‌های یافت‌شده در متن آگهی:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (listing.phoneRepeatCount > 0) {
                        Text(
                            text = "این شماره در ${listing.phoneRepeatCount} آگهی دیگر هم دیده شده",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    listing.detectedPhoneNumbers.forEach { phoneNumber ->
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row {
                                    TextButton(onClick = { onCall(phoneNumber) }) {
                                        Icon(
                                            imageVector = Icons.Outlined.Call,
                                            contentDescription = "تماس",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                    TextButton(onClick = { onBlockPhoneNumber(phoneNumber) }) {
                                        Text(
                                            "مسدود کردن",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                            TextButton(onClick = { onViewSellerReport(phoneNumber) }) {
                                Text("گزارش این فروشنده", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(onClick = onAskAi) {
                    Icon(
                        imageVector = Icons.Outlined.SmartToy,
                        contentDescription = "پرسش از هوش مصنوعی",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onReject) {
                    if (isRestoreAction) {
                        Icon(
                            imageVector = Icons.Outlined.Restore,
                            contentDescription = "بازگردانی",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = rejectContentDescription,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (showSaveButton) {
                    IconButton(
                        onClick = onSave,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BookmarkBorder,
                            contentDescription = "ذخیره",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

private fun formatToman(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("fa", "IR"))
    return "${formatter.format(amount.toLong())} تومان"
}

@Composable
private fun StarRating(rating: Int) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DuplicateBadge(modifier: Modifier = Modifier) {
    var flipped by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clickable { flipped = !flipped }
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        if (!flipped) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = " احتمال آگهی تکراری",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        } else {
            Text(
                text = "بر اساس شباهت متن با آگهی‌های دیگر — لمس کنید تا برگردد",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
