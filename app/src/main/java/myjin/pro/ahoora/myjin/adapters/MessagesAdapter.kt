package myjin.pro.ahoora.myjin.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ir.paad.audiobook.utils.Converter
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.models.KotlinMessagesModel
import java.util.*


class MessagesAdapter(context: Context, list: List<KotlinMessagesModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mContext = context
    val item = list
    private var onlyForFirstTime = true

    val width = Converter.getScreenWidthPx(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(mContext).inflate(R.layout.item_message, parent, false)
        return ImageHolder(v)
    }

    override fun getItemCount(): Int {
        return item.size + 10
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        /*try {
            Glide.with(mContext)
                    .load(item.get(position))
                    .apply(RequestOptions()
                            .fitCenter()
                            .placeholder(R.color.colorAccent))
                    .into((holder as ImageHolder)
                            .ivImage)
        } catch (e: Exception) {
            Log.e("glideErr", e.message + " ")
        }*/
        setAnimation(holder.itemView, position)
    }

    private fun setAnimation(viewToAnimate: View, position: Int) {
        if (onlyForFirstTime) {
            val a = ObjectAnimator.ofFloat(viewToAnimate, "translationX", width.toFloat() / 2, 0f)
            val r = Random()
            val i1 = r.nextInt(300)
            a.duration = i1.toLong()
            a.start()
        }

        if (position == itemCount - 1) {
            onlyForFirstTime = false
        }
    }

    internal inner class ImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        override fun onClick(v: View?) {
            val i = Intent()
            i.action = mContext.getString(R.string.reciver)
            mContext.sendBroadcast(i)
        }

        /*private val cvImage: CardView = itemView.findViewById(R.id.cv_bigItem)
        val ivImage: AppCompatImageView = itemView.findViewById(R.id.iv_imageBig)

        init {
            itemView.setOnClickListener(this)
            val layoutParams = cvImage.layoutParams
            layoutParams.width = width - (0.30 * width).toInt()
            cvImage.layoutParams = layoutParams
        }*/
    }

}
