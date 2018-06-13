package com.systemallica.valenbisi.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.google.android.gms.ads.AdView;
import com.systemallica.valenbisi.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DonateFragment extends Fragment implements PurchasesUpdatedListener {

    public static final String PREFS_NAME = "MyPrefsFile";// Debug tag, for logging

    @BindView(R.id.textRemove) TextView textRemove;
    @BindView(R.id.textDonate) TextView textDonate;
    @BindView(R.id.donatorImage) ImageView donatorImage;
    @BindView(R.id.btn_remove_ads) Button btn_remove_ads;
    @BindView(R.id.btn_buy) Button btn_buy;

    public DonateFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_donate, container, false);
        ButterKnife.bind(this, view);

        //Change toolbar title
        getActivity().setTitle(R.string.nav_donate);

        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        donatorImage.setVisibility(GONE);

        SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean removedAds = settings.getBoolean("removedAds", false);
        boolean donationPurchased = settings.getBoolean("donationPurchased", false);

        if (!removedAds) {
            textRemove.setText(R.string.ad_remove_hint);
            btn_remove_ads.setText(R.string.ad_remove);
        } else {
            textRemove.setText(R.string.ad_restore_hint);
            btn_remove_ads.setText(R.string.ad_restore);
        }

        if (donationPurchased) {
            showDonatorStatus();
        }

        btn_remove_ads.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                boolean removedAds = settings.getBoolean("removedAds", false);
                AdView mAdView = getActivity().findViewById(R.id.adView);

                if (!removedAds) {
                    editor.putBoolean("removedAds", true);
                    editor.apply();
                    textRemove.setText(R.string.ad_restore_hint);
                    Snackbar.make(view, R.string.ads_removed, Snackbar.LENGTH_SHORT).show();
                    btn_remove_ads.setText(R.string.ad_restore);
                    mAdView.setVisibility(GONE);
                } else {
                    editor.putBoolean("removedAds", false);
                    editor.apply();
                    textRemove.setText(R.string.ads_restored);
                    Snackbar.make(view, R.string.ads_restored, Snackbar.LENGTH_SHORT).show();
                    btn_remove_ads.setText(R.string.ad_remove);
                    if (mAdView != null) {
                        mAdView.setVisibility(VISIBLE);
                    }
                }

            }
        });

        btn_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startBuyProcess();
            }
        });
    }

    public void startBuyProcess(){
        final BillingClient mBillingClient;

        mBillingClient = BillingClient.newBuilder(getActivity().getApplicationContext()).setListener(this).build();
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    // The billing client is ready
                    // Start buy process
                    BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                            .setSku("donation_upgrade")
                            .setType(BillingClient.SkuType.INAPP)
                            .build();
                    // Launch purchase
                    mBillingClient.launchBillingFlow(getActivity(), flowParams);

                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
                if(getView() != null) {
                    Snackbar.make(getView(), R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@BillingClient.BillingResponse int responseCode, List<Purchase> purchases) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
            if(getView() != null) {
                // Apply preference
                SharedPreferences settings = getActivity().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("donationPurchased", true);
                editor.apply();

                // Destroy Ad
                AdView mAdView = getActivity().findViewById(R.id.adView);
                mAdView.setVisibility(GONE);
                mAdView.destroy();

                showDonatorStatus();
            }
        } else if (responseCode == BillingClient.BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            if(getView() != null) {
                Snackbar.make(getView(), R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            // Handle any other error codes.
            if(getView() != null) {
                Snackbar.make(getView(), R.string.donation_cancelled, Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void showDonatorStatus(){
        // Show feedback to the user
        donatorImage.setVisibility(VISIBLE);
        textRemove.setVisibility(GONE);
        textDonate.setGravity(Gravity.CENTER_VERTICAL);
        textDonate.setText(R.string.donator_thanks);
        btn_remove_ads.setVisibility(GONE);
        btn_buy.setVisibility(GONE);
    }

    //Send donation
    @OnClick(R.id.email) public void email() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:systemallica.apps@gmail.com"));
        startActivity(emailIntent);
    }
}
