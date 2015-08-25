package com.liangei.choosertest;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;
import com.doplgangr.secrecy.jobs.InitializeVaultJob;
import com.ipaulpro.afilechooser.FileChooserActivity;

import java.io.File;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {


    private static final String VAULT_NAME = "choosertestVault";
    private static final String VAULT_PASSWORD = "liangie";
    private Button cButton;
    private TextView txtDetail;
    private Vault secret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtDetail = (TextView) findViewById(R.id.txtDetail);
        cButton = (Button) findViewById(R.id.CButton);
        cButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFile();
            }
        });

        if(secret == null){
            CustomApp.jobManager.addJobInBackground(new InitializeVaultJob(VAULT_NAME, VAULT_PASSWORD));
        }
    }

    private void init(){
        File directory = new File(Storage.getRoot().getAbsolutePath() + "/" + VAULT_NAME);
        if(directory.mkdirs()){
            createVaultInBackground(VAULT_NAME, VAULT_PASSWORD, directory);
        }else{
            Toast.makeText(MainActivity.this,"careate vault failed..",Toast.LENGTH_SHORT).show();
        }
    }

    void createVaultInBackground(final String name, final String password,
                                 final File directory) {
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... voids) {
                VaultHolder.getInstance().createAndRetrieveVault(name, password);
                try {
                    File file = new File(directory + "/.nomedia");
                    file.delete();
                    file.createNewFile();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void v) {
//                refresh();
//                dialog.dismiss();
//                progressDialog.dismiss();
            }
        }.execute();
    }

    public static final int REQUEST_CODE = 6384;

    /**
     * 添加新的文件到vault
     */
    void addFile() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, "pick a file...");
        try {
            startActivityForResult(intent, REQUEST_CODE);
//            FilesActivity.onPauseDecision.startActivity();
        } catch (ActivityNotFoundException e) {
            /*intent = new Intent(context, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
            FilesActivity.onPauseDecision.startActivity();*/
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data.getData() != null) {
            String newFile = data.getData().toString();
            if (newFile != null && !newFile.equals(""))
                txtDetail.setText(txtDetail.getText().toString().replace("No files selected..", "") + "\n" + newFile);
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Toast.makeText(MainActivity.this, "No file selected.....", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public interface OnVaultSelectedListener {
        public void onVaultSelected(String vault, String password);
    }
}
