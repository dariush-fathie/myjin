package myjin.pro.ahoora.myjin.adapters


import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.activities.DetailActivity
import myjin.pro.ahoora.myjin.activities.NoDetailActivity
import myjin.pro.ahoora.myjin.models.KotlinGroupModel
import myjin.pro.ahoora.myjin.models.KotlinItemModel
import myjin.pro.ahoora.myjin.utils.StaticValues

class GroupItemSaveAdapter(ctx: Context, idList: ArrayList<Int>?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val context = ctx
    private val ids = idList
    private var realm: Realm = Realm.getDefaultInstance()
    private var g_url = ""
    private var g_name = ""
    private var active2 = 1;

    private fun getModelByCenterId(centerId: Int): KotlinItemModel {
        var item = KotlinItemModel()
        realm.executeTransaction { db ->
            item = db.where(KotlinItemModel::class.java)
                    .equalTo("centerId", centerId)
                    .findFirst()!!
            g_name = db.where(KotlinGroupModel::class.java).equalTo("groupId", item.groupId).findFirst()?.name!!
            g_url = db.where(KotlinGroupModel::class.java).equalTo("groupId", item.groupId).findFirst()?.g_url!!

        }
        return item
    }

    private fun deleteTab() {

        val i = Intent()
        i.action = "myjin.pro.ahoora.myjin"
        context.sendBroadcast(i)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.group_item, parent, false)
        return ItemHolder(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder as ItemHolder
        val item = getModelByCenterId(ids!![position])

        try {
            holder.title.text = "${item?.firstName} ${item?.lastName}"

            var str = ""
            str = if (!item.gen.equals("0")) {
                if (item.groupId == 1) {
                    item.levelList!![0]?.name + " _ " + item.specialityList!![0]?.name
                } else {
                    g_name
                }
            } else {
                g_name
            }

            holder.subTitle.text = str
            holder.tv_addr.text = item.addressList!![0]?.locTitle


            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_jin)
            var url = ""

            holder.image.colorFilter = null

            if (item.logoImg.equals("")) {

                if (item.gen?.equals("0")!!) {
                    holder.image.setColorFilter(ContextCompat.getColor(context, R.color.mc_icon_color), android.graphics.PorterDuff.Mode.SRC_IN)
                    url = g_url
                } else if (item.gen?.equals("1")!!) {

                    url = context.getString(R.string.ic_doctor_f)
                } else if (item.gen?.equals("2")!!) {

                    url = context.getString(R.string.ic_doctor_m)
                }

            } else {

                url = item.logoImg!!
            }

            Glide.with(context)
                    .load(url)
                    .apply {
                        RequestOptions()

                                .placeholder(drawable)
                    }
                    .into(holder.image)
        } catch (e: Exception) {
            Log.e("SaveAdapter", e.message + " ")
        }
    }

    override fun getItemCount(): Int {
        val s = ids?.size
        return if (s == 0) {
            deleteTab()
            0
        } else {
            s!!
        }
    }

    fun deleteItem(centerId: Int, position: Int) {
        realm.executeTransaction { db ->
            val item = db.where(KotlinItemModel::class.java)
                    .equalTo("centerId", centerId)
                    .findFirst()!!
            item.saved = false
        }

        ids?.remove(centerId)
        notifyItemRemoved(position)

        if (ids?.size == 0) {
            deleteTab()
        }
    }

    private fun getG_name(i: Int) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        val item = realm.where(KotlinItemModel::class.java)
                .equalTo("centerId", i)
                .findFirst()!!
        active2 = item.active2
        g_name = realm.where(KotlinGroupModel::class.java).equalTo("groupId", item.groupId).findFirst()?.name!!
        g_url = realm.where(KotlinGroupModel::class.java).equalTo("groupId", item.groupId).findFirst()?.g_url!!
        realm.commitTransaction()
    }

    internal inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.cl_item -> {

                    getG_name(ids?.get(adapterPosition)!!)

                    if (active2!=0) {

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val options = ActivityOptionsCompat.makeSceneTransitionAnimation((context as AppCompatActivity), image, "transition_name")
                            val i = Intent(context, DetailActivity::class.java)
                            i.putExtra(StaticValues.MODEL, 1)
                            i.putExtra(StaticValues.ID, ids.get(adapterPosition))
                            i.putExtra("g_url", g_url)
                            context.startActivity(i, options.toBundle())
                        } else {
                            val i = Intent(context, DetailActivity::class.java)
                            i.putExtra(StaticValues.MODEL, 1)
                            i.putExtra(StaticValues.ID, ids.get(adapterPosition))
                            i.putExtra("g_url", g_url)
                            context.startActivity(i)
                        }
                    }else{
                        val i = Intent(context, NoDetailActivity::class.java)
                        context.startActivity(i)
                    }
                }
                R.id.iv_starLike -> {
                    deleteItem(ids!![adapterPosition], adapterPosition)
                }
            }

        }

        val title: AppCompatTextView = itemView.findViewById(R.id.tv_title)
        val subTitle: AppCompatTextView = itemView.findViewById(R.id.tv_subTitle)
        val tv_addr: AppCompatTextView = itemView.findViewById(R.id.tv_addr)
        val item: ConstraintLayout = itemView.findViewById(R.id.cl_item)
        val ivDelete: AppCompatImageView = itemView.findViewById(R.id.iv_starLike)
        val image: CircleImageView = itemView.findViewById(R.id.iv_itemImage)

        init {
            item.setOnClickListener(this)
            ivDelete.setImageResource(R.drawable.ic_trash)
            ivDelete.setOnClickListener(this)
        }
    }


}
