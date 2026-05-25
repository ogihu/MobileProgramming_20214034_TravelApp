package com.example.travelapp_20214034_hero.ui.home

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.data.TravelItem
import com.example.travelapp_20214034_hero.databinding.ItemTravelBinding

class TravelAdapter(
    private val onItemClick: (TravelItem) -> Unit,
    private val onItemLongClick: (TravelItem) -> Unit
) : RecyclerView.Adapter<TravelViewHolder>() {

    private val items = mutableListOf<TravelItem>()

    fun submitList(newItems: List<TravelItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TravelViewHolder {
        val binding = ItemTravelBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TravelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TravelViewHolder, position: Int) {
        val item = items[position]
        holder.binding.textPlace.text = item.place
        holder.binding.textDate.text = item.visitDate
        holder.binding.textMemoPreview.text =
            if (item.memo.isBlank()) "" else item.memo

        val context = holder.itemView.context
        if (!item.photoUri.isNullOrBlank()) {
            Glide.with(context)
                .load(Uri.parse(item.photoUri))
                .centerCrop()
                .placeholder(R.drawable.bg_photo_placeholder)
                .into(holder.binding.imageThumbnail)
        } else {
            holder.binding.imageThumbnail.setImageResource(R.drawable.bg_photo_placeholder)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
