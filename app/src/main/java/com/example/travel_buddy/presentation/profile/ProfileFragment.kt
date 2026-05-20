package com.example.travel_buddy.presentation.profile

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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.load
import com.example.travel_buddy.R
import com.example.travel_buddy.data.model.Post
import com.example.travel_buddy.data.model.UserProfile
import com.example.travel_buddy.databinding.FragmentProfileBinding
import com.example.travel_buddy.di.ServiceLocator
import com.example.travel_buddy.presentation.common.AuthViewModelFactory
import com.example.travel_buddy.presentation.common.UiState
import com.example.travel_buddy.presentation.discovery.TripCardAdapter
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by viewModels { AuthViewModelFactory() }
    private lateinit var adapter: TripCardAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProfileBinding.bind(view)

        setupRecyclerView()
        setupListeners()
        observeStates()
        profileViewModel.loadProfile()
    }

    private fun setupRecyclerView() {
        adapter = TripCardAdapter(
            trips = emptyList(),
            onTripClicked = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_profileFragment_to_tripDetailFragment, bundle)
            },
            onLikeClicked = { post ->
                lifecycleScope.launch {
                    val result = ServiceLocator.postRepository.toggleLike(post.postId)
                    if (result is com.example.travel_buddy.core.common.AppResult.Success) {
                        val msg = if (result.data) "Liked!" else "Unliked!"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.rvProfileTrips.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.rvProfileTrips.adapter = adapter
    }

    private fun setupListeners() {
        binding.buttonEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.buttonLogoutText.setOnClickListener {
            profileViewModel.logout()
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    profileViewModel.loadMyTrips()
                } else {
                    profileViewModel.loadSavedTrips()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.profileState.collect { state ->
                        binding.progressProfile.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
                        when (state) {
                            is UiState.Success -> bindProfile(state.data)
                            is UiState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            else -> Unit
                        }
                    }
                }

                launch {
                    profileViewModel.statsState.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                binding.tvAdventuresCount.text = state.data.first.toString()
                                binding.tvLikesCount.text = state.data.second.toString()
                                binding.tvSavedCount.text = state.data.third.toString()
                            }
                            else -> Unit
                        }
                    }
                }

                launch {
                    profileViewModel.postsState.collect { state ->
                        binding.progressProfile.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE
                        when (state) {
                            is UiState.Success -> adapter.updateData(state.data)
                            is UiState.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            else -> Unit
                        }
                    }
                }

                launch {
                    profileViewModel.logoutState.collect { state ->
                        binding.buttonLogoutText.isEnabled = state !is UiState.Loading
                        when (state) {
                            is UiState.Success -> {
                                profileViewModel.clearLogoutState()
                                val intent = android.content.Intent(requireContext(), com.example.travel_buddy.LoginActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            }
                            is UiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                profileViewModel.clearLogoutState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun bindProfile(profile: UserProfile) {
        binding.textUsername.text = profile.username
        binding.textEmail.text = profile.email
        binding.imageProfile.load(profile.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
