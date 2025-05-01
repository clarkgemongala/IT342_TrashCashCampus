package com.example.trashcashcampus_mobile.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.trashcashcampus_mobile.R
import com.example.trashcashcampus_mobile.utils.VoucherGenerator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale
import android.animation.AnimatorSet
import android.os.Build

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
        
        // Start the animation
        animateRewardsEntrance()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the user points listener when fragment is destroyed
        userPointsListener?.remove()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if userId is available, if not try to get it
        if (userId.isEmpty()) {
            getUserId()
        }
        
        // Force refresh user points when returning to this fragment
        if (userId.isNotEmpty()) {
            // Refresh the points data
            Log.d(TAG, "Forcing user points refresh in RewardsFragment onResume")
            forceRefreshUserPoints()
        }
        
        // Set up listener if not already active
        if (userPointsListener == null && userId.isNotEmpty()) {
            setupUserPointsListener()
        }
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
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        
        categoryButtons.forEach { (category, button) ->
            if (category == activeCategory) {
                // Active button state - explicitly set the button's chipBackgroundColor (for Material Chips)
                if (button is com.google.android.material.chip.Chip) {
                    button.setChipBackgroundColorResource(R.color.primary)
                    button.setTextColor(whiteColor)
                } else {
                    // Fallback for regular buttons
                    button.setBackgroundColor(primaryColor)
                    button.setTextColor(whiteColor)
                }
                button.isEnabled = true // Keep it enabled to maintain appearance
            } else {
                // Inactive button state
                if (button is com.google.android.material.chip.Chip) {
                    button.setChipBackgroundColorResource(R.color.surface)
                    button.setTextColor(textPrimaryColor)
                } else {
                    // Fallback for regular buttons
                    button.setBackgroundColor(bgColor)
                    button.setTextColor(textPrimaryColor)
                }
                button.isEnabled = true
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
                    val pointsValue = snapshot.get("points")
                    
                    // Calculate points from both fields
                    val totalPoints = when (totalPointsValue) {
                        is Long -> totalPointsValue.toInt()
                        is Int -> totalPointsValue
                        is Double -> totalPointsValue.toInt()
                        is String -> totalPointsValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    val points = when (pointsValue) {
                        is Long -> pointsValue.toInt()
                        is Int -> pointsValue
                        is Double -> pointsValue.toInt()
                        is String -> pointsValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    // Use the higher value for consistency
                    userPoints = Math.max(totalPoints, points)
                    
                    Log.d(TAG, "Real-time update: userPoints = $userPoints (totalPoints=$totalPoints, points=$points)")
                    
                    // Update UI
                    tvUserPoints.text = userPoints.toString()
                    
                    // Update adapter if it exists
                    updateRewardsAdapterWithPoints()
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
        
        // Check if this reward was already redeemed today
        checkDailyRedemptionLimit(reward) { alreadyRedeemed ->
            if (alreadyRedeemed) {
                Toast.makeText(requireContext(), "You have already redeemed this reward today. Please try again tomorrow.", Toast.LENGTH_LONG).show()
                return@checkDailyRedemptionLimit
            }
            
            // Show confirmation dialog if not already redeemed today
            AlertDialog.Builder(requireContext())
                .setTitle("Redeem Reward")
                .setMessage("Are you sure you want to redeem ${reward.title} for ${reward.pointsCost} points?")
                .setPositiveButton("Redeem") { _, _ ->
                    redeemReward(reward)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    /**
     * Checks if user has already redeemed this reward today
     * @param reward The reward to check
     * @param callback Called with true if already redeemed today, false otherwise
     */
    private fun checkDailyRedemptionLimit(reward: Reward, callback: (Boolean) -> Unit) {
        if (userId.isEmpty()) {
            callback(false)
            return
        }
        
        // Get today's date in a consistent format (YYYY-MM-DD)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = dateFormat.format(Date())
        
        // Query Firestore for any redemptions of this reward by this user today
        db.collection("redemptions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("rewardId", reward.id)
            .get()
            .addOnSuccessListener { documents ->
                var alreadyRedeemedToday = false
                
                for (document in documents) {
                    // Get timestamp from document
                    val timestamp = document.getTimestamp("timestamp")
                    
                    if (timestamp != null) {
                        // Convert timestamp to same date format
                        val redemptionDate = dateFormat.format(timestamp.toDate())
                        
                        // Check if redemption date matches today's date
                        if (redemptionDate == todayDate) {
                            alreadyRedeemedToday = true
                            break
                        }
                    }
                }
                
                callback(alreadyRedeemedToday)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking redemption limit", e)
                // Default to not redeemed in case of error
                callback(false)
            }
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
        
        // Generate a unique voucher code for this redemption
        // Use the first character of category as prefix if available
        val prefix = if (reward.category.isNotEmpty()) reward.category.substring(0, 1).uppercase() else ""
        val voucherCode = VoucherGenerator.generateVoucherCode(prefix)
        
        // Create a batch to update points and add redemption record
        val batch = db.batch()
        
        // Update user points in both fields for compatibility
        val userRef = db.collection("users").document(userId)
        batch.update(userRef, "totalPoints", FieldValue.increment(-reward.pointsCost.toLong()))
        batch.update(userRef, "points", FieldValue.increment(-reward.pointsCost.toLong()))
        
        // Create redemption record with voucher code
        val redemptionRef = db.collection("redemptions").document()
        val redemptionData = HashMap<String, Any>()
        redemptionData["userId"] = userId
        redemptionData["rewardId"] = reward.id
        redemptionData["rewardName"] = reward.title
        redemptionData["pointsCost"] = reward.pointsCost
        redemptionData["timestamp"] = FieldValue.serverTimestamp()
        redemptionData["status"] = "pending"
        redemptionData["voucherCode"] = voucherCode  // Store the voucher code in the redemption record
        
        batch.set(redemptionRef, redemptionData)
        
        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                showLoading(false)
                
                // Show voucher code dialog
                showVoucherCodeDialog(reward, voucherCode)
                
                // Points will be updated automatically through the snapshot listener
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Log.e(TAG, "Error redeeming reward", e)
                Toast.makeText(requireContext(), "Failed to redeem reward. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }
    
    /**
     * Shows the voucher code dialog after successful redemption
     */
    private fun showVoucherCodeDialog(reward: Reward, voucherCode: String) {
        // Create dialog from custom layout
        val dialog = Dialog(requireContext(), android.R.style.Theme_Material_Light_Dialog)
        dialog.setContentView(R.layout.dialog_voucher_code)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false) // Prevent dismissal by tapping outside
        
        // Set up views
        val tvRewardTitle = dialog.findViewById<TextView>(R.id.tv_reward_title)
        val tvVoucherCode = dialog.findViewById<TextView>(R.id.tv_voucher_code)
        val tvVoucherDescription = dialog.findViewById<TextView>(R.id.tv_voucher_description)
        val btnCopyCode = dialog.findViewById<Button>(R.id.btn_copy_code)
        val btnClose = dialog.findViewById<Button>(R.id.btn_close)
        
        // Set reward details - ensure proper formatting
        tvRewardTitle.text = reward.title
        tvVoucherCode.text = voucherCode
        tvVoucherDescription.text = "Use this voucher code to claim your ${reward.title}:"
        
        // Copy code button with improved feedback
        btnCopyCode.setOnClickListener {
            val clipboardManager = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Voucher Code", voucherCode)
            clipboardManager.setPrimaryClip(clip)
            
            // Show success feedback
            btnCopyCode.text = "Copied!"
            btnCopyCode.isEnabled = false
            
            // Reset after a delay
            Handler(Looper.getMainLooper()).postDelayed({
                btnCopyCode.text = "Copy Code"
                btnCopyCode.isEnabled = true
            }, 1500)
            
            Toast.makeText(requireContext(), "Voucher code copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        
        // Close button
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        // Store in Firestore a record with the voucher code
        logVoucherRedemption(reward, voucherCode)
        
        // Show the dialog
        dialog.show()
    }
    
    /**
     * Logs additional information about the redemption for admin purposes
     */
    private fun logVoucherRedemption(reward: Reward, voucherCode: String) {
        try {
            val redemptionLog = HashMap<String, Any>()
            redemptionLog["userId"] = userId
            redemptionLog["rewardId"] = reward.id
            redemptionLog["rewardName"] = reward.title
            redemptionLog["voucherCode"] = voucherCode
            redemptionLog["timestamp"] = FieldValue.serverTimestamp()
            redemptionLog["deviceInfo"] = Build.MODEL
            
            // Add to voucher tracking collection for admin viewing
            db.collection("voucherCodes").document(voucherCode)
                .set(redemptionLog)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error logging voucher redemption", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logVoucherRedemption", e)
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
            
            // Check if this reward is redeemable today (points and daily limit check)
            val canRedeemPoints = userPoints >= reward.pointsCost
            
            // This will hold our final result after the daily redemption check
            var canRedeem = canRedeemPoints
            
            // Set up the button in "checking" state if we have enough points
            if (canRedeemPoints) {
                // Initially set button to checking state
                updateRedeemButtonForChecking(holder)
                
                // Check daily redemption status (only if user has enough points)
                checkDailyRedemptionLimit(reward) { alreadyRedeemed ->
                    // Update the final result based on daily redemption check
                    canRedeem = canRedeemPoints && !alreadyRedeemed
                    
                    // Update button UI based on both points and daily limit
                    if (canRedeem) {
                        updateRedeemButtonForAvailable(holder)
                    } else if (alreadyRedeemed) {
                        updateRedeemButtonForAlreadyRedeemed(holder)
                    } else {
                        updateRedeemButtonForNotEnoughPoints(holder)
                    }
                    
                    // Set up click listener based on final state
                    setupRedeemButtonClickListener(holder, reward, canRedeem, alreadyRedeemed)
                }
            } else {
                // Not enough points, no need to check daily limit
                updateRedeemButtonForNotEnoughPoints(holder)
                
                // Set up click listener for not enough points
                setupRedeemButtonClickListener(holder, reward, false, false)
            }
            
            // Highlight if user can afford this reward
            if (canRedeemPoints) {
                holder.rewardPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.success))
            } else {
                holder.rewardPoints.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
        }
        
        private fun updateRedeemButtonForAvailable(holder: RewardViewHolder) {
            holder.redeemButton.text = "Redeem"
            holder.redeemButton.isEnabled = true
            holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        
        private fun updateRedeemButtonForNotEnoughPoints(holder: RewardViewHolder) {
            holder.redeemButton.text = "Not Enough Points"
            holder.redeemButton.isEnabled = false
            holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_disabled))
            holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
        
        private fun updateRedeemButtonForAlreadyRedeemed(holder: RewardViewHolder) {
            holder.redeemButton.text = "Already Redeemed Today"
            holder.redeemButton.isEnabled = false
            holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.text_disabled))
            holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
        }
        
        private fun updateRedeemButtonForChecking(holder: RewardViewHolder) {
            holder.redeemButton.text = "Checking..."
            holder.redeemButton.isEnabled = false
            holder.redeemButton.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            holder.redeemButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        
        private fun setupRedeemButtonClickListener(
            holder: RewardViewHolder, 
            reward: Reward, 
            canRedeem: Boolean, 
            alreadyRedeemed: Boolean
        ) {
            holder.redeemButton.setOnClickListener {
                when {
                    canRedeem -> onRedeemClick(reward)
                    alreadyRedeemed -> Toast.makeText(
                        context, 
                        "You've already redeemed this reward today. Try again tomorrow.", 
                        Toast.LENGTH_SHORT
                    ).show()
                    else -> Toast.makeText(
                        context, 
                        "You need ${reward.pointsCost - userPoints} more points", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        override fun getItemCount() = rewards.size
    }

    private fun animateRewardsEntrance() {
        // Get references to view elements
        val titleText = view?.findViewById<View>(R.id.tv_rewards_title)
        val pointsCard = view?.findViewById<View>(R.id.card_user_points)
        val categoriesCard = view?.findViewById<View>(R.id.card_categories)
        val rewardsListCard = view?.findViewById<View>(R.id.card_rewards_list)
        
        // Define animation durations and delays
        val baseDelay = 100L
        val animDuration = 500L
        
        // Animate the title
        titleText?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .setStartDelay(baseDelay)
                .start()
        }
        
        // Animate the points card
        pointsCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 150)
                .start()
        }
        
        // Animate the categories card
        categoriesCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 250)
                .start()
        }
        
        // Animate the rewards list card
        rewardsListCard?.apply {
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(animDuration)
                .setInterpolator(android.view.animation.OvershootInterpolator(0.8f))
                .setStartDelay(baseDelay + 350)
                .start()
        }
    }

    private fun forceRefreshUserPoints() {
        if (userId.isEmpty()) {
            Log.e(TAG, "Cannot refresh user points - userId is empty")
            return
        }
        
        // Show loading indicator
        showLoading(true)
        
        // Get fresh data from Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Log data for debugging
                    val userData = document.data
                    userData?.forEach { (key, value) ->
                        Log.d(TAG, "User data field: $key = $value")
                    }
                    
                    // Get points from totalPoints field
                    val totalPointsValue = when (val pointsValue = document.get("totalPoints")) {
                        is Long -> pointsValue.toInt()
                        is Int -> pointsValue
                        is Double -> pointsValue.toInt()
                        is String -> pointsValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    // Also check the 'points' field
                    val pointsValue = when (val pointsValue = document.get("points")) {
                        is Long -> pointsValue.toInt()
                        is Int -> pointsValue
                        is Double -> pointsValue.toInt()
                        is String -> pointsValue.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    // Use the higher value of the two fields
                    val finalPointsValue = Math.max(totalPointsValue, pointsValue)
                    
                    Log.d(TAG, "Force refreshed points: totalPoints=$totalPointsValue, points=$pointsValue, using max: $finalPointsValue")
                    
                    // Update UI and adapter
                    userPoints = finalPointsValue
                    tvUserPoints.text = finalPointsValue.toString()
                    updateRewardsAdapterWithPoints()
                } else {
                    Log.e(TAG, "User document not found during refresh")
                }
                
                // Hide loading indicator
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error refreshing user points", e)
                showLoading(false)
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RewardsFragment()
    }
} 