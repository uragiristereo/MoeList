package com.axiel7.moelist.data.model.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.axiel7.moelist.R
import com.axiel7.moelist.data.model.base.Localizable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WeekDay(
    val value: String,
    val numeric: Int
) : Localizable {
    @SerialName("monday")
    MONDAY(
        value = "monday",
        numeric = 1
    ),

    @SerialName("tuesday")
    TUESDAY(
        value  = "tuesday",
        numeric = 2
    ),

    @SerialName("wednesday")
    WEDNESDAY(
        value = "wednesday",
        numeric = 3
    ),

    @SerialName("thursday")
    THURSDAY(
        value = "thursday",
        numeric = 4
    ),

    @SerialName("friday")
    FRIDAY(
        value = "friday",
        numeric = 5
    ),

    @SerialName("saturday")
    SATURDAY(
        value = "saturday",
        numeric = 6
    ),

    @SerialName("sunday")
    SUNDAY(
        value = "sunday",
        numeric = 7
    );

    @Composable
    override fun localized() = when (this) {
        MONDAY -> stringResource(R.string.monday)
        TUESDAY -> stringResource(R.string.tuesday)
        WEDNESDAY -> stringResource(R.string.wednesday)
        THURSDAY -> stringResource(R.string.thursday)
        FRIDAY -> stringResource(R.string.friday)
        SATURDAY -> stringResource(R.string.saturday)
        SUNDAY -> stringResource(R.string.sunday)
    }
}