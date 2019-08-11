package com.systemallica.valenbisi.activities

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.util.Log
import com.android.billingclient.api.*
import com.systemallica.valenbisi.R
import kotlinx.android.synthetic.main.activity_donate.*


class DonateActivity : AppCompatActivity(), PurchasesUpdatedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donate)

        val mToolbar = findViewById<Toolbar>(R.id.toolbarDonate)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        setClickListeners()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setClickListeners() {
        card_view_one.setOnClickListener { startBuyProcess("donation_upgrade") }
        card_view_three.setOnClickListener { startBuyProcess("donation_upgrade_3") }
        card_view_five.setOnClickListener { startBuyProcess("donation_upgrade_5") }
    }

    private fun startBuyProcess(sku: String) {
        val mBillingClient: BillingClient =
            BillingClient.newBuilder(applicationContext).setListener(this).build()
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready
                    // Start buy process
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSku(sku)
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                    // Launch purchase
                    mBillingClient.launchBillingFlow(this@DonateActivity, flowParams)
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Snackbar.make(donateView!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT)
                    .show()
            }
        })
    }

    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            // Apply preference
            val settings = getSharedPreferences(PREFS_NAME, 0)
            val editor = settings.edit()
            editor.putBoolean("donationPurchased", true).apply()

            consumePurchases()
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Snackbar.make(donateView!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        } else {
            // Handle any other error codes.
            Snackbar.make(donateView!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun consumePurchases(){
        val mBillingClient: BillingClient =
            BillingClient.newBuilder(applicationContext).setListener(this).build()
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready
                    val purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP)
                    val purchases = purchasesResult.purchasesList

                    val consumeListener = { responseCodeConsumed: Int, purchaseToken: String ->
                        if (responseCodeConsumed == BillingClient.BillingResponse.OK) {
                            Log.i("Billing", purchaseToken)
                        }
                    }

                    for(purchase in purchases){
                        mBillingClient.consumeAsync(
                            purchase.purchaseToken, consumeListener
                        )
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Log.e("Billing", "consume failed")
            }
        })
    }
}
