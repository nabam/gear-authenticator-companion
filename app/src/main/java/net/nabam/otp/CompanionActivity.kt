package net.nabam.otp

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
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
            runOnUiThread {
                onServiceUp()
            }
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mConsumerService = null
            mIsBound = false
        }
    }

    open val mConnectionFailedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread {
                val intent = Intent(applicationContext, ConnectActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent)
            }
        }
    }
    val mConnectedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runOnUiThread {
                onConnection()
            }
        }
    }

    open fun onConnection() {}
    open fun onServiceUp() {
        if (mConsumerService?.connection != null) {
            onConnection()
        }
    }

    lateinit var mBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBroadcastManager = LocalBroadcastManager.getInstance(this)
        subscribe()

        mIsBound = bindService(
                Intent(this, AccessoryConsumerService::class.java),
                mConnection,
                Context.BIND_AUTO_CREATE)

    }

    override fun onResume() {
        super.onResume()
        subscribe()
    }

    override fun onPause() {
        super.onPause()
        unsubscribe()
    }

    open fun subscribe() {
        mBroadcastManager.registerReceiver(mConnectionFailedBroadcastReceiver, IntentFilter("connection-failed"))
        mBroadcastManager.registerReceiver(mConnectedBroadcastReceiver, IntentFilter("connected"))
    }

    open fun unsubscribe() {
        mBroadcastManager.unregisterReceiver(mConnectionFailedBroadcastReceiver)
        mBroadcastManager.unregisterReceiver(mConnectedBroadcastReceiver)
    }

    override fun onDestroy() {
        if (mIsBound) {
            unbindService(mConnection)
            mIsBound = false
        }

        unsubscribe()
        super.onDestroy()
    }
}