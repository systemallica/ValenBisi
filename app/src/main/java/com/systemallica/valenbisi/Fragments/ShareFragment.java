package com.systemallica.valenbisi.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;


public class ShareFragment extends Fragment {


    public ShareFragment() {
        // Required empty public constructor
    }


    @Override
     public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey check out my app at: https://www.google.com");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }
}
