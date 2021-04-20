package com.sagikor.android.fitracker.ui.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;


import cn.pedant.SweetAlert.SweetAlertDialog;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;


import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.sagikor.android.fitracker.R;
import com.sagikor.android.fitracker.ui.contracts.MainActivityContract;
import com.sagikor.android.fitracker.ui.presenter.MainActivityPresenter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MainActivityContract.View {

    private static final String TAG = "MainActivity";
    Button btnAddStudent;
    Button btnSearchStudent;
    Button btnMailResults;
    Button btnUpdateStudent;
    ProgressBar progressBar;
    ImageView ivBackgroundImage;
    MaterialToolbar toolbar;
    MainActivityPresenter presenter;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        addButtonsOnClickListeners();
        initFirebase();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (presenter == null)
            presenter = new MainActivityPresenter();
        presenter.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        presenter.unbind();
    }

    @Override
    public void navToAddStudentScreen() {
        startActivity(new Intent(this, AddStudentActivity.class));
    }

    @Override
    public void navToUpdateStudentScreen() {
        navToViewStudentsScreen();
    }

    @Override
    public void navToViewStudentsScreen() {
        startActivity(new Intent(this, ViewStudentsActivity.class));
    }

    @Override
    public void navToSettingsScreen() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void navToStatisticsScreen() {
        startActivity(new Intent(this, StatisticsActivity.class));
    }

    @Override
    public void sendDatabaseToEmail() {
        Thread sender = new EmailSendThread();
        sender.start();
    }

    @Override
    public void setActiveMode() {
        hideProgressBar();
        setViewsEnableMode(true);
    }

    @Override
    public void setLoadingMode() {
        showProgressBar();
        setViewsEnableMode(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main_drawer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_statistics) {
            presenter.onNavToStatisticsClick();
        } else if (id == R.id.nav_settings) {
            presenter.onNavToSettingsClick();
        } else if (id == R.id.nav_sign_out) {
            presenter.onDisconnectClick();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void disconnectUser() {
        signOutQuestionPop();
    }

    @Override
    public void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }

    private void initFirebase() {
        FirebaseApp.initializeApp(this);
    }

    private void setViewsEnableMode(boolean isEnable) {
        btnAddStudent.setClickable(isEnable);
        btnSearchStudent.setClickable(isEnable);
        btnMailResults.setClickable(isEnable);
        btnUpdateStudent.setClickable(isEnable);
        ivBackgroundImage.setAlpha(isEnable ? 1f : 0.1f);
        btnAddStudent.setAlpha(isEnable ? 1f : 0.1f);
        btnSearchStudent.setAlpha(isEnable ? 1f : 0.1f);
        btnMailResults.setAlpha(isEnable ? 1f : 0.1f);
        btnUpdateStudent.setAlpha(isEnable ? 1f : 0.1f);
        toolbar.setAlpha(isEnable ? 1f : 0f);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        btnAddStudent = findViewById(R.id.button_add_student);
        btnSearchStudent = findViewById(R.id.button_search);
        btnMailResults = findViewById(R.id.button_mail_results);
        btnUpdateStudent = findViewById(R.id.button_update_student);
        progressBar = findViewById(R.id.main_activity_progress_bar);
        ivBackgroundImage = findViewById(R.id.background_image);
        setButtonsBackgroundAlpha();
    }

    private void setButtonsBackgroundAlpha() {
        btnAddStudent.getBackground().setAlpha(50);
        btnSearchStudent.getBackground().setAlpha(50);
        btnMailResults.getBackground().setAlpha(50);
        btnUpdateStudent.getBackground().setAlpha(50);
    }

    private void addButtonsOnClickListeners() {
        btnAddStudent.setOnClickListener(e -> presenter.onNavToAddStudentClick());
        btnSearchStudent.setOnClickListener(e -> presenter.onNavToViewStudentsClick());
        btnMailResults.setOnClickListener(e -> presenter.onSendToEmailClick());
        btnUpdateStudent.setOnClickListener(e -> presenter.onNavToUpdateStudentClick());
    }

    private void signOutQuestionPop() {
        final String SURE_QUESTION = getString(R.string.exit_question);
        final String YES = getString(R.string.yes);
        final String NO = getString(R.string.no);
        new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                .setTitleText(SURE_QUESTION)
                .setConfirmText(YES)
                .setConfirmClickListener(sDialog -> {
                    sDialog.dismissWithAnimation();
                    signOut();
                })
                .setCancelButton(NO, SweetAlertDialog::dismissWithAnimation)
                .show();

    }

    private void signOut() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
        finish();
    }

    @Override
    public void popMessage(String message, msgType type) {
        View contextView = findViewById(R.id.main_activity_root);
        Snackbar.make(contextView, message, Snackbar.LENGTH_SHORT)
                .setBackgroundTint(getColor(R.color.colorPrimary))
                .show();
    }

    private class EmailSendThread extends Thread {
        private static final String TAG = "EmailSendThread";

        @Override
        public void run() {
            final String students_grade = getString(R.string.students_grades);
            //get students data
            StringBuilder sb = new StringBuilder();
            sb.append(getExcelHeaders());
            sb.append(presenter.getStudentsAsCSV());


            try {
                FileOutputStream out = openFileOutput(students_grade + ".csv", MODE_PRIVATE);
                out.write((sb.toString()).getBytes());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //exporting
            File file = new File(getFilesDir(), students_grade + ".csv");
            String authority = getApplicationContext().getPackageName() + ".provider";
            Uri path = FileProvider.getUriForFile(getApplicationContext(), authority, file);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.putExtra(Intent.EXTRA_SUBJECT, students_grade);
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            startActivity(Intent.createChooser(fileIntent, "Send mail"));
        }

        private String getExcelHeaders() {
            final String name = getString(R.string.name);
            final String studentClass = getString(R.string.student_class);
            final String aerobicResult = getString(R.string.aerobic_result);
            final String aerobicScore = getString(R.string.aerobic_score);
            final String absResult = getString(R.string.abs_result);
            final String absScore = getString(R.string.abs_score);
            final String jumpResult = getString(R.string.jump_result);
            final String jumpScore = getString(R.string.jump_score);
            final String handsResult = getString(R.string.hands_result);
            final String handsScore = getString(R.string.hands_score);
            final String cubesResult = getString(R.string.cubes_result);
            final String cubesScore = getString(R.string.cubes_score);
            final String finalScore = getString(R.string.final_score);
            StringBuilder sb = new StringBuilder();
            return sb.append(name).append(",").append(studentClass).append(",").append(aerobicScore)
                    .append(",").append(aerobicResult).append(",").append(absScore)
                    .append(",").append(absResult).append(",").append(handsScore).append(",")
                    .append(handsResult).append(",").append(cubesScore).append(",")
                    .append(cubesResult).append(",").append(jumpScore).append(",")
                    .append(jumpResult).append(",").append(finalScore).toString();
        }
    }

}
