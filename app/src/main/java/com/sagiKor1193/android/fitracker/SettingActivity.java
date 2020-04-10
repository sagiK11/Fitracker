package com.sagiKor1193.android.fitracker;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


public class SettingActivity extends AppCompatActivity {
    private final String TAG = "Setting";
    private final String[] answers = { "No", "Yes" };
    private final int NO = 0;
    private final int YES = 1;


    @Override
    protected void onCreate( @Nullable Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_setting );
    }

    public void deleteAllStudents( View view ) {
        final AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setTitle( "Are you sure you wish to delete all students?" );
        builder.setItems( answers, ( dialog, which ) -> {

            switch ( which ) {
                case NO: {
                    popToast( "No student was deleted." );
                    return;
                }
                case YES: {
                    Log.w( TAG, "user deleted all students" );
                    MainActivity.getDbHandler().clearDataBase();
                    popToast( "Students deleted successfully" );
                    break;
                }
            }
        } );
        builder.show();
    }


    private void popToast( String msg ) {
        Toast toast = Toast.makeText( getApplicationContext(), msg, Toast.LENGTH_SHORT );
        toast.show();
    }
}
