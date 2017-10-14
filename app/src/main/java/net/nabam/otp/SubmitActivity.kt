package net.nabam.otp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.android.synthetic.main.activity_submit.*
import net.nabam.otp.util.OtpInfo

class SubmitActivity : CompanionActivity() {
    lateinit var mOtpInfo:OtpInfo
    val mObjectMapper = jacksonObjectMapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submit)

        val payload = intent.getParcelableExtra<OtpInfo>("otp_info")
        if (payload != null) {
            mOtpInfo = payload
        } else {
            finish()
        }

        labelView.text = mOtpInfo?.label
        typeView.text = mOtpInfo?.type.toString()
        aliasField.setText(mOtpInfo?.alias, TextView.BufferType.EDITABLE)

        submit.setOnClickListener { _ ->
            if (!aliasField.text.isNullOrBlank()) {
                mOtpInfo.alias = aliasField.text.toString()
            }
            mConsumerService?.sendData(mObjectMapper.writeValueAsString(mOtpInfo))

            Toast.makeText(this@SubmitActivity, R.string.submitted, Toast.LENGTH_LONG).show()
            val intent = Intent(this@SubmitActivity, ActionActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}
