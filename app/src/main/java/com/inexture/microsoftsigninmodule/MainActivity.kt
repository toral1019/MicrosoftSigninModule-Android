package com.inexture.microsoftsigninmodule

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.inexture.microsoftsignin.microsoftSignInHandler
import com.inexture.microsoftsigninmodule.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        mBinding.btnSignIn.setOnClickListener {
            microsoftSignInHandler(this,authConfig = R.raw.auth_config){
                onSuccess {
                    Log.d("===success",it.toString())
                }
                onError {
                    Toast.makeText(this@MainActivity,"error",Toast.LENGTH_LONG)
                }
            }
        }
    }
}
