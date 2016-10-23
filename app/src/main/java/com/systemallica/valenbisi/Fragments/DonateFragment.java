package com.systemallica.valenbisi.Fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
    public void onViewCreated(final View view, Bundle savedInstanceState){
        final Button btn_buy = (Button) view.findViewById(R.id.btn_buy);


        btn_buy.setOnClickListener(new View.OnClickListener() {

            int i = 0;
            TextView tv = (TextView)view.findViewById(R.id.textRemove);
            public void onClick(View v) {

                if(i==0) {
                    SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("removedAds", true);
                    editor.apply();
                    btn_buy.setText(R.string.ad_restore);
                    tv.setText(R.string.ad_restore_hint);
                    i = 1;
                }
                else{
                    SharedPreferences settings = getActivity().getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean("removedAds", false);
                    editor.apply();
                    btn_buy.setText(R.string.ad_remove);
                    tv.setText(R.string.ad_remove_hint);
                    i = 0;
                }

            }
        });
    }
}
