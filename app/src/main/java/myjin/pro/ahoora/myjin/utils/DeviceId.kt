package myjin.pro.ahoora.myjin.utils

import android.os.Build
import ir.paad.audiobook.utils.StringUtil

object DeviceId {
     var deviceBuildInfo = Build.BOARD + Build.BRAND + Build.DISPLAY + Build.HOST + Build.ID +
            Build.MANUFACTURER + Build.MODEL + Build.PRODUCT + Build.TAGS + Build.USER + Build.VERSION.SDK_INT

    init {
        deviceBuildInfo = StringUtil.removeSpaces(deviceBuildInfo)

    }


}
