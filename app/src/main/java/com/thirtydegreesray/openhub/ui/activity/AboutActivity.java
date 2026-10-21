

package com.thirtydegreesray.openhub.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danielstone.materialaboutlibrary.MaterialAboutActivity;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.thirtydegreesray.openhub.BuildConfig;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.util.AppOpener;
import com.thirtydegreesray.openhub.util.AppUtils;
import com.thirtydegreesray.openhub.util.ThemeHelper;
import com.thirtydegreesray.openhub.util.UpdateChecker;

import es.dmoral.toasty.Toasty;

/**
 * Created by ThirtyDegreesRay on 2017/8/29.
 */

public class AboutActivity extends MaterialAboutActivity {

    public static void show(@NonNull Context context){
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    private boolean isAlive = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        isAlive = true;
        ThemeHelper.applyForAboutActivity(this);
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    protected MaterialAboutList getMaterialAboutList(@NonNull Context context) {
        MaterialAboutCard.Builder appBuilder = new MaterialAboutCard.Builder();
        buildApp(appBuilder, context);
        MaterialAboutCard.Builder authorBuilder = new MaterialAboutCard.Builder();
        buildAuthor(authorBuilder, context);
        MaterialAboutCard.Builder forkBuilder = new MaterialAboutCard.Builder();
        buildForkDeveloper(forkBuilder, context);
        MaterialAboutCard.Builder shareBuilder = new MaterialAboutCard.Builder();
        buildShare(shareBuilder, context);
        return new MaterialAboutList(appBuilder.build(), authorBuilder.build(),
                forkBuilder.build(), shareBuilder.build());
    }

    @Nullable
    @Override
    protected CharSequence getActivityTitle() {
        return getString(R.string.about);
    }

    private void buildApp(MaterialAboutCard.Builder appBuilder, final Context context){
        appBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .text(getString(R.string.app_github_name))
                .desc(getString(R.string.app_copyright))
                .icon(R.mipmap.logo)
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.version)
                .subText(BuildConfig.VERSION_NAME)
                .icon(R.drawable.ic_menu_about)
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.source_code)
                .subText(R.string.source_code_wishes)
                .icon(R.drawable.ic_code)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        RepositoryActivity.show(context, getString(R.string.fork_developer_login_id), getString(R.string.app_github_name));
                    }
                })
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.check_update)
                .icon(R.drawable.ic_refresh)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        checkUpdate();
                    }
                })
                .build());
    }

    private void buildForkDeveloper(MaterialAboutCard.Builder forkBuilder, final Context context){
        forkBuilder.title(R.string.fork_developer);
        forkBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.fork_developer_name)
                .subText(R.string.fork_developer_login_id)
                .icon(R.drawable.ic_menu_person)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        ProfileActivity.show(AboutActivity.this, getString(R.string.fork_developer_login_id),
                                getString(R.string.fork_developer_avatar_url));
                    }
                })
                .build());
        forkBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.fork_repository)
                .subText(R.string.source_code_wishes)
                .icon(R.drawable.ic_code)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        RepositoryActivity.show(context, getString(R.string.fork_developer_login_id),
                                getString(R.string.app_github_name));
                    }
                })
                .build());
        forkBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.follow_on_github)
                .icon(R.drawable.ic_github)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        ProfileActivity.show(AboutActivity.this, getString(R.string.fork_developer_login_id),
                                getString(R.string.fork_developer_avatar_url));
                    }
                })
                .build());
        forkBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText(R.string.fork_developer_email)
                .icon(R.drawable.ic_mail)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppOpener.launchEmail(context, getString(R.string.fork_developer_email));
                    }
                })
                .setOnLongClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppUtils.copyToClipboard(context, getString(R.string.fork_developer_email));
                    }
                })
                .build());
    }

    private void checkUpdate(){
        if(!isAlive) return;
        Toasty.info(this, getString(R.string.checking_update), android.widget.Toast.LENGTH_SHORT).show();
        UpdateChecker.check(new UpdateChecker.Callback() {
            @Override
            public void onResult(UpdateChecker.Result result, String error) {
                if(!isAlive) return;
                if(error != null || result == null){
                    Toasty.error(AboutActivity.this,
                            getString(R.string.update_check_failed, error == null ? "" : error),
                            android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                if(result.hasUpdate){
                    new MaterialDialog.Builder(AboutActivity.this)
                            .title(getString(R.string.update_available_title) + " " + result.latestVersion)
                            .content(getString(R.string.update_available_message,
                                    result.latestVersion, BuildConfig.VERSION_NAME))
                            .positiveText(R.string.update_download)
                            .negativeText(R.string.cancel)
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull com.afollestad.materialdialogs.DialogAction which) {
                                    AppOpener.openInCustomTabsOrBrowser(AboutActivity.this,
                                            result.downloadUrl);
                                }
                            })
                            .show();
                } else {
                    Toasty.success(AboutActivity.this, getString(R.string.update_latest_version),
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void buildAuthor(MaterialAboutCard.Builder appBuilder, final Context context){
        appBuilder.title(R.string.author);
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.author_name)
                .subText(R.string.author_location)
                .icon(R.drawable.ic_menu_person)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        ProfileActivity.show(AboutActivity.this, getString(R.string.author_login_id),
                                getString(R.string.author_avatar_url));
                    }
                })
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.follow_on_github)
                .icon(R.drawable.ic_github)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        ProfileActivity.show(AboutActivity.this, getString(R.string.author_login_id),
                                getString(R.string.author_avatar_url));
                    }
                })
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.email)
                .subText(R.string.auth_email_address)
                .icon(R.drawable.ic_mail)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppOpener.launchEmail(context, getString(R.string.auth_email_address));
                    }
                })
                .setOnLongClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppUtils.copyToClipboard(context, getString(R.string.auth_email_address));
                    }
                })
                .build());
    }

    private void buildShare(MaterialAboutCard.Builder appBuilder, final Context context) {
        appBuilder.title(R.string.feedback_and_share);
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.share_to_friends)
                .icon(R.drawable.ic_share)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppOpener.shareText(AboutActivity.this, getString(R.string.cookapk_download_url));
                    }
                })
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.rate_in_market)
                .icon(R.drawable.ic_menu_star)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        AppOpener.openInMarket(context);
                    }
                })
                .build());
        appBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.feedback)
                .icon(R.drawable.ic_feedback)
                .setOnClickAction(new MaterialAboutItemOnClickAction() {
                    @Override
                    public void onClick() {
                        IssuesActivity.showForRepo(AboutActivity.this,
                                getString(R.string.author_login_id), getString(R.string.app_name));
                    }
                })
                .build());
    }

    @Override
    protected void onDestroy() {
        isAlive = false;
        super.onDestroy();
    }

}
