package com.auramusic.util

import android.os.Build

val isAtLeastR: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
val isAtLeastQ: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
val isAtLeastT: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
val isAtLeastP: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
val isAtLeastM: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
