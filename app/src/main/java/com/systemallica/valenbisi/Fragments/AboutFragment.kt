package com.systemallica.valenbisi.fragments

import android.support.v4.app.Fragment
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.systemallica.valenbisi.BuildConfig
import com.systemallica.valenbisi.R

import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //Change toolbar title
        activity!!.setTitle(R.string.nav_about)

        //Add version number to the textview
        versionCode.text = BuildConfig.VERSION_NAME

        setClickListeners()
    }

    private fun setClickListeners() {
        //Open GitHub page
        github.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/systemallica/ValenBisi"))
            startActivity(browserIntent)
        }

        //Send email
        email.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO)
            emailIntent.data = Uri.parse("mailto:systemallica.apps@gmail.com")
            startActivity(emailIntent)
        }

        // Rate app
        rate.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.systemallica.valenbisi"))
            startActivity(browserIntent)
        }
    }
}

