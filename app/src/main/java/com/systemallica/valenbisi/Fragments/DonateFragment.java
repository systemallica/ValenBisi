package com.systemallica.valenbisi.Fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.systemallica.valenbisi.R;

public class DonateFragment extends Fragment {

    View view;
    public static final String PREFS_NAME = "MyPrefsFile";

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
        view = inflater.inflate(R.layout.fragment_donate, container, false);

        //Change toolbar title
        getActivity().setTitle(R.string.nav_donate);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        final Button btn_buy = (Button) view.findViewById(R.id.btn_buy);

        btn_buy.setOnClickListener(new View.OnClickListener() {


            public void onClick(View v) {

                SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("removedAds", true);
                editor.putBoolean("isChanged", true);
                editor.apply();

            }
        });
    }
}
