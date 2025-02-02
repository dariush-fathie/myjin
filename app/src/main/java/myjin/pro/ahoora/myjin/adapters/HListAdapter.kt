package myjin.pro.ahoora.myjin.adapters;

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
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
import myjin.pro.ahoora.myjin.activities.OfficeActivity
import myjin.pro.ahoora.myjin.models.KotlinGroupModel
import myjin.pro.ahoora.myjin.models.KotlinItemModel
import myjin.pro.ahoora.myjin.utils.StaticValues

class HListAdapter(ctx: Context, array: ArrayList<Int>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val context = ctx
    val idArray = array
    private var g_url = ""
    private var g_name = ""

    val realm: Realm = Realm.getDefaultInstance()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v: View = LayoutInflater.from(context).inflate(R.layout.map_bottom_list_item, parent, false)
        return ItemHolder(v)
    }

    override fun getItemCount(): Int {
        return idArray.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        realm.beginTransaction()
        val item = realm.where(KotlinItemModel::class.java).equalTo("centerId", idArray.get(position)).findFirst()
        g_name = realm.where(KotlinGroupModel::class.java).equalTo("groupId", item?.groupId).findFirst()?.name!!
        g_url = realm.where(KotlinGroupModel::class.java).equalTo("groupId", item?.groupId).findFirst()?.g_url!!
        realm.commitTransaction()
        (holder as ItemHolder).title.text = item?.firstName + " " + item?.lastName
        var str = ""
        if (!item!!.gen.equals("0")) {
            if (item.groupId == 1) {
                str = item.levelList!![0]?.name + " _ " + item.specialityList!![0]?.name
            } else {
                str = g_name
            }

        } else {
            str = g_name
        }

        holder.subTitle.text = str

        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_jin)
        var url = ""
        holder.image.colorFilter = null

        if (item.logoImg.equals("")) {

            if (item.gen?.equals("0")!!) {
                holder.image.setColorFilter(ContextCompat.getColor(context, R.color.mc_icon_color),android.graphics.PorterDuff.Mode.SRC_IN)
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
                            .fitCenter()
                            .placeholder(drawable)
                }
                .into(holder.image)
    }

    fun getModelByCenterId(centerId: Int): KotlinItemModel {
        var item = KotlinItemModel()
        realm.executeTransaction { db ->
            item = db.where(KotlinItemModel::class.java)
                    .equalTo("centerId", centerId)
                    .findFirst()!!
        }
        return item
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        val image: CircleImageView = itemView.findViewById(R.id.iv_bottomListImage)
        val title: AppCompatTextView = itemView.findViewById(R.id.tv_bottomListTitle)
        val subTitle: AppCompatTextView = itemView.findViewById(R.id.tv_bottomListSubTitle)
        override fun onClick(v: View?) {
            val item = getModelByCenterId(idArray[adapterPosition])
            if (item.active2 != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val options = ActivityOptionsCompat.makeSceneTransitionAnimation((context as OfficeActivity),
                            image, "transition_name")

                    val i = Intent(context, DetailActivity::class.java)
                    i.putExtra(StaticValues.MODEL, 0)
                    i.putExtra(StaticValues.ID, idArray[adapterPosition])
                    i.putExtra("g_url", g_url)

                    context.startActivity(i,  options.toBundle())


                } else {
                    val i = Intent(context, DetailActivity::class.java)
                    i.putExtra(StaticValues.MODEL, 0)
                    i.putExtra(StaticValues.ID, idArray[adapterPosition])
                    i.putExtra("g_url", g_url)

                    (context as OfficeActivity).startActivity(i)

                }
            }else{
                val j = Intent(context, NoDetailActivity::class.java)
                (context as OfficeActivity).startActivity(j)
            }
        }

        init {

            itemView.setOnClickListener(this)
        }
    }

}