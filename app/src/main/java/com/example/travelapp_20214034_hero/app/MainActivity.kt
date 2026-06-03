package com.example.travelapp_20214034_hero.app

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.travelapp_20214034_hero.R
import com.example.travelapp_20214034_hero.databinding.ActivityMainBinding
import com.example.travelapp_20214034_hero.ui.list.HomeFragment
import com.example.travelapp_20214034_hero.ui.map.MapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var homeFragment: HomeFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        val existingHome = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? HomeFragment
        if (existingHome != null) {
            homeFragment = existingHome
        } else if (savedInstanceState == null) {
            showFragment(HomeFragment(), TAG_HOME, addToBackStack = false)
        } else {
            homeFragment = supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_home

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                    if (current !is HomeFragment) {
                        if (supportFragmentManager.backStackEntryCount > 0) {
                            supportFragmentManager.popBackStack()
                            homeFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
                                as? HomeFragment
                        } else {
                            showFragment(HomeFragment(), TAG_HOME, addToBackStack = false)
                        }
                    }
                    invalidateOptionsMenu()
                    true
                }
                R.id.nav_map -> {
                    if (supportFragmentManager.findFragmentById(R.id.fragmentContainer) !is MapFragment) {
                        showFragment(MapFragment(), TAG_MAP, addToBackStack = true)
                    }
                    invalidateOptionsMenu()
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment, tag: String, addToBackStack: Boolean) {
        if (fragment is HomeFragment) {
            homeFragment = fragment
        }
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
        if (addToBackStack) {
            transaction.addToBackStack(tag)
        }
        transaction.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val onMap = supportFragmentManager.findFragmentById(R.id.fragmentContainer) is MapFragment
        menu.findItem(R.id.action_sort)?.isVisible = !onMap
        menu.findItem(R.id.action_delete_all)?.isVisible = !onMap
        menu.findItem(R.id.action_map_normal)?.isVisible = onMap
        menu.findItem(R.id.action_map_satellite)?.isVisible = onMap
        menu.findItem(R.id.action_map_fit)?.isVisible = onMap
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val home = findHomeFragment()
        val map = findMapFragment()
        return when (item.itemId) {
            R.id.action_sort -> {
                home?.toggleSort()
                true
            }
            R.id.action_delete_all -> {
                home?.confirmDeleteAll()
                true
            }
            R.id.action_about -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(R.string.ok, null)
                    .show()
                true
            }
            R.id.action_map_normal -> {
                map?.setMapTypeNormal()
                true
            }
            R.id.action_map_satellite -> {
                map?.setMapTypeSatellite()
                true
            }
            R.id.action_map_fit -> {
                map?.fitAllMarkers()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun findHomeFragment(): HomeFragment? {
        homeFragment?.let { return it }
        return supportFragmentManager.fragments
            .filterIsInstance<HomeFragment>()
            .firstOrNull()
    }

    private fun findMapFragment(): MapFragment? {
        return supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? MapFragment
            ?: supportFragmentManager.fragments.filterIsInstance<MapFragment>().firstOrNull()
    }

    companion object {
        private const val TAG_HOME = "home"
        private const val TAG_MAP = "map"
    }
}
