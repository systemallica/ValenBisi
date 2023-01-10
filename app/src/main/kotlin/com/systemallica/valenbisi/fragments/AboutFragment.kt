package com.systemallica.valenbisi.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.danielstone.materialaboutlibrary.ConvenienceBuilder
import com.danielstone.materialaboutlibrary.MaterialAboutFragment
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.mikepenz.aboutlibraries.LibsBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import com.systemallica.valenbisi.R
import com.systemallica.valenbisi.activities.DonateActivity

class AboutFragment : MaterialAboutFragment() {

    override fun getMaterialAboutList(activityContext: Context): MaterialAboutList {
        val appCardBuilder = MaterialAboutCard.Builder()

        appCardBuilder.addItem(
            MaterialAboutTitleItem.Builder()
                .text(R.string.app_name)
                .desc(getString(R.string.about_developer))
                .icon(R.mipmap.ic_launcher)
                .build()
        )
            .addItem(
                ConvenienceBuilder.createVersionActionItem(
                    activityContext,
                    IconicsDrawable(activityContext, CommunityMaterial.Icon2.cmd_information_outline).apply {
                        sizeDp = 18
                    },
                    getString(R.string.about_version),
                    true
                )
            )
            .addItem(
                ConvenienceBuilder.createRateActionItem(
                    activityContext,
                    IconicsDrawable(activityContext, CommunityMaterial.Icon3.cmd_star_outline).apply {
                        sizeDp = 18
                    },
                    getString(R.string.about_rate),
                    null
                )
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(getString(R.string.about_support))
                    .icon(
                        IconicsDrawable(activityContext, CommunityMaterial.Icon2.cmd_heart_outline).apply {
                            sizeDp = 18
                        }
                    )
                    .setOnClickAction {
                        val intent = Intent(activityContext, DonateActivity::class.java)
                        startActivity(intent)
                    }
                    .build()
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(getString(R.string.nav_share))
                    .icon(
                        IconicsDrawable(activityContext, CommunityMaterial.Icon3.cmd_share_outline).apply {
                            sizeDp = 18
                        }
                    )
                    .setOnClickAction {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://play.google.com/store/apps/details?id=com.systemallica.valenbisi"
                            )
                            type = "text/plain"
                        }
                        startActivity(Intent.createChooser(sendIntent, getString(R.string.nav_share)))
                    }
                    .build()
            )

        val aboutAppBuilder = MaterialAboutCard.Builder()
        aboutAppBuilder.title(getString(R.string.about_about))

        aboutAppBuilder.addItem(
            MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_changelog))
                .icon(
                    IconicsDrawable(activityContext, CommunityMaterial.Icon2.cmd_history).apply {
                        sizeDp = 18
                    }
                )
                .setOnClickAction(
                    ConvenienceBuilder.createWebViewDialogOnClickAction(
                        activityContext,
                        getString(R.string.about_releases),
                        getString(R.string.about_versions_url),
                        true,
                        false
                    )
                )
                .build()
        )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(getString(R.string.about_fork))
                    .icon(
                        IconicsDrawable(activityContext, CommunityMaterial.Icon2.cmd_github).apply {
                            sizeDp = 18
                        }
                    )
                    .setOnClickAction(
                        ConvenienceBuilder.createWebsiteOnClickAction(
                            activityContext,
                            Uri.parse(getString(R.string.about_url))
                        )
                    )
                    .build()
            )
            .addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    activityContext,
                    IconicsDrawable(activityContext, CommunityMaterial.Icon.cmd_alert_circle_outline).apply {
                        sizeDp = 18
                    },
                    getString(R.string.about_issue),
                    false,
                    Uri.parse(getString(R.string.about_issue_url))
                )
            )
            .addItem(
                MaterialAboutActionItem.Builder()
                    .text(getString(R.string.about_libs))
                    .icon(
                        IconicsDrawable(activityContext, CommunityMaterial.Icon.cmd_code_tags).apply {
                            sizeDp = 18
                        }
                    )
                    .setOnClickAction {
                        LibsBuilder()
                            .withAboutIconShown(true)
                            .withAboutVersionShown(true)
                            .start(activityContext)
                    }
                    .build()
            )

        val authorCardBuilder = MaterialAboutCard.Builder()
        authorCardBuilder.title(getString(R.string.about_author))

        authorCardBuilder.addItem(
            MaterialAboutActionItem.Builder()
                .text(getString(R.string.about_author_name))
                .subText(getString(R.string.about_authot_location))
                .icon(
                    IconicsDrawable(activityContext, CommunityMaterial.Icon.cmd_account_outline).apply {
                        sizeDp = 18
                    }
                )
                .build()
        )
            .addItem(
                ConvenienceBuilder.createWebsiteActionItem(
                    activityContext,
                    IconicsDrawable(activityContext, CommunityMaterial.Icon.cmd_earth).apply {
                        sizeDp = 18
                    },
                    getString(R.string.about_visit),
                    true,
                    Uri.parse(getString(R.string.about_author_web))
                )
            )
            .addItem(
                ConvenienceBuilder.createEmailItem(
                    activityContext,
                    IconicsDrawable(activityContext, CommunityMaterial.Icon.cmd_email_outline).apply {
                        sizeDp = 18
                    },
                    getString(R.string.about_send_email),
                    true,
                    getString(R.string.about_author_email),
                    getString(R.string.about_author_email_subject)
                )
            )

        return MaterialAboutList(
            appCardBuilder.build(),
            aboutAppBuilder.build(),
            authorCardBuilder.build()
        )
    }
}
