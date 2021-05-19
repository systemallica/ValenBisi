package com.systemallica.valenbisi.activities

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.billingclient.api.*
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.databinding.ActivityDonateBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class DonateActivity : AppCompatActivity(), PurchasesUpdatedListener, CoroutineScope {

    private lateinit var billingClient: BillingClient
    private var job: Job = Job()
    private lateinit var binding: ActivityDonateBinding

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val mToolbar = findViewById<Toolbar>(R.id.toolbarDonate)
        setSupportActionBar(mToolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        setClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun setClickListeners() {
        binding.cardViewOne.setOnClickListener { startBuyProcess("donation_upgrade") }
        binding.cardViewThree.setOnClickListener { startBuyProcess("donation_upgrade_3") }
        binding.cardViewFive.setOnClickListener { startBuyProcess("donation_upgrade_5") }
    }

    private fun startBuyProcess(sku: String) {

        billingClient = BillingClient.newBuilder(applicationContext).setListener(this).enablePendingPurchases().build()
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready
                    // Start buy process
                    launch {
                        val result = querySkuDetails(sku)
                        onResult(result)
                    }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Snackbar.make(binding.donateView, R.string.donation_cancelled, Snackbar.LENGTH_SHORT)
                        .show()
            }
        })
    }

    private fun onResult(skuDetailsLis: SkuDetailsResult) {
        // Only one item at a time
        val skuDetails = skuDetailsLis.skuDetailsList!![0]

        // Set params of the purchase
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()

        // Launch purchase
        billingClient.launchBillingFlow(this@DonateActivity, flowParams)

    }

    private suspend fun querySkuDetails(sku: String): SkuDetailsResult {
        val skuList = ArrayList<String>()
        skuList.add(sku)

        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        // Return SkuDetails
        return withContext(Dispatchers.IO) {
            billingClient.querySkuDetails(params.build())
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            // Acknowledge purchase
            launch {
                handlePurchase(purchases[0])
            }

            // Consume it so it can be purchased again
            launch {
                consumePurchase(purchases[0])
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Snackbar.make(binding.donateView, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        } else {
            // Handle any other error codes.
            Snackbar.make(binding.donateView, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant entitlement to the user.
            Snackbar.make(binding.donateView, "Thank you!", Snackbar.LENGTH_SHORT).show()
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build())
                }
            }
        }
    }

    private suspend fun consumePurchase(purchase: Purchase) {
        val consumeParams =
                ConsumeParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
        withContext(Dispatchers.IO) {
            billingClient.consumePurchase(consumeParams)
        }
    }
}
