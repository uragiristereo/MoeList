package com.axiel7.moelist.uicompose.details.composables

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.axiel7.moelist.R
import com.axiel7.moelist.uicompose.theme.MoeListTheme

@Composable
fun MediaInfoView(
    title: String,
    info: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .then(modifier)
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = info ?: stringResource(R.string.unknown),
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MediaInfoPreview() {
    MoeListTheme {
        MediaInfoView(
            title = "Title",
            info = "Hello this is a large anime title do you like it?"
        )
    }
}