package net.nabam.otp

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import net.nabam.otp.service.AccessoryConsumerService

/**
 * Created by lev on 22/09/17.
 */
open class CompanionActivity : AppCompatActivity() {
    var mConsumerService: AccessoryConsumerService? = null
    var mIsBound = false

    val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mConsumerService = (service as AccessoryConsumerService.LocalBinder).service
            onServiceUp()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mConsumerService = null
            mIsBound = false
        }
    }

    open fun onServiceUp() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mIsBound = bindService(
                Intent(this, AccessoryConsumerService::class.java),
                mConnection,
                Context.BIND_AUTO_CREATE)

    }

    override fun onDestroy() {
        // Un-bind service
        if (mIsBound) {
            unbindService(mConnection)
            mIsBound = false
        }
        super.onDestroy()
    }
}