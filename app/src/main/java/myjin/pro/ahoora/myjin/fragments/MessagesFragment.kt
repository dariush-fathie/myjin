package myjin.pro.ahoora.myjin.fragments

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import io.realm.Case
import io.realm.Realm
import io.realm.RealmResults
import io.realm.Sort
import kotlinx.android.synthetic.main.fragment_messages.*
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.activities.MainActivity2
import myjin.pro.ahoora.myjin.adapters.MessagesAdapter
import myjin.pro.ahoora.myjin.adapters.FormatVideoPopupAdapter
import myjin.pro.ahoora.myjin.customClasses.MsgSpinnerDialog
import myjin.pro.ahoora.myjin.customClasses.VerticalLinearLayoutDecoration
import myjin.pro.ahoora.myjin.interfaces.OnSpinnerItemSelected
import myjin.pro.ahoora.myjin.interfaces.SendIntentForResult
import myjin.pro.ahoora.myjin.models.KotlinMessagesModel
import myjin.pro.ahoora.myjin.models.events.SearchMEvent
import myjin.pro.ahoora.myjin.models.events.TestEvent
import myjin.pro.ahoora.myjin.models.events.VisibilityEvent
import myjin.pro.ahoora.myjin.models.formatVideoPopupModel
import myjin.pro.ahoora.myjin.utils.ApiInterface
import myjin.pro.ahoora.myjin.utils.KotlinApiClient
import myjin.pro.ahoora.myjin.utils.NetworkUtil
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MessagesFragment : Fragment(), View.OnClickListener {


    private lateinit var res: RealmResults<KotlinMessagesModel>

    private var loadFlag = false

    private var realm: Realm = Realm.getDefaultInstance()
    val typesArray = ArrayList<String>()
    val sourceArray = ArrayList<String>()
    val idT = ArrayList<Int>()
    val idS = ArrayList<Int>()
    var posS = 0
    var posT = 0


    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_messagesTryAgain -> {
                tryAgain()
            }
            R.id.cv1 -> openSourceDialog()
            R.id.cv2 -> openTypeDialog()
            R.id.cv3->showListPopupWindow(cv3)
        }
    }

    override fun onStart() {
        super.onStart()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {

        EventBus.getDefault().unregister(this)
        super.onStop()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_messagesTryAgain.setOnClickListener(this)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            if (requestCode == (rv_messages.adapter as MessagesAdapter).requestCode) {
                data ?: return
                val p = data.getIntExtra("position", 1)
                val mark = data.getBooleanExtra("save", false)
                (rv_messages.adapter as MessagesAdapter).mark(p, mark)
            }
        }
    }

    @Subscribe
    fun notifyAdapter(t: TestEvent) {

        if (realm.isClosed) {
            realm = Realm.getDefaultInstance()
        }
        Handler().postDelayed({
            realm.executeTransactionAsync { db ->
                val query = db.where(KotlinMessagesModel::class.java)
                if (posS > 0) {
                    query.equalTo("groupId", posS)
                }
                if (posT > 0) {
                    query.equalTo("typeId", posT)
                }
                val res = query.findAll()

                val itemsList = ArrayList<KotlinMessagesModel>()
                res.forEach { item ->
                    val i = KotlinMessagesModel()
                    i.title = item.title
                    i.imageUrl = item.imageUrl
                    i.regDate = item.regDate
                    i.bgColor = item.bgColor
                    i.priority = item.priority
                    i.content = item.content
                    i.shortDescription = item.shortDescription
                    i.groupName = item.groupName
                    i.groupId = item.groupId
                    i.messageId = item.messageId
                    i.saved = item.saved
                    i.type = item.type
                    i.typeId = item.typeId
                    itemsList.add(i)
                }

                activity?.runOnUiThread {
                    loadAdapter(itemsList)
                }

            }
        }, 200)

    }


    private fun loadTabsAndSpinner() {
        cv1.visibility = View.VISIBLE
        cv2.visibility = View.VISIBLE
       // cv3.visibility = View.VISIBLE
        cv1.setOnClickListener(this)
        cv2.setOnClickListener(this)
        cv3.setOnClickListener(this)
    }


    private fun openSourceDialog() {
        val dialog = MsgSpinnerDialog(activity as MainActivity2, sourceArray, getString(myjin.pro.ahoora.myjin.R.string.mmnrek))
        dialog.setOnSpinnerItemSelectedListener(object : OnSpinnerItemSelected {
            override fun onClick(name: String, position: Int) {
                posS = idS[position]
                spinner_sources.text = name

                if (!realm.isInTransaction) {
                    realm.beginTransaction()

                    res = if (idS[position] > 0) {
                        if (posT > 0) {
                            realm.where(KotlinMessagesModel::class.java).equalTo("typeId", posT).and().equalTo("groupId", idS.get(position)).findAll()
                        } else {
                            realm.where(KotlinMessagesModel::class.java).equalTo("groupId", idS.get(position)).findAll()
                        }

                    } else {
                        if (posT > 0) {
                            realm.where(KotlinMessagesModel::class.java).equalTo("typeId", posT).findAll()
                        } else {
                            realm.where(KotlinMessagesModel::class.java).findAll()
                        }
                    }

                    res = res.sort("regDate", Sort.DESCENDING)
                    realm.commitTransaction()

                    val list = ArrayList<KotlinMessagesModel>()

                    res.forEach { ii ->
                        list.add(ii)
                    }

                    Log.e("rrr", posT.toString() + "  " + idS.get(position))
                    loadAdapter(list)
                }
            }
        })
        dialog.show()
    }

    private fun openTypeDialog() {
        val dialog = MsgSpinnerDialog(activity as MainActivity2, typesArray, getString(R.string.dbmnrek))
        dialog.setOnSpinnerItemSelectedListener(object : OnSpinnerItemSelected {
            @SuppressLint("SetTextI18n")
            override fun onClick(name: String, position: Int) {
                posT = idT.get(position)
                spinner_types.text = name

                if (!realm.isInTransaction) {
                    realm.beginTransaction()

                    res = if (idT[position] > 0) {
                        if (posS > 0) {
                            realm.where(KotlinMessagesModel::class.java).equalTo("typeId", idT.get(position)).and().equalTo("groupId", posS).findAll()
                        } else {
                            realm.where(KotlinMessagesModel::class.java).equalTo("typeId", idT.get(position)).findAll()
                        }

                    } else {
                        if (posS > 0) {
                            realm.where(KotlinMessagesModel::class.java).equalTo("groupId", posS).findAll()
                        } else {
                            realm.where(KotlinMessagesModel::class.java).findAll()
                        }
                    }

                    res = res.sort("regDate", Sort.DESCENDING)
                    realm.commitTransaction()

                    val list = ArrayList<KotlinMessagesModel>()

                    res.forEach { ii ->
                        list.add(ii)
                    }

                    Log.e("rrr", posS.toString() + "  " + idT.get(position))
                    loadAdapter(list)
                }
            }
        })
        dialog.show()
    }

    private fun showListPopupWindow(anchor: View) {
        val formatVideoPopupModels = ArrayList<formatVideoPopupModel>()
        formatVideoPopupModels.add(formatVideoPopupModel("همه", R.drawable.ic_format_all))
        formatVideoPopupModels.add(formatVideoPopupModel("انیمیشن", R.drawable.ic_format_animation))
        formatVideoPopupModels.add(formatVideoPopupModel("فیلم و ویدئو", R.drawable.ic_format_video))
        formatVideoPopupModels.add(formatVideoPopupModel("اینفوگرافیک", R.drawable.ic_format_infographic))
        formatVideoPopupModels.add(formatVideoPopupModel("عکس", R.drawable.ic_format_photo))
        formatVideoPopupModels.add(formatVideoPopupModel("پادکست (صوتی)   ", R.drawable.ic_format_podcast))
        formatVideoPopupModels.add(formatVideoPopupModel("متن و مقاله", R.drawable.ic_format_text))

        val listPopupWindow = createListPopupWindow(anchor,   formatVideoPopupModels)
        listPopupWindow.setOnItemClickListener { parent, view, position, id ->
            listPopupWindow.dismiss()
            Toast.makeText(activity as Context, "clicked at $position", Toast.LENGTH_SHORT)
                    .show()
        }
        listPopupWindow.show()
    }

    private fun createListPopupWindow(anchor: View, items: List<formatVideoPopupModel>): ListPopupWindow {
        val popup = ListPopupWindow(activity as Context)
        val adapter = FormatVideoPopupAdapter(items)

        popup.verticalOffset = resources.getDimension(R.dimen.pop_up_window_margin_top).toInt()
        popup.anchorView = anchor
        popup.width = measureContentWidth(adapter)
        popup.setAdapter(adapter)
        return popup
    }

    private fun measureContentWidth(listAdapter: BaseAdapter): Int {
        var mMeasureParent: ViewGroup? = null
        var maxWidth = 0
        var itemView: View? = null
        var itemType = 0

        val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val count = listAdapter.getCount()
        for (i in 0 until count) {
            val positionType = listAdapter.getItemViewType(i)
            if (positionType != itemType) {
                itemType = positionType
                itemView = null
            }

            if (mMeasureParent == null) {
                mMeasureParent = FrameLayout(activity as Context)
            }

            itemView = listAdapter.getView(i, itemView, mMeasureParent)
            itemView!!.measure(widthMeasureSpec, heightMeasureSpec)

            val itemWidth = itemView.measuredWidth

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth
            }
        }

        return Math.max(maxWidth, resources.getDimension(R.dimen.pop_up_window_min_width).toInt())
    }

    private var lock = false

    private fun getMessages() {
        if (!lock) {
            lock = true
            hideErrLayout()
            showCPV()
            val apiInterface = KotlinApiClient.client.create(ApiInterface::class.java)
            val response = apiInterface.messages
            response.enqueue(object : Callback<List<KotlinMessagesModel>> {

                override fun onResponse(call: Call<List<KotlinMessagesModel>>?, response: Response<List<KotlinMessagesModel>>?) {
                    response?.body() ?: onFailure(call, Throwable("null body"))
                    response?.body() ?: return

                    val result = response.body()

                    result ?: onFailure(call, Throwable("null list"))
                    result ?: return

                    realm.executeTransactionAsync { realm: Realm? ->

                        val savedItem = realm?.where(KotlinMessagesModel::class.java)
                                ?.equalTo("saved", true)
                                ?.findAll()
                        val savedItemIds = ArrayList<Int>()
                        savedItem?.forEach { model: KotlinMessagesModel? ->
                            savedItemIds.add(model?.messageId!!)
                        }

                        realm?.where(KotlinMessagesModel::class.java)
                                ?.equalTo("saved", false)
                                ?.findAll()
                                ?.deleteAllFromRealm()

                        result.forEach { kotlinItemModel: KotlinMessagesModel ->
                            if (savedItemIds.contains(kotlinItemModel.messageId)) {
                                kotlinItemModel.saved = true
                            }
                            realm?.copyToRealmOrUpdate(kotlinItemModel)
                        }
                    }

                    sourceArray.clear()
                    typesArray.clear()
                    idS.clear()
                    idT.clear()

                    sourceArray.add("همه")
                    typesArray.add("همه")

                    idT.add(0)
                    idS.add(0)

                    val list = ArrayList<KotlinMessagesModel>()
                    result.forEach { item: KotlinMessagesModel ->
                        list.add(item)
                        if (!sourceArray.contains(item.groupName)) {
                            sourceArray.add(item.groupName)
                            idS.add(item.groupId)
                        }
                        if (!typesArray.contains(item.type)) {
                            typesArray.add(item.type)
                            idT.add(item.typeId)
                        }
                    }


                    loadTabsAndSpinner()
                    loadAdapter(list)

                    loadFlag = true

                    hideCPV()
                    hideErrLayout()
                    lock = false

                }

                override fun onFailure(call: Call<List<KotlinMessagesModel>>?, t: Throwable?) {
                    Log.e("Messages", "${t?.message} - error")
                    loadFlag = false
                    showErrLayout()
                    hideCPV()
                    lock = false
                }
            })
        }
    }

    private fun tryAgain() {
        hideErrLayout()
        if (NetworkUtil().isNetworkAvailable(activity as Context)) {
            getMessages()
        } else {
            showErrLayout()
        }
    }

    private fun showErrLayout() {
        tv_MessagesText.visibility = View.VISIBLE
        btn_messagesTryAgain.visibility = View.VISIBLE
        hideCPV()
    }

    private fun hideErrLayout() {
        tv_MessagesText.visibility = View.GONE
        btn_messagesTryAgain.visibility = View.GONE
    }

    private fun showCPV() {
        rl_messages.visibility = View.VISIBLE
    }

    private fun hideCPV() {
        rl_messages.visibility = View.GONE
    }

    @Subscribe
    fun onBecomeVisible(e: VisibilityEvent) {
        if (e.position == 1) {

            if (!loadFlag) {
                if (NetworkUtil().isNetworkAvailable(activity as Context)) {
                    getMessages()
                } else {
                    showErrLayout()
                }
            }
        }
    }

    @Subscribe
    fun searchInToMessages(v: SearchMEvent) {

        val value = v.value
        val pos = v.pos
        if (pos == 1) {
            if (value != "") {
                realm.beginTransaction()
                val res = realm.where(KotlinMessagesModel::class.java)
                        ?.contains("title", value, Case.INSENSITIVE)?.or()?.contains("content", value, Case.INSENSITIVE)?.findAll()
                res?.sort("regDate", Sort.DESCENDING)
                realm.commitTransaction()

                sourceArray.clear()
                typesArray.clear()
                idS.clear()
                idT.clear()

                sourceArray.add("همه")
                typesArray.add("همه")

                idT.add(0)
                idS.add(0)

                val list = ArrayList<KotlinMessagesModel>()

                res?.forEach { item: KotlinMessagesModel ->
                    list.add(item)
                    if (!sourceArray.contains(item.groupName)) {
                        sourceArray.add(item.groupName)
                        idS.add(item.groupId)
                    }
                    if (!typesArray.contains(item.type)) {
                        typesArray.add(item.type)
                        idT.add(item.typeId)
                    }
                }

                loadTabsAndSpinner()
                loadAdapter(list)

                loadFlag = true

                hideCPV()
                hideErrLayout()
                lock = false

            } else {
                lock = false
                tryAgain()
            }
        }

    }


    private fun loadAdapter(list: List<KotlinMessagesModel>) {
        if (list.isEmpty()) {
            tv_noItemfound.visibility = View.VISIBLE
        } else {
            tv_noItemfound.visibility = View.GONE
        }

        rv_messages.layoutManager = LinearLayoutManager(activity)

        while (rv_messages.itemDecorationCount > 0) {
            rv_messages.removeItemDecorationAt(0)
        }

        rv_messages.addItemDecoration(VerticalLinearLayoutDecoration(activity as Context
                , 8, 8, 8, 8).apply { lastItemPadding(72) })
        rv_messages.adapter = MessagesAdapter(activity as Context, list, object : SendIntentForResult {
            override fun send(i: Intent, bundle: Bundle?, requestCode: Int) {
                if (bundle != null) {
                    startActivityForResult(i, requestCode, bundle)
                } else {
                    startActivityForResult(i, requestCode)
                }
            }
        })
    }
}