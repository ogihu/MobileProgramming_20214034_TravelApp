package com.example.travelapp_20214034_hero.ui.list

import androidx.recyclerview.widget.DiffUtil
import com.example.travelapp_20214034_hero.data.TravelItem

class TravelDiffCallback : DiffUtil.ItemCallback<TravelItem>() {
    override fun areItemsTheSame(oldItem: TravelItem, newItem: TravelItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: TravelItem, newItem: TravelItem): Boolean =
        oldItem == newItem
}
