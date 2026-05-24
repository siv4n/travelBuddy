package com.example.travel_buddy

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.travel_buddy.databinding.ActivityMainBinding
import androidx.navigation.fragment.NavHostFragment
import com.example.travel_buddy.di.ServiceLocator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ServiceLocator.initialize(this)

        val destination = intent.getStringExtra("destination")
        if (destination == "register") {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navHostFragment.navController.navigate(R.id.registerFragment)
        }
    }
}
