package net.nabam.otp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_connect.*

class ConnectActivity : CompanionActivity() {
    override fun onConnection() {
        super.onConnection()
        val intent = Intent(this@ConnectActivity, ActionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent)
    }

    override val mConnectionFailedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBroadcastManager = LocalBroadcastManager.getInstance(this)
        setContentView(R.layout.activity_connect)

        connect_button.setOnClickListener { _ ->
            mConsumerService?.findPeers()
        }
    }

    override fun subscribe() {
        mBroadcastManager.registerReceiver(mConnectedBroadcastReceiver, IntentFilter("connected"))
    }

    override fun unsubscribe() {
        mBroadcastManager.unregisterReceiver(mConnectedBroadcastReceiver)
    }
}

