package com.example.trashcashcampus_mobile.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.trashcashcampus_mobile.QRScannerActivity
import com.example.trashcashcampus_mobile.R

class MapFragment : Fragment() {
    private val TAG = "MapFragment"
    
    // UI elements
    private lateinit var btnScanQR: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI elements
        btnScanQR = view.findViewById(R.id.btn_scan_qr)
        
        // Set up button click listeners
        setupListeners()
    }
    
    private fun setupListeners() {
        btnScanQR.setOnClickListener {
            val intent = Intent(requireContext(), QRScannerActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }
} 