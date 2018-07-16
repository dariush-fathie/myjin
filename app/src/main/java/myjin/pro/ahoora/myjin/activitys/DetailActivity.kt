package myjin.pro.ahoora.myjin.activitys

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.constraint.ConstraintLayout
import android.support.design.widget.BottomSheetBehavior
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApi
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.detail_map.*
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.customClasses.CustomBottomSheetBehavior
import myjin.pro.ahoora.myjin.interfaces.TempListener
import myjin.pro.ahoora.myjin.models.KotlinAboutContactModel
import myjin.pro.ahoora.myjin.models.KotlinGroupModel
import myjin.pro.ahoora.myjin.models.KotlinItemModel
import myjin.pro.ahoora.myjin.models.SimpleResponseModel
import myjin.pro.ahoora.myjin.utils.*
import retrofit2.Call
import retrofit2.Response
import java.util.*
import java.util.concurrent.TimeUnit

class DetailActivity : AppCompatActivity(), View.OnClickListener, OnMapReadyCallback, GoogleMap.OnCameraMoveListener {
    internal var Receiver: BroadcastReceiver? = null
    private val request_permission = 1052
    private val request_location_setting = 1053

    private val realm = Realm.getDefaultInstance()!!
    private var id = 0
    private var i = 0
    private var isSaved = false
    private var mode = true
    private var g_url = ""
    private var n = 0
    private var forward = true
    private var t = Timer()
    private var play = true

    private lateinit var polyline: Polyline

    private var theBitmap: Bitmap? = null
    // if model save or delete form realm -> change = true  else change = false
    var change = false
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var bottomSheetCallback: BottomSheetBehavior.BottomSheetCallback
    var mMap: GoogleMap? = null

    // current model
    lateinit var item: KotlinItemModel

    // google location service
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    // send request to location service with @mLocationRequest
    private lateinit var mLocationRequest: LocationRequest
    // @mLocationRequest response
    private lateinit var mLocationCallback: LocationCallback

    private lateinit var mSettingsClient: SettingsClient
    private lateinit var mLocationSettingsRequest: LocationSettingsRequest

    private lateinit var currentPoint: LatLng

    private val updateInterval: Long = 10000
    private val fastestUpdateInterval: Long = updateInterval / 2

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_save -> {
                change = true
                if (isSaved) {
                    val draw = ContextCompat.getDrawable(this@DetailActivity, R.drawable.icons_bookmark_1)
                    draw?.setColorFilter(ContextCompat.getColor(this@DetailActivity, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                    iv_save.setImageDrawable(draw)
                    deleteItem(id)
                    isSaved = false

                } else {
                    val draw = ContextCompat.getDrawable(this@DetailActivity, R.drawable.ic_bookmark)
                    draw?.setColorFilter(ContextCompat.getColor(this@DetailActivity, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                    iv_save.setImageDrawable(draw)
                    saveItem(id)
                    isSaved = true
                }

                animateBookmark(iv_save)
            }

            R.id.rl_seeOnMap -> {
                openMapSheet()
            }
            R.id.fab_closeMap -> {
                closeMapSheet()
            }
            R.id.btn_savePoint -> {
                savePoint()
            }
            R.id.fab_direction -> directionRequest()
            R.id.iv_goback -> {
                onBackPressed()
            }

            R.id.cv_direction -> selectModeDir()
            R.id.cv_level -> selectModeLevel()
            R.id.iv_share -> share()

            R.id.tv_website->goToWebSite()
            R.id.tv_telephone-> call()
        }
    }



    fun animateBookmark(view: ImageView) {
        val animation = AnimationSet(true)
        animation.addAnimation(AlphaAnimation(0.0f, 1.0f))
        animation.addAnimation(ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f))
        animation.duration = 400
        animation.repeatMode = Animation.REVERSE
        view.startAnimation(animation)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        if (intent != null) {
            id = intent.getIntExtra("id", 1)
            isSaved = checkItemIsSaved()
            g_url = intent.getStringExtra("g_url")

            if (isSaved) {
                val draw = ContextCompat.getDrawable(this@DetailActivity, R.drawable.ic_bookmark)
                draw?.setColorFilter(ContextCompat.getColor(this@DetailActivity, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                iv_save.setImageDrawable(draw)
            } else {
                val draw = ContextCompat.getDrawable(this@DetailActivity, R.drawable.icons_bookmark_1)
                draw?.setColorFilter(ContextCompat.getColor(this@DetailActivity, R.color.colorAccent), PorterDuff.Mode.SRC_IN)
                iv_save.setImageDrawable(draw)
            }
            if (intent.getIntExtra(StaticValues.MODEL, 0) == 0) {
                i = 0
            } else if (intent.getIntExtra(StaticValues.MODEL, 0) == 1) {
                i = 1
                iv_save.visibility = View.GONE
            } else if (intent.getIntExtra(StaticValues.MODEL, 0) == 2) {
                i = 2
                iv_save.visibility = View.VISIBLE
            }

            if (i != 2) {
                val realm = Realm.getDefaultInstance()
                realm.beginTransaction()
                item = realm.where(KotlinItemModel::class.java).equalTo("centerId", id).findFirst()!!
                realm.commitTransaction()
            } else {
                item = SearchActivity.tempModel
            }

            loadDetails()
            iv_save.setOnClickListener(this)
            btn_savePoint.setOnClickListener(this)
            fab_closeMap.setOnClickListener(this)
            rl_seeOnMap.setOnClickListener(this)
            fab_direction.setOnClickListener(this)
            iv_goback.setOnClickListener(this)
            iv_share.setOnClickListener(this)
            cv_direction.setOnClickListener(this)
            cv_level.setOnClickListener(this)

            tv_website.setOnClickListener(this)
            tv_telephone.setOnClickListener(this)

        }
        initBottomSheet()
        initLists()
        startReceive()
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(Receiver, IntentFilter(getString(R.string.reciver2)))
    }

    private fun startReceive() {
        if (Receiver == null) {

            Receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.getAction().equals(getString(R.string.reciver2))) {
                        playOrStop()
                    }
                }
            }
        }
    }

