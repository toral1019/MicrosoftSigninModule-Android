package com.inexture.microsoftsignin

import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun microsoftSignInHandler(target: Any?, @RawRes authConfig: Int, callback: (SignInResult.() -> Unit)) {
    transactFragmentAndContinue(target) { fragment ->
        //apply callback for sign in
        val signInResult = SignInResult(authConfig).apply(callback)
        fragment?.signIn(authConfig,signInResult)
    }
}

private fun microsoftSignOutHandler(target: Any?, callback: (() -> Unit)) {
    transactFragmentAndContinue(target) { fragment ->
        fragment?.signOut()
        callback()
    }
}

private fun transactFragmentAndContinue(target: Any?, callback: (MicrosoftSignInManagerFragment?) -> Unit) {
    if (target is AppCompatActivity || target is Fragment) {

        val context = when (target) {
            is Context -> target
            is Fragment -> target.context
            else -> null
        }

        var checkerFragment = when (context) {
            // for app compat activity
            is AppCompatActivity -> context.supportFragmentManager?.findFragmentByTag(MicrosoftSignInManagerFragment::class.java.canonicalName) as MicrosoftSignInManagerFragment?
            // for support fragment
            is Fragment -> context.childFragmentManager.findFragmentByTag(MicrosoftSignInManagerFragment::class.java.canonicalName) as MicrosoftSignInManagerFragment?
            // else return null
            else -> null
        }

        if (checkerFragment == null) {
            Log.d("googleSignInHandler", "runWithPermissions: adding headless fragment for asking permissions")
            checkerFragment = MicrosoftSignInManagerFragment.newInstance()
            when (context) {
                is AppCompatActivity -> {
                    context.supportFragmentManager.beginTransaction().apply {
                        add(checkerFragment, MicrosoftSignInManagerFragment::class.java.canonicalName)
                        commit()
                    }
                    // make sure fragment is added before we do any context based operations
                    context.supportFragmentManager?.executePendingTransactions()
                }
                is Fragment -> {
                    // this does not work at the moment
                    context.childFragmentManager.beginTransaction().apply {
                        add(checkerFragment, MicrosoftSignInManagerFragment::class.java.canonicalName)
                        commit()
                    }
                    // make sure fragment is added before we do any context based operations
                    context.childFragmentManager.executePendingTransactions()
                }
            }
        }
        callback(checkerFragment)
    }
}