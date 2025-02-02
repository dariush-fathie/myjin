package myjin.pro.ahoora.myjin.customClasses

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.ThumbnailUtils
import androidx.appcompat.widget.AppCompatImageView
import android.util.DisplayMetrics
import android.view.View
import android.widget.LinearLayout
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import myjin.pro.ahoora.myjin.R


class Clusters : ClusterItem {
    private val mPosition: LatLng
    var mTitle: String = ""
    var mSnippet: String = ""
    lateinit var icon: Bitmap
    private lateinit var mContext: Context

    constructor(context: Context, lat: Double, lng: Double) {
        mPosition = LatLng(lat, lng)

    }

    constructor(context: Context, lat: Double, lng: Double, title: String, snippet: String) {

        mPosition = LatLng(lat, lng)
        mContext = context
        mTitle = title
        mSnippet = snippet

        val markerView = View.inflate(context, R.layout.map_marker, null)
        var ivm = markerView.findViewById<AppCompatImageView>(R.id.iv_marker)
       // ivm.setImageBitmap(getBitmap("http://myjin.ir/photos/ic_28.png"))

        icon = createDrawableFromView(markerView)
    }


    override fun getPosition(): LatLng {


        return mPosition
    }

    private fun createDrawableFromView(view: View): Bitmap {
        val displayMetrics = DisplayMetrics()
        (mContext as AppCompatActivity).windowManager.defaultDisplay
                .getMetrics(displayMetrics)
        view.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels)
        view.layout(0, 0, displayMetrics.widthPixels,
                displayMetrics.heightPixels)
        val bitmap = Bitmap.createBitmap(view.measuredWidth,
                view.measuredHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        view.draw(canvas)


        val THUMBSIZE = 96

        return ThumbnailUtils.extractThumbnail(bitmap,
                THUMBSIZE, THUMBSIZE)

        return bitmap
    }


}
