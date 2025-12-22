package com.mtdevelopment.checkout.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

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
        webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra(EXTRA_URL) ?: return finish()
        val method = intent.getStringExtra(EXTRA_METHOD) ?: "GET"
        val targetRedirect =
            intent.getStringExtra(EXTRA_REDIRECT_URL) ?: BuildConfig.SUMUP_REDIRECT_URL

        // Récupération des paramètres POST (PaReq, MD, TermUrl, etc.) sous forme de Map
        val payloadParams =
            intent.getSerializableExtra(EXTRA_PAYLOAD_PARAMS) as? HashMap<String, String>

        setupWebView(targetRedirect)

        if (method.equals("POST", ignoreCase = true) && payloadParams != null) {
            // Cas complexe : Il faut POSTER des données
            val html = buildHtmlForm(url, payloadParams)
            webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
        } else {
            // Cas simple : GET
            webView.loadUrl(url)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(targetRedirect: String) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val loadingUrl = request?.url?.toString() ?: return false

                // DÉTECTION DU SUCCÈS / FIN
                // Si l'URL chargée par la banque correspond à votre URL de retour (ex: votresite.com/success)
                // Cela signifie que la banque a fini son travail.
                if (loadingUrl.startsWith(targetRedirect)) {
                    // On ferme l'activité, le Polling qui tourne en fond dans le Repository
                    // va détecter le changement de statut (PENDING -> PAID/FAILED)
                    setResult(Activity.RESULT_OK)
                    finish()
                    return true
                }
                return false
            }
        }
    }

    /**
     * Génère un formulaire HTML automatique pour poster les données sécurisées
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