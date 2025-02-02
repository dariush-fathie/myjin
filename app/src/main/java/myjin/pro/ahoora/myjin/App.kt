package myjin.pro.ahoora.myjin

import android.content.IntentFilter
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import androidx.multidex.MultiDexApplication
import com.google.firebase.analytics.FirebaseAnalytics
import im.crisp.sdk.Crisp
import io.realm.Realm
import io.realm.RealmConfiguration
import myjin.pro.ahoora.myjin.utils.NetworkStateReceiver
import myjin.pro.ahoora.myjin.utils.NetworkUtil

class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()

        FirebaseAnalytics.getInstance(this)

        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .name("database.realm")
                //.encryptionKey(getKey())
                .schemaVersion(1)
                //.modules(new MySchemaModule())
                //.migration(new MyMigration())
                .build()


        /*OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .filterOtherGCMReceivers(true)
                .setNotificationOpenedHandler(CustomNotificationOpenedHandler(this))
                //.setNotificationReceivedHandler(CustomNotificationReceivedHandler())
                .init()*/

        Realm.setDefaultConfiguration(config)

        registerForNetworkChangeEvents()


        Crisp.initialize(this)
        Crisp.getInstance().websiteId = "8c52ac5b-a56a-4728-bf99-134386471d62"

    }

    private fun registerForNetworkChangeEvents() {
        val networkStateChangeReceiver = NetworkStateReceiver()
        registerReceiver(networkStateChangeReceiver, IntentFilter(CONNECTIVITY_ACTION))
        registerReceiver(networkStateChangeReceiver, IntentFilter(NetworkUtil().WIFI_STATE_CHANGE_ACTION))
        registerReceiver(networkStateChangeReceiver, IntentFilter(NetworkUtil().manuallyTriggerOnReceive))
    }


}
