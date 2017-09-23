package net.nabam.otp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_camera.*
import net.nabam.otp.util.parseUri
import java.io.IOException

const val PERMISSIONS_REQUEST_CAMERA = 0;

class CameraActivity : CompanionActivity() {
    lateinit var cameraSource : CameraSource

    val mObjectMapper = jacksonObjectMapper()

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
        setContentView(R.layout.activity_camera)

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
            override fun release() { }
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (mConsumerService?.connection != null) {
                    val barcodes = detections.detectedItems

                    if (barcodes.size() != 0) {
                        try {
                            val json = mObjectMapper.writeValueAsString(
                                    parseUri(Uri.parse(barcodes.valueAt(0).displayValue)))
                            mConsumerService?.sendData(json)
                        } catch (e: RuntimeException) {
                            return
                        }

                        runOnUiThread {
                            Toast.makeText(this@CameraActivity, R.string.submitted, Toast.LENGTH_LONG).show()
                            val intent = Intent(this@CameraActivity, ActionActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent)
                        }
                    }
                }
            }
        })
    }
}
