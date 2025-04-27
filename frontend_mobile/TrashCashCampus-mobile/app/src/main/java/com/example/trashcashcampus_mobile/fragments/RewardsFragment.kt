package com.example.trashcashcampus_mobile.fragments

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trashcashcampus_mobile.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.HashMap

class RewardsFragment : Fragment() {
    private val TAG = "RewardsFragment"
    
    // UI elements
    private lateinit var recyclerRewards: RecyclerView
    private lateinit var tvUserPoints: TextView
    private lateinit var tvEmptyState: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var categoryButtons: Map<String, Button>
    
    // Data
    private val db = Firebase.firestore
    private var userPoints = 0
    private var activeCategory = "all"
    private var rewards = listOf<Reward>()
    
    // User ID and listener
    private var userId = ""
    private var userPointsListener: ListenerRegistration? = null

    // Reward data class
    data class Reward(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val pointsCost: Int = 0,
        val category: String = "",
        val icon: String = "🎁",
        val backgroundColor: String = "#f0f0f0"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_rewards, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        initializeUI(view)
        
        // Get user ID from shared preferences or Firebase Auth
        getUserId()
        
        // Set up category buttons
        setupCategoryButtons()
        
        // Load data
        setupUserPointsListener()
        
        // First, log all rewards to see the actual categories in Firestore
        logAllRewardsInDatabase()
        
        // Load rewards directly from Firestore
        loadRewards(activeCategory)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the user points listener when fragment is destroyed
        userPointsListener?.remove()
    }
    
    private fun getUserId() {
        // Try to get user ID from SharedPreferences first
        val prefs = requireActivity().getSharedPreferences("TrashCashPrefs", Context.MODE_PRIVATE)
        userId = prefs.getString("userId", "") ?: ""
        
        // If not found, try Firebase Auth
        if (userId.isEmpty()) {
            userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        }
        
        if (userId.isEmpty()) {
            Log.e(TAG, "User ID is empty - unable to load user points")
        } else {
            Log.d(TAG, "Using user ID: $userId")
        }
    }
    
    private fun initializeUI(view: View) {
        recyclerRewards = view.findViewById(R.id.recycler_rewards)
        tvUserPoints = view.findViewById(R.id.tv_user_points)
        tvEmptyState = view.findViewById(R.id.tv_empty_state)
        progressLoading = view.findViewById(R.id.progress_loading)
        
        // Initialize the recycler view with a grid layout
        val layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerRewards.layoutManager = layoutManager
        
        // Initialize category buttons
        categoryButtons = mapOf(
            "all" to view.findViewById(R.id.btn_category_all),
            "campus" to view.findViewById(R.id.btn_category_campus),
            "food" to view.findViewById(R.id.btn_category_food),
            "merchandise" to view.findViewById(R.id.btn_category_merchandise),
            "experiences" to view.findViewById(R.id.btn_category_experiences)
        )
        
        // Show loading state initially
        showLoading(true)
    }
    
    private fun setupCategoryButtons() {
        // Define the categories we're using in the app and in Firestore
        // These MUST match what's in the database
        val categoryMap = mapOf(
            "all" to "All Rewards",
            "campus" to "Campus Services",
            "food" to "Food & Beverages",
            "merchandise" to "Merchandise",
            "experiences" to "Experiences"
        )
        
        categoryButtons.forEach { (category, button) ->
            // Log for debugging
            Log.d(TAG, "Setting up button for category: '$category'")
            
            button.setOnClickListener {
                Log.d(TAG, "Category button clicked: '$category'")
                activeCategory = category
                loadRewards(activeCategory)
                updateCategoryButtonsUI(activeCategory)
            }
        }
        
        // Set the initial active category
        updateCategoryButtonsUI(activeCategory)
        Log.d(TAG, "Initial category set to: '$activeCategory'")
    }
    
    private fun updateCategoryButtonsUI(activeCategory: String) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val bgColor = ContextCompat.getColor(requireContext(), R.color.background)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val blackColor = ContextCompat.getColor(requireContext(), android.R.color.black)
        
