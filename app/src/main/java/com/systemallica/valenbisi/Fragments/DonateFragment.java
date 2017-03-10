package com.systemallica.valenbisi.Fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdView;
import com.spark.submitbutton.SubmitButton;
import com.systemallica.valenbisi.MainActivity;
import com.systemallica.valenbisi.R;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class DonateFragment extends Fragment{

    public static final String PREFS_NAME = "MyPrefsFile";// Debug tag, for logging

    @BindView(R.id.textRemove) TextView textRemove;
    @BindView(R.id.textDonate) TextView textDonate;
    @BindView(R.id.donatorImage) ImageView donatorImage;

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

        SubmitButton  btn_remove_ads = (SubmitButton)view.findViewById(R.id.btn_remove_ads);
        SubmitButton btn_buy = (SubmitButton)view.findViewById(R.id.btn_buy);

        donatorImage.setVisibility(GONE);

        SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        boolean removedAds = settings.getBoolean("removedAds", false);
        boolean donationPurchased = settings.getBoolean("donationPurchased", false);

        if(!removedAds){
            textRemove.setText(R.string.ad_remove_hint);
            btn_remove_ads.setText(R.string.ad_remove);
        } else {
            textRemove.setText(R.string.ad_restore_hint);
            btn_remove_ads.setText(R.string.ad_restore);
        }

        if(donationPurchased){
            donatorImage.setVisibility(VISIBLE);
            textRemove.setVisibility(GONE);
            textDonate.setGravity(Gravity.CENTER_VERTICAL);
            textDonate.setText(R.string.donator_thanks);
            btn_remove_ads.setVisibility(GONE);
            btn_buy.setVisibility(GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState){
        final SubmitButton btn_remove_ads = (SubmitButton ) view.findViewById(R.id.btn_remove_ads);

        btn_remove_ads.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                boolean removedAds = settings.getBoolean("removedAds", false);
                AdView mAdView = (AdView) getActivity().findViewById(R.id.adView);

                if(!removedAds) {
                    editor.putBoolean("removedAds", true);
                    editor.apply();
                    textRemove.setText(R.string.ad_restore_hint);
                    Snackbar.make(view, R.string.ads_removed, Snackbar.LENGTH_SHORT).show();

                    mAdView.setVisibility(GONE);
                    //mAdView.destroy();

                }
                else{
                    editor.putBoolean("removedAds", false);
                    editor.apply();
                    textRemove.setText(R.string.ads_restored);
                    Snackbar.make(view, R.string.ads_restored, Snackbar.LENGTH_SHORT).show();

                    if(mAdView!=null) {
                        mAdView.setVisibility(VISIBLE);
                    }

                }

            }
        });

        SubmitButton btn_buy = (SubmitButton)view.findViewById(R.id.btn_buy);
        btn_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity)getActivity()).startBuyProcess();
            }
        });

    }
}
