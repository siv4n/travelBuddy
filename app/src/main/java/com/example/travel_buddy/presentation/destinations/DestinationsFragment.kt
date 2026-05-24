package com.example.travel_buddy.presentation.destinations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travel_buddy.databinding.FragmentDestinationsBinding
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.presentation.common.UiState
import kotlinx.coroutines.launch

class DestinationsFragment : Fragment() {

    private var _binding: FragmentDestinationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DestinationsViewModel by viewModels {
        DestinationsViewModelFactory(ServiceLocator.destinationRepository)
    }

    private lateinit var adapter: DestinationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDestinationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeStates()
        viewModel.loadDestinations()
    }

    private fun setupRecyclerView() {
        adapter = DestinationAdapter(emptyList()) { destination ->
            Toast.makeText(
                requireContext(),
                "Clicked: ${destination.name}",
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.rvDestinations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDestinations.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            viewModel.loadDestinations(forceRefresh = true)
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destinationsState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.rvDestinations.visibility = View.GONE
                            binding.tvError.visibility = View.GONE
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvDestinations.visibility = View.VISIBLE
                            binding.tvError.visibility = View.GONE
                            adapter.updateData(state.data)
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.rvDestinations.visibility = View.GONE
                            binding.tvError.visibility = View.VISIBLE
                            binding.tvError.text = state.message
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
