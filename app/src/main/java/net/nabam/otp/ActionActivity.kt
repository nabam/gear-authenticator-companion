package net.nabam.otp

import android.app.AlertDialog
import android.content.*
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_action.*

class ActionActivity : CompanionActivity() {

    val mServiceFailedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(applicationContext, intent.getStringExtra("message"), Toast.LENGTH_LONG).show()
            val builder = AlertDialog.Builder(this@ActionActivity);
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

    override fun onConnection() {
        super.onConnection()
        scan_button.isEnabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_action)

        if (mConsumerService == null) {
            scan_button.isEnabled = false
        }
        scan_button.setOnClickListener { _ ->
            val intent = Intent(this@ActionActivity, CameraActivity::class.java)
            startActivity(intent)
        }
    }

    override fun subscribe() {
        super.subscribe()
        mBroadcastManager.registerReceiver(mServiceFailedBroadcastReceiver, IntentFilter("service-failed"))
    }
    override fun unsubscribe() {
        super.unsubscribe()
        mBroadcastManager.unregisterReceiver(mServiceFailedBroadcastReceiver)
    }
}
