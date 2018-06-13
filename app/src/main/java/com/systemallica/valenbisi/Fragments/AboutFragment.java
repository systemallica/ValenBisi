package com.systemallica.valenbisi.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.systemallica.valenbisi.BuildConfig;
import com.systemallica.valenbisi.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AboutFragment extends Fragment {

    @BindView(R.id.version_code) TextView version_code;

    public AboutFragment() {
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
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, view);

        //Change toolbar title
        getActivity().setTitle(R.string.nav_about);

        //Add version number to the textview
        version_code.setText(BuildConfig.VERSION_NAME);

        return view;
    }

    //Open GitHub page
    @OnClick(R.id.github) public void github() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/systemallica/ValenBisi"));
        startActivity(browserIntent);
    }

    //Send email
    @OnClick(R.id.email) public void email() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:systemallica.apps@gmail.com"));
        startActivity(emailIntent);
    }

    //Open Play Store
    @OnClick(R.id.rate) public void rate() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.systemallica.valenbisi"));
        startActivity(browserIntent);
    }


}

