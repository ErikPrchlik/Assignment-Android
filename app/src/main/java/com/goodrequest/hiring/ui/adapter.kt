package com.goodrequest.hiring.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.goodrequest.hiring.R
import com.goodrequest.hiring.databinding.ItemBinding

interface OnItemClickListener {
    fun onButtonClick(position: Int)
}

class PokemonAdapter(private val onClickListener: OnItemClickListener): RecyclerView.Adapter<Item>() {
    private val items = ArrayList<Pokemon>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Item =
        Item(LayoutInflater.from(parent.context).inflate(R.layout.item, parent, false))

    override fun onBindViewHolder(holder: Item, position: Int) {
        holder.show(items[position])
        //refresh page click
        holder.itemButton.setOnClickListener {
            onClickListener.onButtonClick(position)
        }
    }


    override fun getItemCount(): Int =
        items.size

    fun show(pokemons: List<Pokemon>) {
        items.clear()
        items.addAll(pokemons)
        notifyDataSetChanged()
    }
}

class Item(view: View): RecyclerView.ViewHolder(view) {

    private val ui = ItemBinding.bind(view)
    val itemButton: Button = ui.refreshPage

    fun show(pokemon: Pokemon) {
        when {
            pokemon.id.isNotEmpty() -> {
                //normal
                ui.info.visibility = View.VISIBLE
                ui.image.load(pokemon.detail?.image) {
                    crossfade(true)
                    placeholder(R.drawable.ic_launcher_foreground)
                }
                ui.move.text = pokemon.detail?.move
                ui.weight.text = pokemon.detail?.weight.toString()
                ui.name.text = pokemon.name
            }
            pokemon.name.isEmpty() -> {
                //refresh
                ui.info.visibility = View.GONE
            }
            else -> {
                //page fail
                ui.info.visibility = View.GONE
                ui.progressBar.visibility = View.GONE
                if (pokemon.name == "ERROR") {
                    //refresh page forbidden
                    ui.refreshPage.visibility = View.GONE
                }
            }
        }
    }
}