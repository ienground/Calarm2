package zone.ien.calarm.activity

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import zone.ien.calarm.R
import zone.ien.calarm.constant.SharedDefault
import zone.ien.calarm.constant.SharedKey
import zone.ien.calarm.databinding.ActivityLocationBinding

class LocationActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    lateinit var binding: ActivityLocationBinding

    lateinit var map: GoogleMap
    lateinit var geocoder: Geocoder
    lateinit var sharedPreferences: SharedPreferences
    lateinit var lm: LocationManager
    lateinit var locationListener: LocationListener

    var currentLatitude = SharedDefault.HOME_LATITUDE.toDouble()
    var currentLongitude = SharedDefault.HOME_LONGITUDE.toDouble()
    var currentAddress = ""

    var setLatitude = SharedDefault.HOME_LATITUDE.toDouble()
    var setLongitude = SharedDefault.HOME_LONGITUDE.toDouble()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_location)
        binding.activity = this

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        geocoder = Geocoder(this)
        sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        setLatitude = sharedPreferences.getFloat(SharedKey.HOME_LATITUDE, setLatitude.toFloat()).toDouble()
        setLongitude = sharedPreferences.getFloat(SharedKey.HOME_LONGITUDE, setLongitude.toFloat()).toDouble()
        currentLatitude = setLatitude
        currentLongitude = setLongitude

        locationListener = LocationListener { location ->
            currentLatitude = location.latitude
            currentLongitude = location.longitude

            var address: List<Address> = listOf()
            val cameraPosition = LatLng(currentLatitude, currentLongitude)
            val markerOptions = MarkerOptions().apply {
                position(cameraPosition)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(currentLatitude, currentLongitude, 1) { address = it }
                } else {
                    address = geocoder.getFromLocation(currentLatitude, currentLongitude, 1) as List<Address>
                }
            } catch (e: Exception) {
                listOf<Address>()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                val currentLocationAddress = if (address.isNotEmpty()) {
                    val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                    if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
                } else {
                    getString(R.string.cannot_get_address)
                }

                currentAddress = currentLocationAddress
                binding.tvAddress.text = currentLocationAddress
            }, 1000)

            map.clear()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, 18.5f))
            map.addMarker(markerOptions)

            setLatitude = currentLatitude
            setLongitude = currentLongitude
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnSet.setOnClickListener {
            sharedPreferences.edit().putFloat(SharedKey.HOME_LATITUDE, setLatitude.toFloat()).apply()
            sharedPreferences.edit().putFloat(SharedKey.HOME_LONGITUDE, setLongitude.toFloat()).apply()
            sharedPreferences.edit().putLong(SharedKey.HOME_CHANGE_TIME, System.currentTimeMillis()).apply()

            setResult(RESULT_OK)
            finish()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener(this)

        var address: List<Address> = listOf()
        val cameraPosition = LatLng(setLatitude, setLongitude)
        val markerOptions = MarkerOptions().apply {
            position(cameraPosition)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(currentLatitude, currentLongitude, 1) { address = it }
            } else {
                address = geocoder.getFromLocation(currentLatitude, currentLongitude, 1) as List<Address>
            }
        } catch (e: Exception) {
            listOf<Address>()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentLocationAddress = if (address.isNotEmpty()) {
                val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
            } else {
                getString(R.string.cannot_get_address)
            }

            currentAddress = currentLocationAddress
            binding.tvAddress.text = currentLocationAddress
        }, 1000)

        map.clear()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, 18.5f))
        map.addMarker(markerOptions)
    }

    override fun onMapClick(latLng: LatLng) {
        val cameraPosition = LatLng(latLng.latitude, latLng.longitude)
        val markerOptions = MarkerOptions().apply {
            position(cameraPosition)
        }
        var address: List<Address> = listOf()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(currentLatitude, currentLongitude, 1) { address = it }
            } else {
                address = geocoder.getFromLocation(currentLatitude, currentLongitude, 1) as List<Address>
            }
        } catch (e: Exception) {
            listOf<Address>()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val currentLocationAddress = if (address.isNotEmpty()) {
                val firstAddress = address.first().getAddressLine(0).replace(address.first().countryName, "")
                if (firstAddress.isNotBlank() && firstAddress.first() == ' ') firstAddress.substring(1) else firstAddress
            } else {
                getString(R.string.cannot_get_address)
            }

            currentAddress = currentLocationAddress
            binding.tvAddress.text = currentLocationAddress
        }, 1000)

        map.clear()
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, 18.5f))
        map.addMarker(markerOptions)

        setLatitude = latLng.latitude
        setLongitude = latLng.longitude
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_location, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
            }
            R.id.menu_current_location -> {
                val cameraPosition = LatLng(currentLatitude, currentLongitude)
                val markerOptions = MarkerOptions().apply {
                    position(cameraPosition)
                }

                binding.tvAddress.text = currentAddress
                map.clear()
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(cameraPosition, 18.5f))
                map.addMarker(markerOptions)

                val isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
                val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 0)
                } else {
                    if (isGPSEnabled) {
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
                    } else if (isNetworkEnabled) {
                        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10f, locationListener)
                    }
                }
            }
            R.id.menu_search -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

}