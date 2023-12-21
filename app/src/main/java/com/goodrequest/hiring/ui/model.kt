package com.goodrequest.hiring.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goodrequest.hiring.PokemonApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.Serializable

class PokemonViewModel(
    state: SavedStateHandle,
    private val context: Context?,
    private val api: PokemonApi) : ViewModel() {

    val pokemons = state.getLiveData<Result<List<Pokemon>>?>("pokemons", null)

    fun load() {
        viewModelScope.launch {
            val result = api.getPokemons(page = 1)
            loadDetails(result)
        }
    }

    private fun loadDetails(pokemonsResult: Result<List<Pokemon>>) {
        viewModelScope.launch {
            pokemonsResult.onSuccess { pokemons ->
                pokemons.forEach {
                    async { api.getPokemonDetail(it) }.await().onSuccess { pokemonDetail->
                        it.detail = pokemonDetail
                    }
                }
            }
        }.invokeOnCompletion {
            pokemons.postValue(pokemonsResult)
        }
    }
}

data class Pokemon(
    val id     : String,
    val name   : String,
    var detail : PokemonDetail? = null): Serializable

data class PokemonDetail(
    val image  : String,
    val move   : String,
    val weight : Int)