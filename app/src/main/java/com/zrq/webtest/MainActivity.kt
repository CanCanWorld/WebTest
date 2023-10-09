package com.zrq.webtest

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zrq.webtest.ui.theme.WebTestTheme

class MainActivity : ComponentActivity() {

    private var mNfcAdapter: NfcAdapter? = null
    private var mPendingIntent: PendingIntent? = null
    private var mTag: Tag? = null
    private var value by mutableStateOf("")

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initNFC()
        setContent {
            WebTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            value = value,
                            onValueChange = {
                                value = it
                                Log.d(TAG, "onValueChange: $value")
                            })
                        Button(modifier = Modifier.fillMaxWidth(), onClick = { writeContent() }) {
                            Text(text = "写入")
                        }
                    }
                }
            }
        }
    }

    //初始化NFC，检测是否可用
    private fun initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this) //设备的NfcAdapter对象
        if (mNfcAdapter == null) { //判断设备是否支持NFC功能
            Toast.makeText(this, "设备不支持NFC功能!", Toast.LENGTH_SHORT).show()
            return
        }
        if (!mNfcAdapter!!.isEnabled) { //判断设备NFC功能是否打开
            Toast.makeText(this, "请到系统设置中打开NFC功能!", Toast.LENGTH_SHORT).show()
            return
        }
        //创建PendingIntent对象,当检测到一个Tag标签就会执行此Intent
        mPendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), FLAG_MUTABLE)
    }


    //写卡
    private fun writeContent() {
        if (value == "") {
            Toast.makeText(this, "内容不能为空!", Toast.LENGTH_SHORT).show()
            return
        }
        NFCUtil.writeMessage(this, mTag, value)
    }

    //读卡
    override fun onNewIntent(intent: Intent) {
        Log.d(TAG, "onNewIntent: $intent")
        super.onNewIntent(intent)
        initTag(intent)
        val devmark: String = NFCUtil.readMessage(this, mTag)
        Log.d(TAG, "devmark: $devmark")
        if (devmark == "") {
            Toast.makeText(this, "设备编码为空!", Toast.LENGTH_SHORT).show()
            return
        } else {
            Toast.makeText(this, "设备编码: $devmark", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initTag(intent: Intent) {
        mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG) //获取到Tag标签对象
        Log.d(TAG, "initTag: $mTag")
    }

    override fun onResume() {
        super.onResume()
        if (mNfcAdapter != null && mNfcAdapter!!.isEnabled) {
            mNfcAdapter!!.enableForegroundDispatch(this, mPendingIntent, null, null);//打开前台发布系统，使页面优于其它nfc处理.当检测到一个Tag标签就会执行mPendingItent
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}
