package net.nabam.otp

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_connect.*


const val TAG = "GearAuthenticator"

class ConnectActivity : CompanionActivity() {
    lateinit var mBroadcastManager : LocalBroadcastManager

    val mConnectedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            connect_button.visibility = View.INVISIBLE
            val intent = Intent(this@ConnectActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent)
        }
    }

    val mConnectionFailedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            connect_button.visibility = View.VISIBLE
        }
    }

    val mServiceFailedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(applicationContext, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(this@ConnectActivity);
            builder
                    .setMessage(intent.getStringExtra("message"))
                    .setCancelable(false)
                    .setPositiveButton("Ok", object : DialogInterface.OnClickListener {
                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            finish()
                        }
                    })
            builder.show()
        }
    }

    override fun onServiceUp() {
        super.onServiceUp()
        mConsumerService?.findPeers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBroadcastManager = LocalBroadcastManager.getInstance(this)
        setContentView(R.layout.activity_connect)
        connect_button.visibility = View.INVISIBLE

        subscribe()

        connect_button.setOnClickListener { _ ->
            mConsumerService?.findPeers()
        }
    }

    private fun subscribe() {
        mBroadcastManager.registerReceiver(mConnectedBroadcastReceiver, IntentFilter("connected"))
        mBroadcastManager.registerReceiver(mConnectionFailedBroadcastReceiver, IntentFilter("connection-failed"))
        mBroadcastManager.registerReceiver(mServiceFailedBroadcastReceiver, IntentFilter("service-failed"))
    }

    private fun unsubscribe() {
        mBroadcastManager.unregisterReceiver(mConnectedBroadcastReceiver)
        mBroadcastManager.unregisterReceiver(mConnectionFailedBroadcastReceiver)
        mBroadcastManager.unregisterReceiver(mServiceFailedBroadcastReceiver)
    }

    override fun onDestroy() {
        unsubscribe()
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        unsubscribe()
    }

    override fun onResume() {
        super.onResume()
        subscribe()
    }
}
