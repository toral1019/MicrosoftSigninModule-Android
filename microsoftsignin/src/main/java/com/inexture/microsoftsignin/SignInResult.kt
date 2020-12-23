package com.inexture.microsoftsignin

import androidx.annotation.RawRes
import org.json.JSONObject


open class SignInResult {
    internal var onError: (() -> Unit)? = null
    internal var onSuccess: ((account:JSONObject) -> Unit)? = null
    var authConfig: Int? = null

    constructor(@RawRes authConfig: Int?) {
        this.authConfig = authConfig
    }

    //DSL
    fun onError(func: (() -> Unit)?) {
        this.onError = func
    }

    fun onSuccess(func: ((account: JSONObject) -> Unit)?) {
        this.onSuccess = func
    }
}