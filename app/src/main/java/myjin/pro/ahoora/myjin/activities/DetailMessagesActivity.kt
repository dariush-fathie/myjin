package myjin.pro.ahoora.myjin.activities

import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_messages.*
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.models.KotlinMessagesModel
import myjin.pro.ahoora.myjin.utils.DateConverter

class DetailMessagesActivity : AppCompatActivity(), View.OnClickListener {
    var change = false
    private var isSaved = false
    private var messageId: Int = 1
    private var converter: DateConverter? = null
    private var theBitmap: Bitmap? = null

    private val realm = Realm.getDefaultInstance()!!
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_save -> {
                change = true
                if (isSaved) {

                    val draw = ContextCompat.getDrawable(this@DetailMessagesActivity, R.drawable.ic_bookmark_empty_msg)
                    draw?.setColorFilter(ContextCompat.getColor(this@DetailMessagesActivity, R.color.white), PorterDuff.Mode.SRC_IN)

                    iv_save.setImageDrawable(draw)
                    deleteItem()
                    isSaved = false

                } else {
                    val draw = ContextCompat.getDrawable(this@DetailMessagesActivity, R.drawable.ic_bookmark_fill_msg)
                    draw?.setColorFilter(ContextCompat.getColor(this@DetailMessagesActivity, R.color.white), PorterDuff.Mode.SRC_IN)
                    iv_save.setImageDrawable(draw)
                    saveItem()
                    isSaved = true
                }

                animateBookmark(iv_save)
            }

            R.id.iv_share -> share()

            R.id.iv_goback -> onBackPressed()
        }
    }

    private fun share() {

        val shareIntent = Intent()


        var str = "ژین من (www.MyJin.ir):"
        str += "\n\n"
        str += "${tv_group.text}\n"
        str += "${tv_time.text}\n\n"
        str += "${tv_title.text}... "

        str+="\uD83D\uDC47\n"

        if (realm.isInTransaction) realm.commitTransaction()



        realm.beginTransaction()
        val res = realm.where(KotlinMessagesModel::class.java)
                .equalTo("messageId", messageId)
                .findFirst()!!
        str += res.mainUrl.toString()
        realm.commitTransaction()


        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, str)
      //  var url = ""

        //    if (checkStoragePermissions()) {
          //      theBitmap = (iv_messageImage.drawable as BitmapDrawable).bitmap
            //    url = MediaStore.Images.Media.insertImage(this.contentResolver, theBitmap, "title", "description")
              //  shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
                //shareIntent.type = "image/*"
            //} else {
              //  Toast.makeText(this@DetailMessagesActivity, "جهت پیوست کردن عکس با متن اجازه دستیابی به حافظه دستگاه را بدهید", Toast.LENGTH_LONG).show()
            //}


        startActivity(Intent.createChooser(shareIntent, "Share via"))

    }

    override fun onBackPressed() {
        if (change) {

            var resultPayload = Intent(this@DetailMessagesActivity, MainActivity2::class.java)
            if (tf){
                resultPayload = Intent(this@DetailMessagesActivity, FavMessageActivity::class.java)
            }
            resultPayload.putExtra("save", isSaved)
            resultPayload.putExtra("messageId", messageId)
            resultPayload.putExtra("position", position)
            setResult(AppCompatActivity.RESULT_OK, resultPayload)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    private var position: Int = 0
    private var tf =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)
        if (intent != null) {

            messageId = intent.getIntExtra("messageId", 1)
            position = intent.getIntExtra("position", 1)
            tf=intent.getBooleanExtra("tf",false)
        }
        converter = DateConverter(this@DetailMessagesActivity)


        iv_save.setOnClickListener(this)
        iv_share.setOnClickListener(this)
        iv_goback.setOnClickListener(this)
        fill()
    }

    private fun animateBookmark(view: ImageView) {
        val animation = AnimationSet(true)
        animation.addAnimation(AlphaAnimation(0.0f, 1.0f))
        animation.addAnimation(ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f))
        animation.duration = 400
        animation.repeatMode = Animation.REVERSE
        view.startAnimation(animation)
    }

    private fun saveItem() {
        realm.executeTransaction { db ->
            val item = db.where(KotlinMessagesModel::class.java)
                    .equalTo("messageId", messageId)
                    .findFirst()!!
            item.saved = true
        }
    }

    private fun deleteItem() {
        realm.executeTransaction { db ->
            val item = db.where(KotlinMessagesModel::class.java)
                    .equalTo("messageId", messageId)
                    .findFirst()!!
            item.saved = false
        }
    }

    private fun checkItemIsSaved(): Boolean {
        var isSaved = false
        realm.executeTransaction { db ->
            val model = db.where(KotlinMessagesModel::class.java)
                    ?.equalTo("messageId", messageId)
                    ?.equalTo("saved", true)
                    ?.findAll()!!
            isSaved = model.count() > 0
        }
        return isSaved
    }

    @SuppressLint("SetTextI18n")
    private fun fill() {
        val realm = Realm.getDefaultInstance()
        val drawable = ContextCompat.getDrawable(this@DetailMessagesActivity, R.drawable.pl_ho_intro)
        realm.executeTransaction { db ->
            val Items = db.where(KotlinMessagesModel::class.java).equalTo("messageId", messageId).findFirst()
            tv_title.text = Items?.title
            tv_group.text =Items?.groupName + " ، " + Items?.type
            tv_content.text = Items?.content
            tv_time.text=converter?.convert2(Items?.regDate)

            Glide.with(this@DetailMessagesActivity)
                    .load(Items?.imageUrl)
                    .apply {
                        RequestOptions()
                                .placeholder(drawable)
                    }
                    .into(iv_messageImage)

        }

        isSaved = checkItemIsSaved()
        if (isSaved) {
            val draw = ContextCompat.getDrawable(this@DetailMessagesActivity, R.drawable.ic_bookmark_fill_msg)
            iv_save.setImageDrawable(draw)
        } else {
            val draw = ContextCompat.getDrawable(this@DetailMessagesActivity, R.drawable.ic_bookmark_empty_msg)
            iv_save.setImageDrawable(draw)
        }
    }
    private val rwRequest = 1080
    private val rwPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE
            , Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private fun checkStoragePermissions(): Boolean {
        val write = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val read = ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        rwPermissions,
                        rwRequest
                )
                false
            }
        } else {
            // API < 23
            true
        }
    }
}
