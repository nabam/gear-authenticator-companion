package net.nabam.otp.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast

import com.samsung.android.sdk.SsdkUnsupportedException
import com.samsung.android.sdk.accessory.*
import net.nabam.otp.R

import java.io.IOException

const val TAG = "SAP Consumer"

class AccessoryConsumerService : SAAgent {
    constructor() : super(TAG, ServiceConnection::class.java) {
        this.mBinder = LocalBinder()
        this.mHandler = Handler()
    }

    inner class ServiceConnection : SASocket(ServiceConnection::class.java.name) {

        override fun onError(channelId: Int, errorMessage: String, errorCode: Int) {}

        override fun onReceive(channelId: Int, data: ByteArray) {}

        override fun onServiceConnectionLost(reason: Int) {
            closeConnection()
            findPeerAgents()
        }
    }

    inner class LocalBinder : Binder() {
        val service: AccessoryConsumerService
            get() = this@AccessoryConsumerService
    }

    private val mBinder: LocalBinder
    private var mHandler: Handler

    lateinit var mBroadcastManager : LocalBroadcastManager

    private var mConnectionHandler: ServiceConnection? = null

    val connection: ServiceConnection?
        get() = mConnectionHandler

    override fun onCreate() {
        super.onCreate()
        val mAccessory = SA()
        val intent = Intent("service-failed")

        mBroadcastManager = LocalBroadcastManager.getInstance(this)

        try {
            Log.i(TAG, "Initializing accessory consumer service")
            mAccessory.initialize(this)
        } catch (e: SsdkUnsupportedException) {
            if (e.type != SsdkUnsupportedException.LIBRARY_UPDATE_IS_RECOMMENDED) {
              Log.e(TAG, "AccessoryConsumerService failed to initialize", e)
              intent.putExtra("message", e.message)
              mBroadcastManager.sendBroadcast(intent)
              stopSelf()
            }
        } catch (e: Exception) {
            intent.putExtra("message", e.message)
            mBroadcastManager.sendBroadcast(intent)
            stopSelf()
        }

        findPeerAgents()
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onFindPeerAgentsResponse(peerAgents: Array<SAPeerAgent>?, result: Int) {
        if (result == SAAgent.PEER_AGENT_FOUND && peerAgents != null) {
            for (peerAgent in peerAgents)
                requestServiceConnection(peerAgent)
            return
        } else if (result == SAAgent.FINDPEER_DEVICE_NOT_CONNECTED) {
            Toast.makeText(applicationContext, R.string.peer_device_not_connected, Toast.LENGTH_LONG).show()
        } else if (result == SAAgent.FINDPEER_SERVICE_NOT_FOUND) {
            Toast.makeText(applicationContext, R.string.peer_service_not_found, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(applicationContext, R.string.no_peers_found, Toast.LENGTH_LONG).show()
        }

        mBroadcastManager.sendBroadcast(Intent("connection-failed"))
    }

    override fun onServiceConnectionRequested(peerAgent: SAPeerAgent?) {
        if (peerAgent != null) {
            acceptServiceConnectionRequest(peerAgent)
        }
    }

    override fun onServiceConnectionResponse(peerAgent: SAPeerAgent?, socket: SASocket, result: Int) {
        if (result == SAAgent.CONNECTION_SUCCESS) {
            this.mConnectionHandler = socket as ServiceConnection

            mBroadcastManager.sendBroadcast(Intent("connected"))
            return
        } else if (result == SAAgent.CONNECTION_ALREADY_EXIST) {
            Toast.makeText(baseContext, R.string.connection_already_exists, Toast.LENGTH_LONG).show()
        } else if (result == SAAgent.CONNECTION_DUPLICATE_REQUEST) {
            Toast.makeText(baseContext, R.string.connection_duplicate_request, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(baseContext, R.string.connection_failure, Toast.LENGTH_LONG).show()
        }

        mBroadcastManager.sendBroadcast(Intent("connection-failed"))
    }

    override fun onError(peerAgent: SAPeerAgent?, errorMessage: String, errorCode: Int) {
        super.onError(peerAgent, errorMessage, errorCode)
    }

    override fun onPeerAgentsUpdated(peerAgents: Array<SAPeerAgent>?, result: Int) {
        val peers = peerAgents
        val status = result
        mHandler.post {
            if (peers != null) {
                if (status == SAAgent.PEER_AGENT_AVAILABLE) {
                    Toast.makeText(applicationContext, R.string.peer_agent_available, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, R.string.peer_agent_unavailable, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun findPeers() {
        findPeerAgents()
    }

    fun sendData(data: String): Boolean {
        if (mConnectionHandler == null) {
            return false
        }

        try {
            mConnectionHandler?.secureSend(getServiceChannelId(0), data.toByteArray())
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    fun closeConnection(): Boolean {
        val ret : Boolean
        if (mConnectionHandler != null) {
            mConnectionHandler?.close()
            mConnectionHandler = null
            ret = true
        } else {
            ret = false
        }

        mBroadcastManager.sendBroadcast(Intent("disconnected"))

        return ret
    }
}
