package com.mtdevelopment.checkout.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Specialized Activity to handle 3D Secure (3DS) authentication required by banks.
 * It uses a [WebView] to display the bank's authentication page.
 * 
 * The flow is as follows:
 * 1. SumUp API returns a "Next Step" indicating 3DS is required, with a URL and payload.
 * 2. This Activity is launched with the URL and parameters.
 * 3. If the method is POST, a hidden HTML form is generated and auto-submitted in the WebView.
 * 4. If the method is GET, the URL is simply loaded.
 * 5. The Activity monitors URL loading. When the return URL (provided by SumUp) is reached,
 *    it signifies the user has completed the bank challenge.
 * 6. The Activity finishes, allowing the background polling in the DataSource to detect the final PAID status.
 */
class ThreeDSecureActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_METHOD = "extra_method"
        const val EXTRA_PAYLOAD_PARAMS = "extra_payload"
        const val EXTRA_REDIRECT_URL = "extra_target_redirect"
    }

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Creating WebView programmatically for simplicity
        webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val method = intent.getStringExtra(EXTRA_METHOD) ?: "GET"
        val targetRedirect =
            intent.getStringExtra(EXTRA_REDIRECT_URL) ?: BuildConfig.SUMUP_REDIRECT_URL

        // POST parameters (PaReq, MD, TermUrl, etc.) provided by the payment gateway
        val payloadParams =
            intent.getSerializableExtra(EXTRA_PAYLOAD_PARAMS) as? HashMap<String, String>

        setupWebView(targetRedirect)

        if (method.equals("POST", ignoreCase = true) && payloadParams != null) {
            // Complex case: Bank requires a POST request with specific data
            val html = buildHtmlForm(url, payloadParams)
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } else {
            // Simple case: Just load the URL
            webView.loadUrl(url)
        }
    }

    /**
     * Configures the WebView to handle the banking redirect flow.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(targetRedirect: String) {
        webView.settings.javaScriptEnabled =
            true // Required for bank pages and form auto-submission
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val loadingUrl = request?.url?.toString() ?: return false

                // DETECTION OF COMPLETION
                // If the URL matches the target redirect URL, the 3DS process is finished on the bank side.
                if (loadingUrl.startsWith(targetRedirect)) {
                    // Signal success and close. The background polling will now see the checkout status update.
                    setResult(Activity.RESULT_OK)
                    finish()
                    return true
                }
                return false
            }
        }
    }

    /**
     * Generates a hidden HTML form that auto-submits via JavaScript.
     * This is the standard way to initiate a 3DS POST request within a WebView.
     */
    private fun buildHtmlForm(url: String, params: HashMap<String, String>): String {
        val inputs = params.map { (key, value) ->
            """<input type="hidden" name="$key" value="$value" />"""
        }.joinToString("\n")

        return """
            <html>
            <head><script>function submit() { document.getElementById('form').submit(); }</script></head>
            <body onload="submit()">
                <form id="form" action="$url" method="POST">
                    $inputs
                </form>
            </body>
            </html>
        """.trimIndent()
    }
}