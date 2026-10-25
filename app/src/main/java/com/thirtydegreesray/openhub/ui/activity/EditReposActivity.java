

package com.thirtydegreesray.openhub.ui.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.thirtydegreesray.dataautoaccess.annotation.AutoAccess;
import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerActivityComponent;
import com.thirtydegreesray.openhub.inject.module.ActivityModule;
import com.thirtydegreesray.openhub.mvp.contract.IEditReposContract;
import com.thirtydegreesray.openhub.mvp.model.RepoEditInfo;
import com.thirtydegreesray.openhub.mvp.model.Repository;
import com.thirtydegreesray.openhub.mvp.presenter.EditReposPresenter;
import com.thirtydegreesray.openhub.ui.activity.base.BaseActivity;
import com.thirtydegreesray.openhub.util.BundleHelper;
import com.thirtydegreesray.openhub.util.StringUtils;

import java.util.regex.Pattern;

import butterknife.BindView;

/**
 * Create, edit and delete a repository of the authenticated user.
 */
public class EditReposActivity extends BaseActivity<EditReposPresenter>
        implements IEditReposContract.View {

    private static final int DEFAULT_REQUEST_CODE = 100;

    private static final Pattern REPO_NAME_PATTERN =
            Pattern.compile("^[A-Za-z0-9._-]{1,100}$");

    public static void showForAdd(@NonNull Activity activity, int requestCode) {
        Intent intent = new Intent(activity, EditReposActivity.class);
        intent.putExtras(BundleHelper.builder().put("addMode", true).build());
        activity.startActivityForResult(intent, requestCode);
    }

    public static void showForAdd(@NonNull Activity activity) {
        showForAdd(activity, DEFAULT_REQUEST_CODE);
    }

    public static void showForEdit(@NonNull Activity activity, @NonNull Repository repository,
                                   int requestCode) {
        Intent intent = new Intent(activity, EditReposActivity.class);
        intent.putExtras(BundleHelper.builder().put("addMode", false)
                .put("repository", repository).build());
        activity.startActivityForResult(intent, requestCode);
    }

    public static void showForEdit(@NonNull Activity activity, @NonNull Repository repository) {
        showForEdit(activity, repository, DEFAULT_REQUEST_CODE);
    }

    /** opens the edit page and immediately asks for the delete confirmation */
    public static void showForDelete(@NonNull Activity activity, @NonNull Repository repository) {
        Intent intent = new Intent(activity, EditReposActivity.class);
        intent.putExtras(BundleHelper.builder().put("addMode", false)
                .put("deleteMode", true)
                .put("repository", repository).build());
        activity.startActivityForResult(intent, DEFAULT_REQUEST_CODE);
    }

    @AutoAccess boolean deleteMode;

    @BindView(R.id.name_edit) TextInputEditText nameEdit;
    @BindView(R.id.desc_edit) TextInputEditText descEdit;
    @BindView(R.id.homepage_edit) TextInputEditText homepageEdit;
    @BindView(R.id.private_switch) SwitchCompat privateSwitch;
    @BindView(R.id.auto_init_switch) SwitchCompat autoInitSwitch;
    @BindView(R.id.issues_switch) SwitchCompat issuesSwitch;
    @BindView(R.id.wiki_switch) SwitchCompat wikiSwitch;
    @BindView(R.id.projects_switch) SwitchCompat projectsSwitch;
    @BindView(R.id.archived_switch) SwitchCompat archivedSwitch;
    @BindView(R.id.delete_repo_text) TextView deleteReposText;

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_edit_repos;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarBackEnable();
        final boolean addMode = mPresenter.isAddMode();
        setToolbarTitle(addMode ? getString(R.string.repo_new) : getString(R.string.repo_edit));

        autoInitSwitch.setVisibility(addMode ? View.VISIBLE : View.GONE);
        archivedSwitch.setVisibility(addMode ? View.GONE : View.VISIBLE);
        deleteReposText.setVisibility(addMode ? View.GONE : View.VISIBLE);
        showDeleteSection(!addMode);

        Repository repository = mPresenter.getRepository();
        if (repository != null) {
            nameEdit.setText(repository.getName());
            nameEdit.setSelection(nameEdit.length());
            descEdit.setText(repository.getDescription());
            homepageEdit.setText(repository.getHomepage());
            privateSwitch.setChecked(repository.isRepPrivate());
            issuesSwitch.setChecked(repository.isHasIssues());
            wikiSwitch.setChecked(repository.isHasWiki());
            projectsSwitch.setChecked(repository.isHasProjects());
            archivedSwitch.setChecked(repository.isArchived());
        } else {
            issuesSwitch.setChecked(true);
            wikiSwitch.setChecked(true);
            projectsSwitch.setChecked(true);
            autoInitSwitch.setChecked(true);
        }

        if (!addMode) {
            deleteReposText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteConfirmDialog();
                }
            });
        }

        if (!addMode && deleteMode) {
            nameEdit.post(new Runnable() {
                @Override
                public void run() {
                    showDeleteConfirmDialog();
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_repos, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save_repo) {
            save();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        String name = nameEdit.getText().toString().trim();
        if (StringUtils.isBlank(name)) {
            showWarningToast(getString(R.string.repo_name_empty));
            nameEdit.requestFocus();
            return;
        }
        if (!REPO_NAME_PATTERN.matcher(name).matches() || ".".equals(name) || "..".equals(name)) {
            showWarningToast(getString(R.string.repo_name_invalid));
            nameEdit.requestFocus();
            return;
        }

        RepoEditInfo info = new RepoEditInfo();
        info.name = name;
        info.description = descEdit.getText().toString().trim();
        info.homepage = homepageEdit.getText().toString().trim();
        info.isPrivate = privateSwitch.isChecked();
        info.autoInit = autoInitSwitch.isChecked();
        info.hasIssues = issuesSwitch.isChecked();
        info.hasWiki = wikiSwitch.isChecked();
        info.hasProjects = projectsSwitch.isChecked();
        info.archived = archivedSwitch.isChecked();

        if (mPresenter.isAddMode()) {
            showProgressDialog(getString(R.string.repo_creating));
        } else {
            showProgressDialog(getString(R.string.repo_saving));
        }
        mPresenter.saveRepo(info);
    }

    private void showDeleteConfirmDialog() {
        final String fullName = mPresenter.getFullName();
        if (StringUtils.isBlank(fullName)) {
            showWarningToast(getString(R.string.repo_delete_missing_name));
            return;
        }
        final EditText editText = new EditText(getActivity());
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        editText.setHint(fullName);

        FrameLayout container = new FrameLayout(getActivity());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        float density = getResources().getDisplayMetrics().density;
        int padding = (int) (20 * density);
        params.leftMargin = padding;
        params.rightMargin = padding;
        params.topMargin = (int) (8 * density);
        editText.setLayoutParams(params);
        container.addView(editText);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.repo_delete)
                .setMessage(getString(R.string.repo_delete_confirm_msg, fullName))
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.repo_delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input = editText.getText().toString().trim();
                        if (!fullName.equals(input)) {
                            showWarningToast(getString(R.string.repo_delete_name_mismatch));
                            return;
                        }
                        showProgressDialog(getString(R.string.repo_deleting));
                        mPresenter.deleteRepo(input);
                    }
                })
                .show();
    }

    @Override
    public void showSaveSuccess(@NonNull Repository repository, boolean addMode) {
        dismissProgressDialog();
        showSuccessToast(getString(addMode ? R.string.repo_created : R.string.repo_updated,
                repository == null ? "" : repository.getFullName()));
        Intent data = new Intent();
        if (repository != null) {
            data.putExtra("repository", repository);
        }
        data.putExtra("addMode", addMode);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void showDeleteSuccess(@NonNull String fullName) {
        dismissProgressDialog();
        showSuccessToast(getString(R.string.repo_deleted, fullName));
        Intent data = new Intent();
        data.putExtra("deleted", true);
        data.putExtra("fullName", fullName);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void showDeleteSection(boolean visible) {
        if (deleteReposText != null) {
            deleteReposText.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

}
