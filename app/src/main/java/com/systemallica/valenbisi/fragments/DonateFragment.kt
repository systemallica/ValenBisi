package com.systemallica.valenbisi.fragments

import android.support.v4.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.google.android.gms.ads.AdView
import com.systemallica.valenbisi.R

import android.view.View.GONE
import android.view.View.VISIBLE

import kotlinx.android.synthetic.main.fragment_donate.*

class DonateFragment : Fragment(), PurchasesUpdatedListener {

    private val prefsName = "MyPrefsFile"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_donate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Change toolbar title
        activity!!.setTitle(R.string.nav_donate)

        donatorImage.visibility = GONE

        val settings = activity!!.applicationContext.getSharedPreferences(prefsName, 0)
        val removedAds = settings.getBoolean("removedAds", false)
        val donationPurchased = settings.getBoolean("donationPurchased", false)

        if (!removedAds) {
            textRemove.setText(R.string.ad_remove_hint)
            btn_remove_ads.setText(R.string.ad_remove)
        } else {
            textRemove.setText(R.string.ad_restore_hint)
            btn_remove_ads.setText(R.string.ad_restore)
        }

        if (donationPurchased) {
            showDonorStatus()
        }

        setClickListeners()
    }

    private fun setClickListeners() {
        btn_remove_ads.setOnClickListener {
            val settings = activity!!.applicationContext.getSharedPreferences(prefsName, 0)
            val removedAds = settings.getBoolean("removedAds", false)
            val editor = settings.edit()
            val adView = activity!!.findViewById<AdView>(R.id.adView)

            if (!removedAds) {
                editor.putBoolean("removedAds", true).apply()
                textRemove.setText(R.string.ad_restore_hint)
                Snackbar.make(donateView, R.string.ads_removed, Snackbar.LENGTH_SHORT).show()
                btn_remove_ads.setText(R.string.ad_restore)
                adView.visibility = GONE
            } else {
                editor.putBoolean("removedAds", false)
                editor.apply()
                textRemove.setText(R.string.ads_restored)
                Snackbar.make(donateView, R.string.ads_restored, Snackbar.LENGTH_SHORT).show()
                btn_remove_ads.setText(R.string.ad_remove)
                if (adView != null) {
                    adView.visibility = VISIBLE
                }
            }
        }

        email.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.data = Uri.parse("mailto:systemallica.apps@gmail.com")
            startActivity(emailIntent)
        }

        btn_buy.setOnClickListener { startBuyProcess() }
    }

    private fun startBuyProcess() {
        val mBillingClient: BillingClient =
            BillingClient.newBuilder(activity!!.applicationContext).setListener(this).build()
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready
                    // Start buy process
                    val flowParams = BillingFlowParams.newBuilder()
                        .setSku("donation_upgrade")
                        .setType(BillingClient.SkuType.INAPP)
                        .build()
                    // Launch purchase
                    mBillingClient.launchBillingFlow(activity, flowParams)

                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                Snackbar.make(view!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    override fun onPurchasesUpdated(@BillingClient.BillingResponse responseCode: Int, purchases: List<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            // Apply preference
            val settings = activity!!.getSharedPreferences(prefsName, 0)
            val editor = settings.edit()
            editor.putBoolean("donationPurchased", true).apply()

            // Destroy Ad
            val mAdView = activity!!.findViewById<AdView>(R.id.adView)
            mAdView.visibility = GONE
            mAdView.destroy()

            showDonorStatus()
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Snackbar.make(view!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        } else {
            // Handle any other error codes.
            Snackbar.make(view!!, R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showDonorStatus() {
        // Show feedback to the user
        donatorImage.visibility = VISIBLE
        textRemove.visibility = GONE
        textDonate.gravity = Gravity.CENTER_VERTICAL
        textDonate.setText(R.string.donator_thanks)
        btn_remove_ads.visibility = GONE
        btn_buy.visibility = GONE
    }

}
