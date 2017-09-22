package net.nabam.otp

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.SurfaceHolder
import android.widget.TextView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast

import com.google.android.gms.vision.Detector
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException

const val PERMISSIONS_REQUEST_CAMERA = 0;

class MainActivity : CompanionActivity() {
    lateinit var cameraSource : CameraSource
    lateinit var mBroadcastManager : LocalBroadcastManager

    val mDisconnectedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Toast.makeText(applicationContext, R.string.disconnected, Toast.LENGTH_LONG).show()

            val intent = Intent(this@MainActivity, ConnectActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent)
        }
    }

    fun startCamera(cameraSource: CameraSource, holder: SurfaceHolder) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), PERMISSIONS_REQUEST_CAMERA);

            return
        }

        try {
            cameraSource.start(holder)
        } catch (e: IOException) {
            Log.e("CAMERA SOURCE", e.message);
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish()
            } else {
               startCamera(cameraSource, camera_view.holder)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBroadcastManager = LocalBroadcastManager.getInstance(this)
        subscribe()

        val barcodeDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

        cameraSource = CameraSource.Builder(this, barcodeDetector).build()

        camera_view.getHolder().addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder:SurfaceHolder) {
                startCamera(cameraSource, holder)
            }
            override fun surfaceChanged(holder:SurfaceHolder, format:Int, width:Int, height:Int) {
            }
            override fun surfaceDestroyed(holder:SurfaceHolder) {
                cameraSource.stop()
            }
        })

        barcodeDetector.setProcessor(object: Detector.Processor<Barcode> {
            override fun release() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val barcodes = detections.detectedItems
                val codeInfo = findViewById(R.id.code_info) as TextView

                if (barcodes.size() != 0) {
                    codeInfo.post(object: Runnable {
                        override fun run() = codeInfo.setText(barcodes.valueAt(0).displayValue)
                    })
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        subscribe()
    }

    override fun onPause() {
        super.onPause()
        unsubscribe()
    }

    override fun onDestroy() {
        unsubscribe()
        super.onDestroy()
    }

    fun subscribe() {
        mBroadcastManager.registerReceiver(mDisconnectedBroadcastReceiver, IntentFilter("disconnected"))
    }

    fun unsubscribe() {
        mBroadcastManager.unregisterReceiver(mDisconnectedBroadcastReceiver)
    }
}
