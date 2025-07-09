package io.music_assistant.client.ui.compose.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowCircleDown
import compose.icons.fontawesomeicons.solid.ArrowCircleLeft
import compose.icons.fontawesomeicons.solid.ArrowCircleRight
import compose.icons.fontawesomeicons.solid.ArrowCircleUp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HorizontalPagerIndicator(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onItemMoved: (Int) -> Unit,
) {
    val pageCount = pagerState.pageCount
    Row(
        modifier = modifier.fillMaxWidth().wrapContentHeight(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (pagerState.currentPage > 0) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onItemMoved(-1) },
                imageVector = FontAwesomeIcons.Solid.ArrowCircleLeft,
                tint = MaterialTheme.colors.secondary,
                contentDescription = null,
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pageCount <= 10) {
                repeat(pageCount) { index ->
                    val color =
                        if (pagerState.currentPage == index) MaterialTheme.colors.primary
                        else MaterialTheme.colors.primary.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(4.dp)
                    )
                }
            } else {
                Text(
                    text = "${pagerState.currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (pagerState.currentPage < pageCount - 1) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onItemMoved(1) },
                imageVector = FontAwesomeIcons.Solid.ArrowCircleRight,
                tint = MaterialTheme.colors.secondary,
                contentDescription = null,
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}

@Composable
fun VerticalPagerIndicator(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onItemMoved: (Int) -> Unit,
) {
    val pageCount = pagerState.pageCount
    Column(
        modifier = modifier.wrapContentWidth().fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (pagerState.currentPage > 0) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onItemMoved(-1) },
                imageVector = FontAwesomeIcons.Solid.ArrowCircleUp,
                tint = MaterialTheme.colors.secondary,
                contentDescription = null,
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (pageCount <= 10) {
                repeat(pageCount) { index ->
                    val color =
                        if (pagerState.currentPage == index) MaterialTheme.colors.primary
                        else MaterialTheme.colors.primary.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(4.dp)
                    )
                }
            } else {
                Text(
                    text = "${pagerState.currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        if (pagerState.currentPage < pageCount - 1) {
            Icon(
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onItemMoved(1) },
                imageVector = FontAwesomeIcons.Solid.ArrowCircleDown,
                tint = MaterialTheme.colors.secondary,
                contentDescription = null,
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview
@Composable
fun HorizontalPagerIndicatorPreview() {
    MaterialTheme {
        HorizontalPagerIndicator(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            pagerState = rememberPagerState (pageCount = {5}, initialPage = 2),
            onItemMoved = {}
        )
    }
}

@Preview
@Composable
fun VerticalPagerIndicatorPreview() {
    MaterialTheme {
        VerticalPagerIndicator(
            modifier = Modifier.fillMaxHeight().wrapContentWidth(),
            pagerState = rememberPagerState (pageCount = {5}, initialPage = 2),
            onItemMoved = {}
        )
    }
}