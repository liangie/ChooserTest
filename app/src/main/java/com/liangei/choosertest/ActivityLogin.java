package com.liangei.choosertest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.filesystem.encryption.VaultHolder;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

/**
 * Created by lei on 2015/8/17.
 */
public class ActivityLogin extends Activity implements View.OnClickListener,
        MainActivity.OnVaultSelectedListener{

    private EditText vaultName;
    private EditText vaultPass;
    private Button btnReset;
    private ImageButton ibConfirm;
    private Button btnConfirm;
    private TextView txtCreateTit;
    private TextView txtCreateVault;
    private LinearLayout lyConfirmPass;
    private EditText confirmPass;
    private boolean isCreateNewVault = false;
    private InputMethodManager imm = null;

//    private MainActivity.OnVaultSelectedListener mOnVaultSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
//        mOnVaultSelected = (MainActivity.OnVaultSelectedListener) this;
        init();

    }

    private void init() {
        vaultName = (EditText) findViewById(R.id.edtVaultName);
        vaultPass = (EditText) findViewById(R.id.edtVaultPass);
        btnReset = (Button) findViewById(R.id.btnReset);
        btnConfirm = (Button) findViewById(R.id.btnConfirm);
        txtCreateTit = (TextView) findViewById(R.id.txtCreateTit);
        txtCreateVault = (TextView) findViewById(R.id.txtCreateVault);
        confirmPass = (EditText)findViewById(R.id.edtVaultPassCon);
        lyConfirmPass = (LinearLayout)findViewById(R.id.lyConfirmPass);
        ibConfirm =(ImageButton)findViewById(R.id.confirm);
        btnReset.setOnClickListener(this);
        btnConfirm.setOnClickListener(this);
        txtCreateVault.setOnClickListener(this);
//        ibConfirm.setOnClickListener(this);
//        open();

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnReset:
                if (isCreateNewVault) {
                    isCreateNewVault = false;
                    txtCreateTit.setVisibility(View.GONE);
                    lyConfirmPass.setVisibility(View.GONE);
                    btnReset.setText("reset");
                } else {
                    vaultPass.setText("");
                    vaultName.setText("");
                }

                break;
            case R.id.btnConfirm:
                if (isCreateNewVault) {
                    //创建新的vault
                    add();
                } else {
                    //验证口令，进入vault
                    String vault = vaultName.getText().toString();
                    PasswordListener passwordListener = new PasswordListener(
                            imm, vault, this, vaultPass);
                    vaultPass.setOnEditorActionListener(passwordListener);
                    ibConfirm.setOnClickListener(passwordListener);
//                    onVaultSelected(vaultName.getText().toString(), vaultPass.getText().toString());
                }

                break;
            case R.id.txtCreateVault:
                isCreateNewVault = true;
                btnReset.setText("Cancel");
                txtCreateTit.setVisibility(View.VISIBLE);
                lyConfirmPass.setVisibility(View.VISIBLE);
                vaultPass.setText("");
                vaultName.setText("");
                break;
        }
    }

    void add() {

//        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        String name = vaultName.getText().toString();
        String password = vaultPass.getText().toString();
        String Confirmpassword = confirmPass.getText().toString();
        File directory = new File(Storage.getRoot().getAbsolutePath() + "/" + name);
        Log.d("directoryPath","directoryPath-"+directory.getPath());
        if (!password.equals(Confirmpassword) || "".equals(password))
            passwordWrong();
        else if (directory.mkdirs()) {
            // Create vault to initialize the vault header
            ProgressDialog progress = new ProgressDialog(ActivityLogin.this);
            progress.setIndeterminate(true);
            progress.setMessage("Initializing Vault...");
            progress.setCancelable(false);
            progress.show();
            createVaultInBackground(name, password, directory, progress);


            isCreateNewVault = false;
            txtCreateTit.setVisibility(View.GONE);
            lyConfirmPass.setVisibility(View.GONE);
            btnReset.setText("reset");
        } else
            failedtocreate();

    }

    void createVaultInBackground(final String name, final String password,
                                 final File directory,
                                 final ProgressDialog progressDialog) {
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
                progressDialog.dismiss();
            }
        }.execute();
    }

    void passwordWrong() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ActivityLogin.this)
                        .setTitle("Failed to create new Vault")
                        .setMessage("The password combination is incorrect, please check")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
            }
        });
    }

    void failedtocreate() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(ActivityLogin.this)
                        .setTitle("Failed to create new Vault")
                        .setMessage("Either the vault already exists, or SDCard is unavailab...")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }).show();
            }
        });
    }




    @Override
    public void onVaultSelected(String vault, String password) {
        Intent intent = new Intent(this, ActivityFiles.class);
        intent.putExtra(Config.vault_extra, vault);
        intent.putExtra(Config.password_extra, password);
        startActivity(intent);
    }

    /**
     * Does an action when the "done" key is pressed or a View is
     * clicked.
     */
    private abstract class InputListener
            implements TextView.OnEditorActionListener, View.OnClickListener {

        private final InputMethodManager imm;
//        private final View kbdView;

        InputListener(final InputMethodManager imm) {
            this.imm = imm;
//            this.kbdView = kbdView;
        }

        @Override
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                launchAction();
                hideKeyboard();
                return true;
            }
            return false;
        }

        @Override
        public void onClick(View v) {
            launchAction();
            hideKeyboard();
        }

        public void hideKeyboard() {
//            imm.hideSoftInputFromWindow(kbdView.getWindowToken(),
//                    InputMethodManager.HIDE_IMPLICIT_ONLY);
        }

        abstract void launchAction();
    }

    /**
     * Sends a password to try to unlock a vault.
     */
    private class PasswordListener extends InputListener {

        private final String vault;
        private final MainActivity.OnVaultSelectedListener vaultListener;
        private final EditText passwordField;

        private PasswordListener(
                final InputMethodManager imm,
//                final View kbdView,
                final String vault,
                final MainActivity.OnVaultSelectedListener vaultListener,
                final EditText passwordField) {

            super(imm);
            this.vault = vault;
            this.vaultListener = vaultListener;
            this.passwordField = passwordField;
        }

        @Override
        void launchAction() {
            sendPassword();
        }

        private void sendPassword() {
            String password = passwordField.getText().toString();
            if(isVaultExsists(vault)){
                vaultListener.onVaultSelected(vault, password);
            }else{
                Toast.makeText(ActivityLogin.this,"Vault Name not exist! please check.",Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isVaultExsists(String vault){
            File file = new File(Environment.getExternalStorageDirectory()
                    .getAbsoluteFile()
                    + "/SECRECYFILES");
            File[] files = file.listFiles();
            for(File file1: files){
                if (file1.isDirectory() && file1.getName().equals(vault)){
                    return true;
                }
            }
            return false;
        }
    }
}
