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
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import kotlinx.android.synthetic.main.activity_camera.*
import net.nabam.otp.util.OtpInfo
import net.nabam.otp.util.OtpUriParseException
import net.nabam.otp.util.parseOtpUri
import java.io.IOException

const val PERMISSIONS_REQUEST_CAMERA = 0;

class CameraActivity : CompanionActivity() {
    lateinit var mCameraSource: CameraSource
    lateinit var mBarcodeDetector: BarcodeDetector

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
               startCamera(mCameraSource, camera_view.holder)
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mBarcodeDetector = BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

        mCameraSource = CameraSource.Builder(this, mBarcodeDetector)
                .setAutoFocusEnabled(true)
                .build()

        camera_view.getHolder().addCallback(object: SurfaceHolder.Callback {
            override fun surfaceCreated(holder:SurfaceHolder) {
                startCamera(mCameraSource, holder)
            }
            override fun surfaceChanged(holder:SurfaceHolder, format:Int, width:Int, height:Int) {
            }
            override fun surfaceDestroyed(holder:SurfaceHolder) {
                mCameraSource.stop()
            }
        })

        mBarcodeDetector.setProcessor(object: Detector.Processor<Barcode> {
            override fun release() { }
            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                if (mConsumerService?.connection != null) {
                    val barcodes = detections.detectedItems

                    if (barcodes.size() != 0) {
                        val intent = Intent(this@CameraActivity, SubmitActivity::class.java)

                        val otpInfo:OtpInfo
                        try {
                            otpInfo = parseOtpUri(Uri.parse(barcodes.valueAt(0).displayValue))
                        } catch (_:OtpUriParseException) {
                            return
                        }

                        intent.putExtra("otp_info", otpInfo)
                        runOnUiThread {
                            startActivity(intent)
                        }
                        mBarcodeDetector.release()
                        finish()
                    }
                }
            }
        })
    }
}