    private fun selectModeDir() {
        val popupMenu = PopupMenu(this@DetailActivity, cv_direction, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.menu1, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.car -> {
                    mode = false
                    iv_direction.setImageDrawable(ContextCompat.getDrawable(this@DetailActivity, R.drawable.ic_car))
                }
                R.id.walking -> {
                    mode = true
                    iv_direction.setImageDrawable(ContextCompat.getDrawable(this@DetailActivity, R.drawable.ic_walking))

                }
            }
            true
        }
        popupMenu.show()
    }

    private var mapType = 0

    private fun selectModeLevel() {
        val popupMenu = PopupMenu(this@DetailActivity, cv_level, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.menu2, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.default_ -> {
                    mapType = 0
                    mMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
                }
                R.id.satelite -> {

                    mapType = 1
                    mMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                }
                R.id.terrain -> {
                    mapType = 2
                    mMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN


                }
            }
            true
        }

        popupMenu.show()
    }

    private fun share() {
        val imageUri: Uri = Uri.parse("android.resource://$packageName/drawable/ic_jin")
        val shareIntent: Intent = Intent()

        var str = ""

        str =  "${tv_title.text}\n\n"
        str += "${tv_subTitle.text}\n\n"
        str += " آدرس : " + "${tv_addr.text}\n\n"

        str +="لینک دانلود ژین من \n"

        if(realm.isInTransaction)  realm.commitTransaction()

        val id = 1


        realm.beginTransaction()
        val res = realm.where(KotlinAboutContactModel::class.java)
                .equalTo("id", id)
                .findFirst()!!
        str+= res.tKafeh.toString()
        realm.commitTransaction()

/*String url= MediaStore.Images.Media.insertImage(this.getContentResolver(), theBitmap, "title", "description");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url));*/


        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
        shareIntent.putExtra(Intent.EXTRA_TEXT, str)
        shareIntent.type = "image/*"
        startActivity(Intent.createChooser(shareIntent, "send"))

    }

    private fun getGeoContext(): GeoApiContext {
        val geoApiContext = GeoApiContext.Builder()
        geoApiContext.apiKey("AIzaSyBvIp2HXDke5MeT8IMJczTJ8wGJwjLvUXw")
        geoApiContext.queryRateLimit(3)
        geoApiContext.connectTimeout(5, TimeUnit.SECONDS)
        geoApiContext.writeTimeout(5, TimeUnit.SECONDS)
        geoApiContext.readTimeout(5, TimeUnit.SECONDS)
        return geoApiContext.build()
    }

    private fun directionRequest() {
        val builder = AlertDialog.Builder(this)
        builder.setView(R.layout.progress_dialog)
        builder.setCancelable(false)
        val alert = builder.create()
        alert.show()
        if (this::currentPoint.isInitialized) {
            val itemAddressLatLng = LatLng(item.addressList!![0]?.latitude?.toDouble()!!
                    , item.addressList!![0]?.longitude?.toDouble()!!)
            val origin: com.google.maps.model.LatLng = com.google.maps.model.LatLng(currentPoint.latitude, currentPoint.longitude)
            val destination: com.google.maps.model.LatLng =
                    com.google.maps.model.LatLng(itemAddressLatLng.latitude, itemAddressLatLng.longitude)

            val directionReq: DirectionsApiRequest = DirectionsApi.newRequest(getGeoContext())
            directionReq.destination(origin)
            directionReq.origin(destination)
            directionReq.mode(TravelMode.WALKING)
            directionReq.alternatives(false)
            directionReq.setCallback(object : PendingResult.Callback<DirectionsResult> {
                override fun onResult(result: DirectionsResult?) {
                    runOnUiThread {
                        addPolyline(result!!, mMap!!, LatLng(origin.lat, origin.lng), LatLng(destination.lat, destination.lng))
                        alert.dismiss()
                    }
                }

                override fun onFailure(e: Throwable?) {
                    runOnUiThread {
                        alert.dismiss()
                        Log.e("direction error", e?.message + " ")
                        Toast.makeText(this@DetailActivity, "خطایی رخ داد", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(this, "مکان فعلی شما مشخص نیست", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var originMarker: Marker
    private lateinit var destinationMarker: Marker

    private fun addPolyline(results: DirectionsResult, mMap: GoogleMap, origin: LatLng, destination: LatLng) {
        if (this::originMarker.isInitialized) {
            originMarker.remove()
            destinationMarker.remove()
            polyline.remove()
        }

        originMarker = mMap.addMarker(MarkerOptions().title("مبداء").position(origin))
        destinationMarker = mMap.addMarker(MarkerOptions().title("مقصد").position(destination))

        val builder = LatLngBounds.Builder()
        builder.include(origin).include(destination)

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))

        val decodedPath = results.routes[0].overviewPolyline.decodePath()
        val paths = ArrayList<com.google.android.gms.maps.model.LatLng>()
        Log.e("path", "${decodedPath.size}$decodedPath")
        decodedPath.forEach { path: com.google.maps.model.LatLng ->
            paths.add(com.google.android.gms.maps.model.LatLng(path.lat, path.lng))
        }
        Log.e("paths", "${paths.size}$paths")
        polyline = mMap.addPolyline(PolylineOptions().addAll(paths).color(ContextCompat.getColor(this, R.color.green)))
    }

    @SuppressLint("MissingPermission")
    private fun initLocationApi() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        // get last saved location
        mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
            //mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 17f))
            try {
                currentPoint = LatLng(location.latitude, location.longitude)
            } catch (e: Exception) {
                Log.e("loc", "error")
            }
        }
    }

    private fun createLocationRequest() {
        initLocationApi()
        mLocationRequest = LocationRequest.create()
        // update user location each 10000ms = 10s
        mLocationRequest.interval = updateInterval
        mLocationRequest.fastestInterval = fastestUpdateInterval
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        checkLocationSetting()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == request_location_setting) {
            checkLocationSetting()
        }
    }

    //open location setting page to active location toggle buttton
    private fun locationSettingDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setMessage("برای استفاده از نقشه باید تنظیمات مکان یابی دستگاه و همچنین gps را فعال کنید ")
        dialog.setPositiveButton("فعال کردن") { _, _ ->
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(myIntent, request_location_setting)
        }
        dialog.show()
    }

    // check if location setting is enabled?
    private fun checkLocationSetting() {
        mLocationSettingsRequest = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build()
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener {
                    Log.e("Location", "success")
                    // location setting in enabled
                    createLocationCallback()
                }.addOnFailureListener {
                    Log.e("Location", "failure")
                    locationSettingDialog()
                    // location setting not enabled
                }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        mMap?.isMyLocationEnabled = true
        mMap?.uiSettings?.isMyLocationButtonEnabled = true
    }

    private fun createLocationCallback() {
        Log.e("loc", "1")
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                Log.e("loc", "2")
                locationResult ?: return
                val x = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                currentPoint = x
                //mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(x, 18f))
                Log.e("Location Update", "location updated!")
            }
        }
        startLocationUpdate()
        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        Log.e("loc", "3")
        if (hasLocationPermission()) {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }


    // stop location Update in onStop ;
    private fun stopLocationUpdate() {
        if (hasLocationPermission()) {
            if (this::mFusedLocationClient.isInitialized && this::mLocationCallback.isInitialized) {
                mFusedLocationClient.removeLocationUpdates(mLocationCallback)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
        t.cancel()
        unregisterReceiver(Receiver);
    }

    // save map center point in center information
    private fun savePoint() {
        val b = AlertDialog.Builder(this)
        b.setMessage("توجه فرمایید که نقطه مشخص شده در مرکز نقشه در بانک اطلاعاتی این مرکز ذخیره می شود")
        b.setPositiveButton("متوجه هستم، ادامه بده") { dialog, which ->
            Log.e("autoId", "${item.addressList!![0]?.autoId}")
            val builder = AlertDialog.Builder(this)
            builder.setView(R.layout.progress_dialog)
            builder.setCancelable(false)
            val alertDialog = builder.create()
            alertDialog.show()
            val lat = centerPoint?.latitude
            val lng = centerPoint?.longitude
            val res = KotlinApiClient.client.create(ApiInterface::class.java)
                    .updateGeoLocation(item.addressList!![0]?.autoId!!, lat!!, lng!!)
            res.enqueue(object : retrofit2.Callback<SimpleResponseModel> {
                override fun onResponse(call: Call<SimpleResponseModel>?, response: Response<SimpleResponseModel>?) {
                    val result = response?.body()
                    if (result?.response == "success") {
                        Toast.makeText(this@DetailActivity, "با موفقیت در اطلاعت مرکز ذخیره شد", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@DetailActivity, "خطا در ذخیره کردن اطلاعات", Toast.LENGTH_LONG).show()
                    }
                    alertDialog.dismiss()
                }

                override fun onFailure(call: Call<SimpleResponseModel>?, t: Throwable?) {
                    alertDialog.dismiss()
                    Toast.makeText(this@DetailActivity, "خطا در ذخیره کردن اطلاعات", Toast.LENGTH_LONG).show()
                }
            })

        }
        b.setNegativeButton("ادامه نده، برگرد") { dialog, which ->
            dialog.dismiss()
        }
        b.show()
    }


    private fun closeMapSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun openMapSheet() {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    private fun checkUserLoggedIn() {
        LoginClass(this, object : TempListener {
            override fun IsOk() {
                Log.e("session", "exist")

                tv_point.visibility = View.GONE
                btn_savePoint.visibility = View.VISIBLE
                iv_marker.visibility = View.VISIBLE

                centerPoint = mMap?.projection?.visibleRegion?.latLngBounds?.center
                tv_point.text = formatLatLng(centerPoint!!)
            }

            override fun IsNotOk() {
                Log.e("session", "not exist")
            }
        })
    }

    private fun initBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(cl_detailMapSheet)
        if (bottomSheetBehavior is CustomBottomSheetBehavior) {
            (bottomSheetBehavior as CustomBottomSheetBehavior).setAllowUserDragging(false)
        }
        bottomSheetBehavior.setBottomSheetCallback(getBottomSheetCallback())

    }

    override fun onCameraMove() {
        centerPoint = mMap?.projection?.visibleRegion?.latLngBounds?.center
        tv_point.text = formatLatLng(centerPoint!!)
    }

    private var centerPoint: LatLng? = null

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap
        ll_detailProgress.visibility = View.GONE

        when (mapType) {
            0 -> {
                googleMap?.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
            1 -> {

                googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
            }
            2 -> {
                googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN


            }
        }
        checkLocationPermissions()
        mMap?.setOnCameraMoveListener(this)
        val p = LatLng(item.addressList!![0]?.latitude?.toDouble()!!, item.addressList!![0]?.longitude?.toDouble()!!)
        mMap?.addMarker(MarkerOptions().title("${item.firstName} ${item.lastName}").position(p))?.showInfoWindow()
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 16f))
        checkUserLoggedIn()
    }

    private fun formatLatLng(p: LatLng): String {
        return "x : " + p.longitude + "\n" + "y : " + p.latitude
    }

    private fun getBottomSheetCallback(): BottomSheetBehavior.BottomSheetCallback {
        bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                Log.e("slide", "$slideOffset")
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        if (mMap == null) {
                            val mapFragment = supportFragmentManager
                                    .findFragmentById(R.id.detailMap) as SupportMapFragment
                            mapFragment.getMapAsync(this@DetailActivity)
                        }
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                }
            }
        }
        return bottomSheetCallback
    }

    private fun saveItem(centerId: Int) {
        realm.executeTransaction { db ->
            val item = db.where(KotlinItemModel::class.java)
                    .equalTo("centerId", centerId)
                    .findFirst()!!
            item.saved = true
        }
    }

    private fun deleteItem(centerId: Int) {
        realm.executeTransaction { db ->
            val item = db.where(KotlinItemModel::class.java)
                    .equalTo("centerId", centerId)
                    .findFirst()!!
            item.saved = false
        }
    }

    private fun checkItemIsSaved(): Boolean {
        var isSaved = false
        realm.executeTransaction { db ->
            val model = db.where(KotlinItemModel::class.java)
                    ?.equalTo("centerId", id)
                    ?.equalTo("saved", true)
                    ?.findAll()!!
            isSaved = model.count() > 0
        }
        return isSaved
    }

    private fun initLists() {
        rv_imageListBig.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_imageListBig.adapter = ImageAdapter()
        list_indicator.attachToRecyclerView(rv_imageListBig)
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(rv_imageListBig)
    }

    private  fun autoScrollSlide() {

        val handler = Handler()

        t = Timer()

        t.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                handler.post {

                    if (forward) {
                        rv_imageListBig.smoothScrollToPosition(n++)
                    } else {
                        rv_imageListBig.smoothScrollToPosition(n--)
                    }
                    if (n == rv_imageListBig.adapter.itemCount-1) {
                        forward = false
                        n=rv_imageListBig.adapter.itemCount-1

                    }
                    if (n == 0) {
                        forward = true

                    }


                }

            }


        }, 1, 4000)


    }

    private fun playOrStop() {
        if (play) {
            play = false
            t.cancel()
        } else {
            play = true
            autoScrollSlide()

        }
        Log.e("ddddd","dddd")
    }


    private fun goToWebSite(){
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tv_website.text.toString().trim()))
       startActivity(intent)
    }
    private fun call(){
        val phone = tv_telephone.text.toString().trim()
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
        startActivity(intent)
    }

    private fun loadDetails() {
        var str = ""

        if (realm.isInTransaction) realm.commitTransaction()

        realm.beginTransaction()

        val g_name = realm.where(KotlinGroupModel::class.java).contains("g_url", g_url).findFirst()?.name!!

        realm.commitTransaction()

        tv_title.text = "${item?.firstName} ${item?.lastName}"




        if (!item.gen.equals("0")!!) {
            if (item.groupId == 1) {
                str = item.levelList!![0]?.name + " _ " + item.specialityList!![0]?.name
            } else {
                str = g_name
            }

        } else {
            str = g_name
        }

        tv_subTitle.text = str
        tv_addr.text = item.addressList!![0]?.locTitle

        tv_telephone.text= item.addressList!![0]?.tel1

      /*  if (!item.addressList!![0]?.tel2.toString().trim().equals("")){
            tv_telephone.text= item.addressList!![0]?.tel1+" - "+item.addressList!![0]?.tel2
        }*/

        tv_cv.text=item.bio
        tv_service.text=item.serviceList

        str=""

        item.cInsuranceList?.forEach { inc->

            str+=inc.name+"\n"+inc.description+"\n\n"

        }
        tv_incurance.text=str

        tv_website.text=item.addressList!![0]?.site
        tv_mail.text=item.addressList!![0]?.mail

        str=""

        if (item.addressList!![0]?.sat_attend!!){
            str="شنبه - "
        }
        if (item.addressList!![0]?.sun_attend!!){
            str+="یکشنبه - "
        }
        if (item.addressList!![0]?.mon_attend!!){
            str+="دوشنبه - "
        }
        if (item.addressList!![0]?.tues_attend!!){
            str+="سه شنبه - "
        }
        if (item.addressList!![0]?.wed_attend!!){
            str+="چهار شنبه - "
        }
        if (item.addressList!![0]?.thurs_attend!!){
            str+="پنجشنبه - "
        }
        if (item.addressList!![0]?.fri_attend!!){
            str+="جمعه - "
        }

        str=str.trim().substring(0,str.length-2)
        tv_attend.text=str


        var drawable = ContextCompat.getDrawable(this@DetailActivity, R.drawable.ic_jin)
        var url = ""

        if (item.logoImg.equals("")) {
            aiv_logoImg.setColorFilter(ContextCompat.getColor(this@DetailActivity, R.color.logoColor), android.graphics.PorterDuff.Mode.SRC_IN)

            if (item.gen?.equals("0")!!) {
                 url = g_url
            } else if (item.gen?.equals("1")!!) {
                url = this@DetailActivity.getString(R.string.ic_doctor_f)
            } else if (item.gen?.equals("2")!!) {
                url = this@DetailActivity.getString(R.string.ic_doctor_m)
            }

        } else {
            aiv_logoImg.setColorFilter(null)
            url = item.logoImg!!
        }

        Glide.with(this@DetailActivity)
                .load(url)
                .apply {
                    RequestOptions()
                            .fitCenter()
                            .placeholder(drawable)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                }
                .into(aiv_logoImg)


        // theBitmap = (aiv_logoImg.drawable as BitmapDrawable).bitmap


    }

    override fun onBackPressed() {
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            if (change) {
                val resultPayload = Intent(this@DetailActivity, OfficeActivity::class.java)
                resultPayload.putExtra("save", isSaved)
                resultPayload.putExtra("centerId", id)
                setResult(Activity.RESULT_OK, resultPayload)
            }
            super.onBackPressed()
        }

    }

    // show list of slides in real size
    inner class ImageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val v = LayoutInflater.from(this@DetailActivity).inflate(R.layout.image_big_item, parent, false)
            return ImageHolder(v)
        }

        override fun getItemCount(): Int {
            return item.slideList?.size!!
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            try {
                Glide.with(this@DetailActivity)
                        .load(item.slideList!![position]?.fileUrl)
                        .apply(RequestOptions()
                                .fitCenter()
                                .placeholder(R.color.colorAccent))
                        .into((holder as ImageHolder)
                                .ivImage)
            } catch (e: Exception) {
                Log.e("glideErr", e.message + " ")
            }
        }

        internal inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            override fun onClick(p0: View?) {
                val i = Intent()
                i.action = getString(R.string.reciver)
                sendBroadcast(i)
            }


            val cvImage = itemView.findViewById<CardView>(R.id.cv_bigItem)
            val ivImage = itemView.findViewById<AppCompatImageView>(R.id.iv_imageBig)

            init {
                itemView.setOnClickListener(this)
            }
        }

    }


    // return true if app has location permission
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // request location permission
    private fun checkLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasLocationPermission()) {
                createLocationRequest()
                // access location
                Toast.makeText(this, "location permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                ActivityCompat.requestPermissions(this
                        , arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        , request_permission)
            }
        }else{
            createLocationRequest()
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == request_permission) {
            if (permissions[0] == android.Manifest.permission.ACCESS_FINE_LOCATION) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("granted", "success")
                    createLocationRequest()
                } else {
                    Log.e("granted", "failed")

                }
            }
        }
    }

}
