package com.example.travelapp_20214034_hero.ui.list

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.data.TravelDbHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TravelDeleteUndoHandler(
    private val fragment: Fragment,
    private val rootView: View,
    private val dbHelper: TravelDbHelper,
    private val onChanged: () -> Unit
) {

    fun delete(item: TravelItem) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val existing = withContext(Dispatchers.IO) {
                dbHelper.getTravelById(item.id)
            }
            withContext(Dispatchers.IO) {
                dbHelper.deleteTravel(item.id)
            }
            if (!fragment.isAdded) return@launch
            onChanged()
            showUndoSnackbar(
                onUndo = {
                    existing?.let { dbHelper.insertTravel(it.copy(id = 0)) }
                },
                onCommit = {
                    ImageFileHelper.deletePhotoFile(existing?.photoUri)
                }
            )
        }
    }

    fun deleteAll(sortDescending: Boolean) {
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val deletedItems = withContext(Dispatchers.IO) {
                dbHelper.getAllTravels(sortDescending)
            }
            withContext(Dispatchers.IO) {
                dbHelper.deleteAllTravels()
            }
            if (!fragment.isAdded) return@launch
            onChanged()
            showUndoSnackbar(
                onUndo = {
                    deletedItems.forEach { dbHelper.insertTravel(it.copy(id = 0)) }
                },
                onCommit = {
                    ImageFileHelper.deleteAllInternalPhotos(rootView.context)
                }
            )
        }
    }

    private fun showUndoSnackbar(
        onUndo: suspend () -> Unit,
        onCommit: () -> Unit
    ) {
        Snackbar.make(rootView, R.string.deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                fragment.viewLifecycleOwner.lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        onUndo()
                    }
                    if (!fragment.isAdded) return@launch
                    onChanged()
                }
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        onCommit()
                    }
                }
            })
            .show()
    }
}
