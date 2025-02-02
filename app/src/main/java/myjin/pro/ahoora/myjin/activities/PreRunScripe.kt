package myjin.pro.ahoora.myjin.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.activity_pre_run_scripe.*
import myjin.pro.ahoora.myjin.R


class PreRunScripe : AppCompatActivity(), View.OnClickListener {
    override fun onClick(p0: View?) {
        when(p0?.id){
            R.id.iv_goback->finish()
            R.id.tv_continue->startActivity(Intent(this, ScripeChat::class.java))
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_run_scripe)

        tv_continue.setOnClickListener(this)
        iv_goback.setOnClickListener(this)

    }


}