        categoryButtons.forEach { (category, button) ->
            if (category == activeCategory) {
                // Active button state
                button.isEnabled = false
                button.setBackgroundColor(primaryColor)
                button.setTextColor(blackColor) // Use black text for better visibility on colored background
                button.setTypeface(null, android.graphics.Typeface.BOLD) // Make text bold
            } else {
                // Inactive button state
                button.isEnabled = true
                button.setBackgroundColor(bgColor) // Use background color instead of transparent
                button.setTextColor(textPrimaryColor)
                button.setTypeface(null, android.graphics.Typeface.NORMAL) // Normal text weight
            }
        }
    }
    
    private fun setupUserPointsListener() {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot setup listener - userId is empty")
            tvUserPoints.text = "0"
            return
        }
        
        Log.d(TAG, "Setting up real-time listener for user points")
        
        // Remove any existing listener
        userPointsListener?.remove()
        
        // Set up a new listener
        userPointsListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for user points changes", error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    // Try to get totalPoints with different types handling
                    val totalPointsValue = snapshot.get("totalPoints")
                    if (totalPointsValue != null) {
                        userPoints = when (totalPointsValue) {
                            is Long -> totalPointsValue.toInt()
                            is Int -> totalPointsValue
                            is Double -> totalPointsValue.toInt()
                            is String -> totalPointsValue.toIntOrNull() ?: 0
                            else -> 0
                        }
                        
                        Log.d(TAG, "Real-time update: userPoints = $userPoints")
                        
                        // Update UI
                        tvUserPoints.text = userPoints.toString()
                        
                        // Update adapter if it exists
                        updateRewardsAdapterWithPoints()
                    } else {
                        Log.w(TAG, "User document exists but has no totalPoints field")
                        tvUserPoints.text = "0"
                    }
                } else {
                    Log.w(TAG, "User document doesn't exist")
                    tvUserPoints.text = "0"
                }
            }
    }
    
    private fun updateRewardsAdapterWithPoints() {
        val adapter = recyclerRewards.adapter as? RewardsAdapter
        adapter?.updateUserPoints(userPoints)
    }
    
    private fun logAllRewardsInDatabase() {
        db.collection("rewards").get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "====== All rewards in database ======")
                if (documents.isEmpty) {
                    Log.d(TAG, "No rewards found in database!")
                } else {
                    Log.d(TAG, "Found ${documents.size()} rewards:")
                    documents.forEach { doc ->
                        val title = doc.getString("title") ?: "No title"
                        val category = doc.getString("category") ?: "No category"
                        val points = doc.getLong("pointsCost") ?: 0
                        Log.d(TAG, "Reward: $title, Category: '$category', Points: $points")
                    }
                }
                Log.d(TAG, "====================================")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching all rewards for logging", e)
            }
    }
    
    private fun loadRewards(category: String) {
        showLoading(true)
        
        Log.d(TAG, "Loading rewards for category: '$category'")
        
        try {
            // Create query based on category
            val query = if (category == "all") {
                // Load all rewards for "all" category
                db.collection("rewards")
            } else {
                // For specific categories, use the exact category string
                Log.d(TAG, "Filtering by category: '$category'")
                db.collection("rewards").whereEqualTo("category", category)
            }
            
            query.get()
                .addOnSuccessListener { documents ->
                    showLoading(false)
                    
                    Log.d(TAG, "Query returned ${documents.size()} documents")
                    
                    if (documents.isEmpty) {
                        Log.d(TAG, "No rewards found for category: '$category'")
                        tvEmptyState.text = "No rewards available in this category yet"
                        showEmptyState(true)
                        return@addOnSuccessListener
                    }
                    
                    showEmptyState(false)
                    
                    // Log all documents for debugging
                    documents.forEach { doc ->
                        Log.d(TAG, "Found reward: ${doc.id}, title: ${doc.getString("title")}, " +
                                "category: ${doc.getString("category")}, points: ${doc.getLong("pointsCost")}")
                    }
                    
                    // Convert documents to Reward objects
                    rewards = documents.map { doc ->
                        Reward(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            pointsCost = doc.getLong("pointsCost")?.toInt() ?: 0,
                            category = doc.getString("category") ?: "",
                            icon = doc.getString("icon") ?: "🎁",
                            backgroundColor = doc.getString("backgroundColor") ?: "#f0f0f0"
                        )
                    }
                    
                    Log.d(TAG, "Loaded ${rewards.size} rewards for category '$category'")
                    
                    // Set up adapter with rewards
                    setupRewardsAdapter(rewards)
                }
                .addOnFailureListener { exception ->
                    showLoading(false)
                    Log.e(TAG, "Error loading rewards for category '$category'", exception)
                    Toast.makeText(requireContext(), "Error loading rewards. Please try again.", Toast.LENGTH_SHORT).show()
                    tvEmptyState.text = "Unable to load rewards. Please try again."
                    showEmptyState(true)
                }
        } catch (e: Exception) {
            showLoading(false)
            Log.e(TAG, "Exception while setting up rewards query", e)
            Toast.makeText(requireContext(), "Error loading rewards. Please try again.", Toast.LENGTH_SHORT).show()
            tvEmptyState.text = "Unable to load rewards. Please try again."
            showEmptyState(true)
        }
    }
    
    private fun setupRewardsAdapter(rewards: List<Reward>) {
        val adapter = RewardsAdapter(rewards, userPoints) { reward ->
            showRedeemConfirmation(reward)
        }
        recyclerRewards.adapter = adapter
    }
    
    private fun showRedeemConfirmation(reward: Reward) {
        if (userPoints < reward.pointsCost) {
            Toast.makeText(requireContext(), "Not enough points to redeem this reward", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle("Redeem Reward")
            .setMessage("Are you sure you want to redeem ${reward.title} for ${reward.pointsCost} points?")
            .setPositiveButton("Redeem") { _, _ ->
                redeemReward(reward)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun redeemReward(reward: Reward) {
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "User ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check points balance again
        if (userPoints < reward.pointsCost) {
            Toast.makeText(requireContext(), "Insufficient points to redeem this reward", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading indicator
        showLoading(true)
        
        // Create a batch to update points and add redemption record
        val batch = db.batch()
        
        // Update user points
        val userRef = db.collection("users").document(userId)
        batch.update(userRef, "totalPoints", FieldValue.increment(-reward.pointsCost.toLong()))
        
        // Create redemption record
        val redemptionRef = db.collection("redemptions").document()
        val redemptionData = HashMap<String, Any>()
        redemptionData["userId"] = userId
        redemptionData["rewardId"] = reward.id
        redemptionData["rewardName"] = reward.title
        redemptionData["pointsCost"] = reward.pointsCost
        redemptionData["timestamp"] = FieldValue.serverTimestamp()
        redemptionData["status"] = "pending"
        
        batch.set(redemptionRef, redemptionData)
        
        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Successfully redeemed ${reward.title}!", Toast.LENGTH_SHORT).show()
                
                // Points will be updated automatically through the snapshot listener
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error redeeming reward", e)
                Toast.makeText(requireContext(), "Failed to redeem reward. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        
        // Only hide the recycler if we're loading and it's empty
        if (isLoading && (recyclerRewards.adapter?.itemCount ?: 0) == 0) {
            recyclerRewards.visibility = View.GONE
        } else if (!isLoading) {
            recyclerRewards.visibility = View.VISIBLE
        }
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerRewards.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    // Adapter for rewards
    inner class RewardsAdapter(
        private val rewards: List<Reward>,
        private var userPoints: Int,
        private val onRedeemClick: (Reward) -> Unit
    ) : RecyclerView.Adapter<RewardsAdapter.RewardViewHolder>() {
        
        fun updateUserPoints(points: Int) {
            this.userPoints = points
            notifyDataSetChanged() // Update all items to reflect new points status
        }
        
        inner class RewardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val rewardIcon: TextView = itemView.findViewById(R.id.tv_reward_icon)
            val rewardTitle: TextView = itemView.findViewById(R.id.tv_reward_title)
            val rewardDescription: TextView = itemView.findViewById(R.id.tv_reward_description)
            val rewardPoints: TextView = itemView.findViewById(R.id.tv_reward_points)
            val redeemButton: Button = itemView.findViewById(R.id.btn_redeem)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RewardViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reward, parent, false)
            return RewardViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: RewardViewHolder, position: Int) {
            val reward = rewards[position]
            
            holder.rewardIcon.text = reward.icon
            holder.rewardTitle.text = reward.title
            holder.rewardDescription.text = reward.description
            holder.rewardPoints.text = "${reward.pointsCost} pts"
            
            // Set background color for icon container
            try {
                holder.rewardIcon.setBackgroundColor(Color.parseColor(reward.backgroundColor))
            } catch (e: Exception) {
                holder.rewardIcon.setBackgroundColor(Color.parseColor("#f0f0f0"))
            }
            
            // Set up redeem button based on user points
            val canRedeem = userPoints >= reward.pointsCost
            holder.redeemButton.isEnabled = canRedeem
            
            // Change button appearance based on points status
            if (canRedeem) {
                holder.redeemButton.text = "Redeem"
                holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
                holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            } else {
                holder.redeemButton.text = "Not Enough Points"
                holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_disabled))
                holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
            
            holder.redeemButton.setOnClickListener {
                if (canRedeem) {
                    onRedeemClick(reward)
                } else {
                    Toast.makeText(context, "You need ${reward.pointsCost - userPoints} more points", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Highlight if user can afford this reward
            if (canRedeem) {
                holder.rewardPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                holder.rewardPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        
        override fun getItemCount() = rewards.size
    }

    companion object {
        @JvmStatic
        fun newInstance() = RewardsFragment()
    }
} 