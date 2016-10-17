package com.systemallica.valenbisi.Fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.systemallica.valenbisi.BuildConfig;
import com.systemallica.valenbisi.R;

public class AboutFragment extends Fragment {
    ImageView github;
    TextView version1;

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
        github =(ImageView)view.findViewById(R.id.github);
        github.setOnClickListener(new View.OnClickListener()   {
            public void onClick(View v)  {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/systemallica/ValenBisi"));
                startActivity(browserIntent);
            }
        });

        version1 = (TextView)view.findViewById(R.id.version1);
        version1.setText(BuildConfig.VERSION_NAME);
        return view;
    }


}

