package com.gracecode.iZhihu.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import com.gracecode.iZhihu.R;
import com.gracecode.iZhihu.util.Helper;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

abstract class BaseActivity extends Activity {
    static ActionBar actionBar;
    protected static Context context;
    static SharedPreferences mSharedPreferences;
    private static PackageInfo packageInfo;
    private boolean openAnalytics = true;

    BaseActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // setTheme(android.R.style.Theme_Holo_Light);

        actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setIcon(android.R.color.transparent);

        context = getApplicationContext();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        openAnalytics = mSharedPreferences.getBoolean(getString(R.string.key_analytics), true);
        if (openAnalytics) {
            MobclickAgent.onError(this);
        }

        UmengUpdateAgent.update(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (openAnalytics) {
            MobclickAgent.setDebugMode(true);
            MobclickAgent.onResume(context);
        }
    }

    @Override
    public void onPause() {
        if (openAnalytics) {
            MobclickAgent.onPause(context);
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_preference:
                intent = new Intent(this, Preference.class);
                startActivity(intent);
                return true;

            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_feedback:
                String subject =
                        String.format(getString(R.string.feedback_title),
                                getString(R.string.app_name), packageInfo.versionName);
                Helper.sendMail(this, new String[]{getString(R.string.author_email)}, subject, null);
                return true;

            case R.id.menu_about:
                intent = new Intent(this, About.class);
                startActivity(intent);
                return true;

            case R.id.menu_donate:
                Helper.openWithBrowser(this, getString(R.string.url_donate));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
