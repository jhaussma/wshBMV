package de.wsh.wshbmv.ui.dialog

import de.wsh.wshbmv.db.entities.TbmvLager
import de.wsh.wshbmv.db.entities.relations.BelegAndZielort

interface AddDialogListener {
    fun onAddButtonClicked(clickItem: TbmvLager)
}