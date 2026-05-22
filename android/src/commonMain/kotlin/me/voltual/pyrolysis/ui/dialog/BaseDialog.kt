/*
 * This file is adapted from Neo Store (https://github.com/NeoApplications/Neo-Store)
 * Modified by Voltual to fit Pyrolysis architecture.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 */

package me.voltual.pyrolysis.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun BaseDialog(
    openDialogCustom: MutableState<Boolean>,
    dialogUI: @Composable (() -> Unit),
) {
    Dialog(
        onDismissRequest = { openDialogCustom.value = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        dialogUI()
    }
}