package com.inexture.microsoftsignin


import android.content.ContentValues
import android.util.Log
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalServiceException
import com.microsoft.identity.client.exception.MsalUiRequiredException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MicrosoftSignInManagerFragment : Fragment() {

    internal val SCOPES = arrayOf("https://graph.microsoft.com/User.Read")
    private lateinit var sampleApp: PublicClientApplication
    private lateinit var authResult: IAuthenticationResult
    internal val MSGRAPH_URL = "https://graph.microsoft.com/v1.0/me"
    private var mSignInResult: SignInResult? = null

    companion object {
        private val RC_SIGN_IN = 9001
        fun newInstance(): MicrosoftSignInManagerFragment {
            val myFragment = MicrosoftSignInManagerFragment()
            return myFragment
        }
    }

    fun signIn(@RawRes authConfig: Int, signInResult: SignInResult) {

        mSignInResult = signInResult

        sampleApp = PublicClientApplication(
            this.requireContext(),
            authConfig
        )

        sampleApp.getAccounts(object : PublicClientApplication.AccountsLoadedCallback {
            override fun onAccountsLoaded(accounts: MutableList<IAccount>?) {
                if (accounts?.isNotEmpty()!!) {
                    sampleApp.acquireTokenSilentAsync(SCOPES, accounts.get(0), getAuthSilentCallback())
                } else {
                    /* No accounts or >1 account */
                }
            }
        })

        sampleApp.acquireToken(requireActivity(), SCOPES, getAuthInteractiveCallback())
    }

    /* Callback used for interactive request.  If succeeds we use the access
     * token to call the Microsoft Graph. Does not check cache
     */
    private fun getAuthInteractiveCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, call graph now */
                Log.d("===", "Successfully authenticated")
                Log.d("===", "ID Token: " + authenticationResult.idToken!!)

                /* Store the auth result */
                authResult = authenticationResult

                /* call graph */
                callGraphAPI()

            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d("===", "Authentication failed: $exception")

                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                }
            }

            override fun onCancel() {
                /* User canceled the authentication */
                Log.d("===", "User cancelled login.")
            }
        }
    }

    /* Callback used in for silent acquireToken calls.
    * Looks if tokens are in the cache (refreshes if necessary and if we don't forceRefresh)
    * else errors that we need to do an interactive request.
    */
    private fun getAuthSilentCallback(): AuthenticationCallback {
        return object : AuthenticationCallback {

            override fun onSuccess(authenticationResult: IAuthenticationResult) {
                /* Successfully got a token, call graph now */
                Log.d("=====", "Successfully authenticated")

                /* Store the authResult */
                authResult = authenticationResult

                /* call graph */
                callGraphAPI()

            }

            override fun onError(exception: MsalException) {
                /* Failed to acquireToken */
                Log.d("=====", "Authentication failed: $exception")

                if (exception is MsalClientException) {
                    /* Exception inside MSAL, more info inside MsalError.java */
                } else if (exception is MsalServiceException) {
                    /* Exception when communicating with the STS, likely config issue */
                } else if (exception is MsalUiRequiredException) {
                    /* Tokens expired or no session, retry with interactive */
                }
            }

            override fun onCancel() {
                /* User cancelled the authentication */
                Log.d("=====", "User cancelled login.")
            }
        }
    }

    /* Use Volley to make an HTTP request to the /me endpoint from MS Graph using an access token */
    private fun callGraphAPI() {
        /* Make sure we have a token to send to graph */
        if (authResult.getAccessToken() == null) {
            return
        }

        val queue = Volley.newRequestQueue(context)
        val parameters = JSONObject()

        try {
            parameters.put("key", "value")
        } catch (e: Exception) {
            Log.d("===", "Failed to put parameters: $e")
        }

        val request = object : JsonObjectRequest(
            Request.Method.GET, MSGRAPH_URL,
            parameters, Response.Listener { response ->
                /* Successfully called graph, process data and send to UI */
                Log.d("===", "Response: $response")
                response.let { mSignInResult?.onSuccess?.invoke(response) }

            }, Response.ErrorListener { error -> Log.d("===", "Error: $error") }) {
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer " + authResult.getAccessToken()
                return headers
            }
        }

        Log.d("===", "Adding HTTP GET to Queue, Request: $request")

        request.retryPolicy = DefaultRetryPolicy(
            3000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )
        queue.add(request)
    }


    /* Clears an account's tokens from the cache.
     * Logically similar to "sign out" but only signs out of this app.
     * User will get interactive SSO if trying to sign back-in.
     */
    fun signOut() {
        /* Attempt to get a user and acquireTokenSilent
                * If this fails we do an interactive request
                */
        sampleApp.getAccounts(object : PublicClientApplication.AccountsLoadedCallback {
            override fun onAccountsLoaded(accounts: List<IAccount>) {

                if (accounts.isEmpty()) {
                    /* No accounts to remove */

                } else {
                    for (account in accounts) {
                        sampleApp.removeAccount(
                            account
                        ) { isSuccess ->
                            if (isSuccess!!) {
                                /* successfully removed account */
                                Log.d(ContentValues.TAG, "Logged out successfully")
                            } else {
                                /* failed to remove account */
                                Log.d(ContentValues.TAG, "Failure logging out")
                            }
                        }
                    }
                }
            }
        })
    }


}
