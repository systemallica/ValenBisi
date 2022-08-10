package com.systemallica.valenbisi.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.android.billingclient.api.*
import com.google.android.material.snackbar.Snackbar
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.databinding.ActivityDonateBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.logging.Logger
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
                        val productDetails = queryProductDetails(sku)
                        launchBillingFlow(productDetails)
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

    private fun launchBillingFlow(productDetails: ProductDetailsResult) {
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails.productDetailsList!![0])
                    .build()
            )

        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

        // Launch purchase
        billingClient.launchBillingFlow(this@DonateActivity, billingFlowParams)
    }

    private suspend fun queryProductDetails(sku: String): ProductDetailsResult {
        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(sku)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)

        return withContext(Dispatchers.IO) {
            billingClient.queryProductDetails(params.build())
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
                    .setPurchaseToken(purchase.purchaseToken).build()
                val acknowledgePurchaseResponseListener =
                    AcknowledgePurchaseResponseListener { Logger.getGlobal().info("Purchase acknowledged") }
                withContext(Dispatchers.IO) {
                    billingClient.acknowledgePurchase(
                        acknowledgePurchaseParams,
                        acknowledgePurchaseResponseListener
                    )
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
