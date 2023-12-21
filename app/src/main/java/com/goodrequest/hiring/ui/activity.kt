package com.goodrequest.hiring.ui

import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goodrequest.hiring.PokemonApi
import com.goodrequest.hiring.R
import com.goodrequest.hiring.databinding.ActivityBinding
import com.google.android.material.snackbar.Snackbar

class PokemonActivity: ComponentActivity(), OnItemClickListener {

    private var viewBinding: ActivityBinding? = null
    private val vm by viewModel { PokemonViewModel(it, null, PokemonApi) }
    private var adapter: PokemonAdapter? = null

    private var page: Int = 1
    private var position: Int = 0
    private var pokemons: ArrayList<Pokemon> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.load(page)

        ActivityBinding.inflate(layoutInflater).run {
            viewBinding = this
            setContentView(root)
            setRefresh()
            retry.setOnClickListener {
                loading.visibility = VISIBLE
                vm.load(page)
            }
        }

        //pagination
        setOnScrollListener()

        observePokemons()
    }

    private fun observePokemons() {
        vm.pokemons.observe(this@PokemonActivity) { result: Result<List<Pokemon>>? ->
            result?.fold(
                onSuccess = { pokemons ->
                    //refresh
                    if (viewBinding!!.refresh.isRefreshing ||
                        this.pokemons.contains(Pokemon("", ""))) {
                        viewBinding!!.refresh.isRefreshing = false
                        this.pokemons.clear()
                    }

                    //normal loading
                    viewBinding!!.loading.visibility = GONE
                    viewBinding!!.loadingPage.visibility = GONE

                    //set adapter
                    if (adapter == null) {
                        adapter = PokemonAdapter(this)
                        viewBinding!!.items.adapter = adapter
                    }
                    this.pokemons.addAll(pokemons)
                    adapter!!.show(this.pokemons)
                },
                onFailure = {
                    if (viewBinding!!.refresh.isRefreshing) {
                        //Refresh
                        Snackbar
                            .make(viewBinding!!.root, R.string.refresh_fails, Snackbar.LENGTH_SHORT)
                            .show()
                        viewBinding!!.refresh.isRefreshing = false
                    } else {
                        if (page == 1) {
                            //Init fail
                            viewBinding!!.loading.visibility = GONE
                            viewBinding!!.failure.visibility = VISIBLE
                        } else {
                            //Page fail
                            viewBinding!!.loadingPage.visibility = GONE
                            if (pokemons.contains(Pokemon("", "FAIL"))) {
                                //Another FAIL
                                pokemons.remove(Pokemon("", "FAIL"))
                                pokemons.add(Pokemon("", "ERROR"))
                            } else if (!pokemons.contains(Pokemon("", "ERROR"))) {
                                pokemons.add(Pokemon("", "FAIL"))
                            }
                            adapter!!.show(pokemons)
                        }
                    }
                }
            )
        }
    }

    override fun onButtonClick(position: Int) {
        //reaction on button clicked when page loading failed
        if (viewBinding!!.loadingPage.visibility == GONE) {
            viewBinding!!.loadingPage.visibility = VISIBLE
            vm.load(page)
        }
    }

    private fun setRefresh() {
        //two types of refresh visualization
        viewBinding!!.refresh.setOnRefreshListener {
            if (page > 1) {
                viewBinding!!.refresh.isRefreshing = false
                adapter!!.show(listOf(Pokemon("", "")))
            }
            pokemons.clear()
            page = 1
            vm.load(page)
        }
    }

    private fun setOnScrollListener() {
        viewBinding!!.items.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                //hitting end of RV list
                if (!recyclerView.canScrollVertically(1)) {
                    if (viewBinding!!.loadingPage.visibility != VISIBLE &&
                        !pokemons.contains(Pokemon("", "FAIL")) &&
                        !pokemons.contains(Pokemon("", "ERROR"))) {
                        page = (pokemons.size / 20) + 1
                        vm.load(page)
                        viewBinding!!.loadingPage.visibility = VISIBLE
                    }
                }
            }
        })
    }

    //rotation change state
    override fun onSaveInstanceState(state: Bundle) {
        super.onSaveInstanceState(state)
        val linearLayoutManager = (viewBinding?.items?.layoutManager) as LinearLayoutManager
        position = linearLayoutManager.findFirstVisibleItemPosition()
        state.putInt("Position", position)
    }

    //rotation change state
    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)
        position = state.getInt("Position")
        scrollToPosition(position)
    }

    //back to app
    override fun onResume() {
        super.onResume()
        scrollToPosition(position)
    }

    //scrolling for reset state
    private fun scrollToPosition(position: Int) {
        viewBinding?.items?.viewTreeObserver?.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                viewBinding?.items?.viewTreeObserver?.removeOnPreDrawListener(this)
                viewBinding?.items?.scrollToPosition(position)
                return true
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.pokemons.removeObservers(this)
    }
}

/**
 * Helper function that enables us to directly call constructor of our ViewModel but also
 * provides access to SavedStateHandle.
 * Shit like this is usually generated by Hilt
 */
inline fun <reified VM: ViewModel> ComponentActivity.viewModel(crossinline create: (SavedStateHandle) -> VM) =
    ViewModelLazy(
        viewModelClass = VM::class,
        storeProducer = { viewModelStore },
        factoryProducer = {
            object: AbstractSavedStateViewModelFactory(this@viewModel, null) {
                override fun <T : ViewModel> create(key: String, type: Class<T>, handle: SavedStateHandle): T =
                    create(handle) as T
            }
    })