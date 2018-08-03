package myjin.pro.ahoora.myjin.activitys

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import io.realm.Realm
import ir.paad.audiobook.decoration.VerticalLinearLayoutDecoration
import kotlinx.android.synthetic.main.activity_fav_message.*
import myjin.pro.ahoora.myjin.R
import myjin.pro.ahoora.myjin.adapters.FavMessageAdapter
import myjin.pro.ahoora.myjin.customClasses.MsgSpinnerDialog
import myjin.pro.ahoora.myjin.interfaces.OnSpinnerItemSelected
import myjin.pro.ahoora.myjin.interfaces.SendIntentForResult
import myjin.pro.ahoora.myjin.models.KotlinMessagesModel

class FavMessageActivity : AppCompatActivity(), View.OnClickListener {

    private var sources = ArrayList<Pair<String, Int>>()
    private var types = ArrayList<Pair<String, Int>>()

    private var groupId = -1
    private var typeId = -1

    private var realm = Realm.getDefaultInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fav_message)

        findDistinctTypesAndSources()
        setListener()
    }

    private fun findDistinctTypesAndSources() {
        realm.executeTransactionAsync(Realm.Transaction { db ->

            val distinctSources = db.where(KotlinMessagesModel::class.java)
                    .equalTo("saved", true)
                    .distinct("groupId")
                    .findAll()

            val distinctTypes = db.where(KotlinMessagesModel::class.java)
                    .equalTo("saved", true)
                    .distinct("typeId")
                    .findAll()

            distinctSources.forEach { item ->
                val pair = Pair(item.groupName, item.groupId)
                sources.add(pair)
            }

            sources.add(0, Pair("همه", -1))

            distinctTypes.forEach { item ->
                val pair = Pair(item.type, item.typeId)
                types.add(pair)
            }

            types.add(0, Pair("همه", -1))

        }, Realm.Transaction.OnSuccess {
            querying(-1, -1)
        })
    }

    private var itemsList = ArrayList<KotlinMessagesModel>()

    private fun querying(groupId: Int, typeId: Int) {

        if (realm.isClosed) {
            realm = Realm.getDefaultInstance()
        }

        realm.executeTransactionAsync(Realm.Transaction { db ->
            val query = db.where(KotlinMessagesModel::class.java)
                    .equalTo("saved", true)

            if (groupId != -1) {
                query.equalTo("groupId", groupId)
            }

            if (typeId != -1) {
                query.equalTo("typeId", typeId)
            }

            val result = query.findAll()
            itemsList.clear()

            result.forEach { item ->
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
                Log.e("items", item.toString())
            }

        }, Realm.Transaction.OnSuccess {
            loadAdapter(itemsList)
        })
    }

    private fun loadAdapter(list: List<KotlinMessagesModel>) {

        if (list.isEmpty()) {
            cv1.visibility = View.GONE
            cv2.visibility = View.GONE

            tv_empty.visibility = View.VISIBLE
        } else {
            tv_empty.visibility = View.GONE
        }
        rv_favMessages.layoutManager = LinearLayoutManager(this)

        while (rv_favMessages.itemDecorationCount > 0) {
            rv_favMessages.removeItemDecorationAt(0)
        }

        rv_favMessages.addItemDecoration(VerticalLinearLayoutDecoration(this
                , 8, 8, 8, 8).apply { lastItemPadding(48) })
        rv_favMessages.adapter = FavMessageAdapter(this, list, object : SendIntentForResult {
            override fun send(i: Intent, bundle: Bundle?, requestCode: Int) {
                if (bundle != null) {
                    ActivityCompat.startActivityForResult(this@FavMessageActivity, i, requestCode, bundle)
                } else {
                    startActivityForResult(i, requestCode)
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_sources -> openSourceDialog()
            R.id.tv_types -> openTypeDialog()
            R.id.iv_goback -> {
                //todo setResult()
                finish()
            }
        }
    }

    private fun setListener() {
        tv_sources.setOnClickListener(this)
        tv_types.setOnClickListener(this)
        iv_goback.setOnClickListener(this)
    }

    private fun openSourceDialog() {
        val sourcesName = ArrayList<String>()
        sources.forEach { pair ->
            sourcesName.add(pair.first)
        }

        val dialog = MsgSpinnerDialog(this@FavMessageActivity, sourcesName, "منبع مورد نظر را انتخاب کنید")
        dialog.setOnSpinnerItemSelectedListener(object : OnSpinnerItemSelected {
            override fun onClick(name: String, position: Int) {
                tv_sources.text = "منبع : $name"
                groupId = sources[position].second
                // typeId = -1 - select all types
                resetDefaultType()
                getDistinctType(groupId)
                querying(groupId, typeId)

            }
        })
        dialog.show()
    }

    private fun openTypeDialog() {
        val typesName = ArrayList<String>()
        types.forEach { pair ->
            typesName.add(pair.first)
        }
        val dialog = MsgSpinnerDialog(this@FavMessageActivity, typesName, "دسته بندی مورد نظر را انتخاب کنید")
        dialog.setOnSpinnerItemSelectedListener(object : OnSpinnerItemSelected {
            override fun onClick(name: String, position: Int) {
                tv_types.text = "دسته بندی : $name"
                typeId = types[position].second
                querying(groupId, typeId)
            }
        })
        dialog.show()
    }

    private fun resetDefaultType() {
        typeId = -1
        tv_types.text = "دسته بندی : همه"
    }

    private fun getDistinctType(groupId: Int) {
        realm.executeTransactionAsync { db ->
            val query = db.where(KotlinMessagesModel::class.java)
                    .equalTo("saved", true)
            if (groupId != -1) {
                query.equalTo("groupId", groupId)
            }
            query.distinct("typeId")
            val result = query.findAll()

            types.clear()
            result.forEach { item ->
                val pair = Pair(item.type, item.typeId)
                types.add(pair)
            }
            types.add(0, Pair("همه", -1))

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

}