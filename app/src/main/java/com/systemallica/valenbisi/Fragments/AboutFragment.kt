package com.systemallica.valenbisi.fragments

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment

import com.systemallica.valenbisi.R
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.mikepenz.iconics.IconicsDrawable
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.community_material_typeface_library.CommunityMaterial


class AboutFragment : MaterialAboutFragment() {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //Change toolbar title
        activity!!.setTitle(R.string.nav_about)
    }

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        val appCardBuilder = MaterialAboutCard.Builder()

        appCardBuilder.addItem(MaterialAboutTitleItem.Builder()
                .text(R.string.app_name)
                .desc("© 2018 Systemallica")
                .icon(R.mipmap.ic_launcher)
                .build())

                .addItem(ConvenienceBuilder.createVersionActionItem(activityContext,
                        IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_information_outline)
                                .sizeDp(18),
                        "Version",
                        true))

                .addItem(ConvenienceBuilder.createRateActionItem(activityContext,
                        IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_star)
                                .sizeDp(18),
                        "Rate this app",
                        null
                ))

        val aboutAppBuilder = MaterialAboutCard.Builder()
        aboutAppBuilder.title("About")

        aboutAppBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Changelog")
                .icon(IconicsDrawable(activityContext)
                        .icon(CommunityMaterial.Icon.cmd_history)
                        .sizeDp(18))
                .setOnClickAction(ConvenienceBuilder.createWebViewDialogOnClickAction(activityContext, "Releases", "https://github.com/systemallica/ValenBisi/releases", true, false))
                .build())

                .addItem(MaterialAboutActionItem.Builder()
                        .text("Fork on GitHub")
                        .icon(IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_github_circle)
                                .sizeDp(18))
                        .setOnClickAction(ConvenienceBuilder.createWebsiteOnClickAction(activityContext, Uri.parse("https://github.com/systemallica/valenbisi")))
                        .build())

                .addItem(MaterialAboutActionItem.Builder()
                        .text("Open source libs")
                        .icon(IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_code_tags)
                                .sizeDp(18))
                        .setOnClickAction {
                            LibsBuilder()
                                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                                    .withAutoDetect(true)
                                    .withAboutIconShown(true)
                                    .withAboutVersionShown(true)
                                    .start(activityContext)
                        }
                        .build())

        val authorCardBuilder = MaterialAboutCard.Builder()
        authorCardBuilder.title("Author")

        authorCardBuilder.addItem(MaterialAboutActionItem.Builder()
                .text("Andrés Reverón")
                .subText("Spain")
                .icon(IconicsDrawable(activityContext)
                        .icon(CommunityMaterial.Icon.cmd_account)
                        .sizeDp(18))
                .build())

                .addItem(ConvenienceBuilder.createWebsiteActionItem(activityContext,
                        IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_earth)
                                .sizeDp(18),
                        "Visit my website",
                        true,
                        Uri.parse("http://andres.reveronmolina.me")))

                .addItem(ConvenienceBuilder.createEmailItem(activityContext,
                        IconicsDrawable(activityContext)
                                .icon(CommunityMaterial.Icon.cmd_email)
                                .sizeDp(18),
                        "Send me an email",
                        true,
                        "andres@reveronmolina.me",
                        "Question concerning ValenBisi?"))

        return MaterialAboutList(appCardBuilder.build(), aboutAppBuilder.build(), authorCardBuilder.build())
    }

    override fun getTheme(): Int {
        return R.style.AppTheme_MaterialAboutActivity_Fragment
    }
}

