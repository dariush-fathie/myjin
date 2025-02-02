package myjin.pro.ahoora.myjin.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.tabs.TabLayout
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import de.hdodenhof.circleimageview.CircleImageView
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_search.*
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.customClasses.SimpleItemDecoration
import myjin.pro.ahoora.myjin.models.KotlinGroupModel
import myjin.pro.ahoora.myjin.models.KotlinItemModel
import myjin.pro.ahoora.myjin.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class SearchActivity : AppCompatActivity(), View.OnClickListener, TabLayout.OnTabSelectedListener {

    private var g_url = ""
    private var g_name = ""
    private var active2 = 1

    // holding the pair of <groupId,count> for use in adapter
    private val gIdCountPair = ArrayList<Pair<Int, Int>>()
    // list of all ids in result
    private val idsArray = ArrayList<Int>()
    // list of unique id in result file
    private val uniqueIds = ArrayList<Int>()

    private var resultList: List<KotlinItemModel>? = null

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.fab_goUp -> rv_search.smoothScrollToPosition(0)
            R.id.iv_search -> search()
        }
    }

    override fun onTabReselected(tab: TabLayout.Tab?) {

    }

    override fun onTabUnselected(tab: TabLayout.Tab?) {

    }


    override fun onStart() {
        super.onStart()
        if (idsArray.size==0)
       showShimmer()
    }

    override fun onTabSelected(tab: TabLayout.Tab?) {
        if (tab?.position == 0) {

            loadAdapter(resultList!!)
            Toast.makeText(this@SearchActivity, "${resultList!!.size} مورد یافت شد .", Toast.LENGTH_LONG).show()

        } else {
            val tempRes = ArrayList<KotlinItemModel>()
            resultList?.forEach { item: KotlinItemModel ->
                if (item.groupId == uniqueIds.get((tab?.position!!) - 1)) {
                    tempRes.add(item)
                }
            }
            loadAdapter(tempRes)
            Toast.makeText(this@SearchActivity, "${tempRes.size} مورد یافت شد .", Toast.LENGTH_LONG).show()

        }
    }



    private fun search() {
        if (et_search.text.toString() != "") {
            getItems()
            try {
                Utils.closeKeyBoard(et_search.windowToken, this@SearchActivity)
            }catch (e:IllegalStateException){

            }

        } else {
            Toast.makeText(this@SearchActivity, R.string.lyervk, Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTab() {
        val drawable = ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_all)
        drawable?.setColorFilter(ContextCompat.getColor(this@SearchActivity, R.color.green), PorterDuff.Mode.SRC_IN)
        ctb.addTab(ctb.newTab().setText("همه").setIcon(drawable))

        uniqueIds.forEach { groupId: Int ->

            ctb.addTab(ctb.newTab().setText(getTitleFromDb(groupId)))

        }
    }

    private fun getTitleFromDb(groupId: Int): String {
        var name = ""
        val realm = Realm.getDefaultInstance()
        realm.executeTransaction { realmDatabase: Realm? ->
            name = realmDatabase?.where(KotlinGroupModel::class.java)?.equalTo("groupId", groupId)?.findFirst()?.name!!
        }
        return name
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val value = intent.getStringExtra("sVal")

        et_search.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (et_search.text.toString() != "") {
                    getItems()
                    Utils.closeKeyBoard(et_search.windowToken, this@SearchActivity)
                } else {
                    Toast.makeText(this@SearchActivity, getString(R.string.lyervk), Toast.LENGTH_SHORT).show()
                }
            }
            false
        }
        iv_search.setOnClickListener(this)
        fab_goUp.setOnClickListener(this)
        ctb.addOnTabSelectedListener(this)

        et_search.setText(value)
        search()
    }


    private fun getItems() {
        if (Utils.isNetworkAvailable(this@SearchActivity)) {
            downloadItem()
        } else {
            Toast.makeText(this@SearchActivity, getString(R.string.checkYourConnection), Toast.LENGTH_LONG).show()

        }

    }

    private fun downloadItem() {

        val sp = SharedPer(this@SearchActivity)
        val provId = sp.getInteger(getString(R.string.provId))
        var cityId = sp.getInteger(getString(R.string.cityId))

        if (provId == 19) {
            if (cityId==19)
            cityId = 0
        }

        cpv_progress.visibility = View.VISIBLE
        KotlinApiClient.client.create(ApiInterface::class.java).search(et_search.text.toString(), provId, cityId).enqueue(object : Callback<List<KotlinItemModel>> {
            override fun onResponse(call: Call<List<KotlinItemModel>>?, response: Response<List<KotlinItemModel>>?) {
                resultList = response?.body()
                ctb?.removeAllTabs()
                if (resultList?.size!! > 0) {

                    Toast.makeText(this@SearchActivity, "${resultList!!.size} مورد یافت شد .", Toast.LENGTH_LONG).show()
                    gIdCountPair.clear()
                    idsArray.clear()
                    uniqueIds.clear()

                    resultList!!.forEach { model: KotlinItemModel ->

                        //collect all ids for cound later
                        idsArray.add(model.groupId)
                        // adding unique id to list
                        if (!uniqueIds.contains(model.groupId)) {
                            uniqueIds.add(model.groupId)

                        }
                    }

                    // counting unique ids and put into gIdCountPairs list
                    for (i in 0 until uniqueIds.size) {
                        val gId = uniqueIds[i]
                        var count = 0
                        for (j in 0 until idsArray.size) {
                            if (idsArray[j] == gId) {
                                count++
                            }
                        }
                        gIdCountPair.add(Pair(gId, count))
                    }

                    gIdCountPair.forEach { pair: Pair<Int, Int> ->
                        Log.e("gid: ${pair.first}", "count ${pair.second}")
                    }

                    addTab()
                    /* loadAdapter(resultList!!)*/
                } else {

                    Toast.makeText(this@SearchActivity, "متاسفانه گزینه ای یافت نشد ", Toast.LENGTH_LONG).show()

                    rv_search.adapter = null

                }
                cpv_progress.visibility = View.GONE
                view_shadow.visibility = View.VISIBLE


            }

            override fun onFailure(call: Call<List<KotlinItemModel>>?, t: Throwable?) {
                view_shadow.visibility = View.GONE
                Toast.makeText(this@SearchActivity, "خطا", Toast.LENGTH_SHORT).show()

                Log.e("search", t.toString())
                cpv_progress.visibility = View.GONE
            }
        })

    }


    private fun showShimmer(){
        shimmer.startShimmer()
        shimmer.visibility=View.VISIBLE
    }

    private fun stopShimmer(){
        shimmer.stopShimmer()
        shimmer.visibility=View.GONE

    }

    fun loadAdapter(data: List<KotlinItemModel>) {

        stopShimmer()

        rv_search.layoutManager = LinearLayoutManager(this)
        while (rv_search.itemDecorationCount > 0) {
            rv_search.removeItemDecorationAt(0)
        }
        val decor = SimpleItemDecoration(this, 10)
        rv_search.addItemDecoration(decor)
        rv_search.adapter = SearchAdapterWithHeader(data)

        rv_search.clearOnScrollListeners()
        rv_search.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val i = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                if (i > 8) {
                    (fab_goUp as View).visibility = View.VISIBLE
                } else {
                    (fab_goUp as View).visibility = View.GONE
                }
            }
        })

    }

    companion object {
        lateinit var tempModel: KotlinItemModel
    }

    private fun getG_name(i: Int) {
        val realm = Realm.getDefaultInstance()
        realm.beginTransaction()
        g_name = realm.where(KotlinGroupModel::class.java).equalTo("groupId", i).findFirst()?.name!!
        g_url = realm.where(KotlinGroupModel::class.java).equalTo("groupId", i).findFirst()?.g_url!!
        realm.commitTransaction()
    }


    inner class SearchAdapterWithHeader(data: List<KotlinItemModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val dataSet = data.sortedWith(compareBy { it.firstName })
        override fun getItemCount(): Int {

            return dataSet.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemHolder(LayoutInflater.from(this@SearchActivity).inflate(R.layout.group_item, parent, false))
        }


        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

            holder as ItemHolder

            holder.title.text = dataSet[position].firstName + " " + dataSet[position].lastName

            getG_name(dataSet[position].groupId)
            var str = ""
            if (!dataSet[position].gen.equals("0")) {
                if (dataSet[position].groupId == 1) {
                    str = dataSet[position].levelList!![0]?.name + " _ " + dataSet[position].specialityList!![0]?.name
                } else {
                    str = g_name
                }

            } else {
                str = g_name
            }

            holder.subTitle.text = str
            holder.tv_addr.text = dataSet[position].addressList!![0]?.locTitle


            val drawable = ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_jin)
            var url = ""
            holder.image.setColorFilter(null)

            if (dataSet[position].logoImg.equals("")) {

                if (dataSet[position].gen?.equals("0")!!) {
                    holder.image.setColorFilter(ContextCompat.getColor(this@SearchActivity, R.color.mc_icon_color), android.graphics.PorterDuff.Mode.SRC_IN)
                    url = g_url
                } else if (dataSet[position].gen?.equals("1")!!) {

                    url = this@SearchActivity.getString(R.string.ic_doctor_f)
                } else if (dataSet[position].gen?.equals("2")!!) {
//                        holder.image.background = ContextCompat.getDrawable(this@SearchActivity, R.drawable.t)
                    url = this@SearchActivity.getString(R.string.ic_doctor_m)
                }

            } else {

                url = dataSet[position].logoImg!!
            }

            Glide.with(this@SearchActivity)
                    .load(url)
                    .apply {
                        RequestOptions()

                                .placeholder(drawable)
                    }
                    .into(holder.image)

        }


        inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            override fun onClick(v: View?) {
                tempModel = dataSet[adapterPosition]
                getG_name(tempModel.groupId)
                active2 = tempModel.active2

                if (active2 != 0) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this@SearchActivity, image, "transition_name")

                        val i = Intent(this@SearchActivity, DetailActivity::class.java)
                        i.putExtra(StaticValues.MODEL, 2)
                        i.putExtra(StaticValues.ID, tempModel.centerId)
                        i.putExtra("g_url", g_url)
                        startActivity(i, options.toBundle())

                    } else {
                        val i = Intent(this@SearchActivity, DetailActivity::class.java)
                        i.putExtra(StaticValues.MODEL, 2)
                        i.putExtra(StaticValues.ID, tempModel.centerId)
                        i.putExtra("g_url", g_url)
                        startActivity(i)
                    }
                } else {
                    val j = Intent(this@SearchActivity, NoDetailActivity::class.java)
                    startActivity(j)
                }
            }

            val title: AppCompatTextView = itemView.findViewById(R.id.tv_title)
            val subTitle: AppCompatTextView = itemView.findViewById(R.id.tv_subTitle)
            val tv_addr: AppCompatTextView = itemView.findViewById(R.id.tv_addr)
            val item: ConstraintLayout = itemView.findViewById(R.id.cl_item)
            val ivStar: AppCompatImageView = itemView.findViewById(R.id.iv_starLike)
            val image: CircleImageView = itemView.findViewById(R.id.iv_itemImage)

            init {
                item.setOnClickListener(this)
                ivStar.visibility = View.GONE
            }
        }

    }


}
