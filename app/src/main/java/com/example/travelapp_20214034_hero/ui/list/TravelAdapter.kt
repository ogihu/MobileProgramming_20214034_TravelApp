package com.example.travelapp_20214034_hero.ui.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.common.ImageFileHelper
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.ItemTravelBinding

class TravelAdapter(
    private val onItemClick: (TravelItem) -> Unit,
    private val onRegisterContextMenu: (View, Int) -> Unit,
    private val onUnregisterContextMenu: (View) -> Unit
) : ListAdapter<TravelItem, TravelViewHolder>(TravelDiffCallback()) {

    fun getItemAt(position: Int): TravelItem? = currentList.getOrNull(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val binding = ItemTravelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TravelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.textPlace.text = item.place
        holder.binding.textDate.text = item.visitDate
        holder.binding.textMemoPreview.text =
            if (item.memo.isBlank()) "" else item.memo

        val loadTarget = ImageFileHelper.resolveForGlide(item.photoUri)
        if (loadTarget != null) {
            Glide.with(holder.itemView)
                .load(loadTarget)
                .centerCrop()
                .placeholder(R.drawable.bg_photo_placeholder)
                .into(holder.binding.imageThumbnail)
        } else {
            Glide.with(holder.itemView).clear(holder.binding.imageThumbnail)
            holder.binding.imageThumbnail.setImageResource(R.drawable.bg_photo_placeholder)
        }

        holder.binding.iconHasLocation.visibility =
            if (item.latitude != null && item.longitude != null) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setTag(R.id.tag_list_position, position)
        onRegisterContextMenu(holder.itemView, position)
    }

    override fun onViewRecycled(holder: TravelViewHolder) {
        Glide.with(holder.itemView).clear(holder.binding.imageThumbnail)
        onUnregisterContextMenu(holder.itemView)
        super.onViewRecycled(holder)
    }
}
