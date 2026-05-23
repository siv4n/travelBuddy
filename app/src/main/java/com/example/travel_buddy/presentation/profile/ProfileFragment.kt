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
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
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

        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.ivSettings.setOnClickListener {
            showSettingsMenu(it)
        }

        setupRecyclerView()
        setupListeners()
        observeStates()
        profileViewModel.loadProfile()
        binding.tabLayout.getTabAt(1)?.select()
    }

    private fun setupRecyclerView() {
        adapter = TripCardAdapter(
            trips = mutableListOf(),
            onTripClicked = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.postId)
                }
                findNavController().navigate(R.id.action_profileFragment_to_tripDetailFragment, bundle)
            },
            onLikeClicked = { post ->
                val current = adapter.getPostById(post.postId) ?: post
                val optimisticLiked = !current.isLiked
                val optimisticCount = if (optimisticLiked) {
                    current.likesCount + 1
                } else {
                    (current.likesCount - 1).coerceAtLeast(0)
                }
                adapter.setLikeState(post.postId, optimisticCount, optimisticLiked)

                lifecycleScope.launch {
                    when (val result = ServiceLocator.postRepository.toggleLike(post.postId)) {
                        is com.example.travel_buddy.core.common.AppResult.Success -> {
                            val serverLiked = result.data
                            if (serverLiked != optimisticLiked) {
                                val correctedCount = if (serverLiked) {
                                    current.likesCount + 1
                                } else {
                                    (current.likesCount - 1).coerceAtLeast(0)
                                }
                                adapter.setLikeState(post.postId, correctedCount, serverLiked)
                            }
                        }
                        is com.example.travel_buddy.core.common.AppResult.Error -> {
                            adapter.setLikeState(post.postId, current.likesCount, current.isLiked)
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        )
        binding.rvProfileTrips.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProfileTrips.adapter = adapter
    }

    private fun setupListeners() {
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

    private fun showSettingsMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.profile_settings_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_edit_profile -> {
                        findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
                        true
                    }
                    R.id.action_logout -> {
                        profileViewModel.logout()
                        true
                    }
                    else -> false
                }
            }
            setForceShowIcon(true)
            try {
                val field = javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val helper = field.get(this)
                helper.javaClass.getDeclaredMethod("setBackgroundDrawable", android.graphics.drawable.Drawable::class.java)
                    ?.invoke(helper, ContextCompat.getDrawable(requireContext(), R.drawable.bg_profile_menu_popup))
                helper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                    ?.invoke(helper, true)
            } catch (_: Exception) {
                // Popup icons are optional if the internal helper is unavailable.
            }
            show()
        }
    }

    private fun observeStates() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Listen for saved-post events from details screen
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_saved")
                    ?.observe(viewLifecycleOwner) { postId ->
                        // reload saved trips when a post save state changes
                        profileViewModel.loadSavedTrips()
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_saved")
                    }

                // Listen for like/unlike events from details screen
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Map<String, Any>>("post_liked")
                    ?.observe(viewLifecycleOwner) { data ->
                        val postId = data["postId"] as? String
                        val isLiked = data["isLiked"] as? Boolean
                        val likesCount = data["likesCount"] as? Int
                        if (postId != null && isLiked != null && likesCount != null) {
                            adapter.setLikeState(postId, likesCount, isLiked)
                        }
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<Map<String, Any>>("post_liked")
                    }

                // Listen for post edit events
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_edited")
                    ?.observe(viewLifecycleOwner) { postId ->
                        // Reload current tab (My Trips or Saved)
                        val currentTab = binding.tabLayout.selectedTabPosition
                        if (currentTab == 0) {
                            profileViewModel.loadMyTrips()
                        } else {
                            profileViewModel.loadSavedTrips()
                        }
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_edited")
                    }

                // Listen for post delete events
                findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>("post_deleted")
                    ?.observe(viewLifecycleOwner) { postId ->
                        // Reload current tab (My Trips or Saved)
                        val currentTab = binding.tabLayout.selectedTabPosition
                        if (currentTab == 0) {
                            profileViewModel.loadMyTrips()
                        } else {
                            profileViewModel.loadSavedTrips()
                        }
                        findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>("post_deleted")
                    }
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
