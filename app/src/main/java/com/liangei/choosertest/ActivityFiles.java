package com.liangei.choosertest;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.doplgangr.secrecy.Config;
import com.doplgangr.secrecy.CustomApp;
import com.doplgangr.secrecy.activities.FilePhotoActivity;
import com.doplgangr.secrecy.adapters.FilesListAdapter;
import com.doplgangr.secrecy.events.AddingFileDoneEvent;
import com.doplgangr.secrecy.events.AddingFileEvent;
import com.doplgangr.secrecy.events.DecryptingFileDoneEvent;
import com.doplgangr.secrecy.events.NewFileEvent;
import com.doplgangr.secrecy.filesystem.CryptStateListener;
import com.doplgangr.secrecy.filesystem.OurFileProvider;
import com.doplgangr.secrecy.filesystem.Storage;
import com.doplgangr.secrecy.filesystem.encryption.Vault;
import com.doplgangr.secrecy.filesystem.files.EncryptedFile;
import com.doplgangr.secrecy.jobs.AddFileJob;
import com.doplgangr.secrecy.jobs.InitializeVaultJob;
import com.doplgangr.secrecy.utils.Util;
import com.ipaulpro.afilechooser.FileChooserActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Created by lei on 2015/8/17.
 */
public class ActivityFiles extends Activity implements View.OnClickListener {

    private static final String TAG = "liangei.activityfiles";
    public static final String LOCAL_VAULT_NAME = "liangeiVault";

    private TextView addFile;
    private TextView decryptFile;
    private TextView deleteFile;


    private static final int REQUEST_CODE = 6384;
    private static final ArrayList<String> INCLUDE_EXTENSIONS_LIST = new ArrayList<>();
    private static final int NotificationID = 1820;
    private RecyclerView recyclerView;
    private ProgressBar addFilepBar;
    private TextView mTag;
    private String vault;
    private String password;
    private Vault secret;
    private FilesListAdapter mAdapter;
    private FilesListAdapter listAdapter;
    private FilesListAdapter galleryAdapter;
    private int decryptCounter = 0;
    private boolean isGallery = false;
    private boolean attached = false;
    //Notifications
    private NotificationManager mNotifyManager;
    private NotificationCompat.Builder mBuilder;

    private ProgressDialog mInitializeDialog;

    private RecyclerView.LayoutManager linearLayout;
    private RecyclerView.LayoutManager gridLayout;

    private ActionMode mActionMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beforeOnCreate();

        setContentView(R.layout.activity_files);
//        if (!vault.equals(LOCAL_VAULT_NAME)) {
//            Toast.makeText(this, "Vault Name" + vault + " incorrect", Toast.LENGTH_SHORT).show();
//            finish();
//        }

        Toast.makeText(this, "Vault Name" + vault, Toast.LENGTH_LONG).show();

        if (vault != null && !"".equals(vault)) {
            setTitle(vault);
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        Bundle extras = getIntent().getExtras();

        recyclerView = (RecyclerView) findViewById(R.id.file_list_recycler_view);
        addFile = (TextView) findViewById(R.id.addFile);
        decryptFile = (TextView) findViewById(R.id.decryptFile);
        deleteFile = (TextView)findViewById(R.id.deleteFile);
        addFile.setOnClickListener(this);
        decryptFile.setOnClickListener(this);
        deleteFile.setOnClickListener(this);
        mTag = (TextView) findViewById(R.id.tag);

        linearLayout = new LinearLayoutManager(this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayout);
        recyclerView.setAdapter(mAdapter);
    }

    void beforeOnCreate() {
        Bundle extras = getIntent().getExtras();

        vault = extras.getString(Config.vault_extra);
        password = extras.getString(Config.password_extra);

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        //��activity���´���ʱ������ȫ���fragment���Ա�fragment���Իָ�
//        setRetainInstance(true);

        gridLayout = new GridLayoutManager(this, 3);
        listAdapter = new FilesListAdapter(this, false);
        galleryAdapter = new FilesListAdapter(this, true);
        mAdapter = listAdapter;

        if (secret == null) {
            mInitializeDialog = new ProgressDialog(this);
            mInitializeDialog.setIndeterminate(true);
            mInitializeDialog.setMessage("Initializing Vault��");
            mInitializeDialog.setCancelable(false);
//            mInitializeDialog.show();

            CustomApp.jobManager.addJobInBackground(new InitializeVaultJob(vault, password));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.addFile:
                addFile();
                break;
            case R.id.decryptFile:
                decryptSelectedItems();
                break;
            case R.id.deleteFile:
                deleteSelectedItems();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(mAdapter);
        EventBus.getDefault().unregister(listAdapter);
        EventBus.getDefault().unregister(galleryAdapter);
        EventBus.getDefault().unregister(this);
    }

    void addToList(final EncryptedFile encryptedFile) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listAdapter.add(encryptedFile);
                galleryAdapter.add(encryptedFile);
                listAdapter.sort();
                galleryAdapter.sort();
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    public void onEventMainThread(NewFileEvent event) {
        // Add new file to the list, sort it to its alphabetical position, and highlight
        // it with smooth scrolling.
        Util.toast(this, "lalalala; mAdapter!= null-"+(mAdapter != null)+"; attached-"+attached,
                Toast.LENGTH_SHORT);
        if ((mAdapter != null) && (attached)) {
            Util.toast(this, "File successfully added",
                    Toast.LENGTH_SHORT);
            addToList(event.encryptedFile);
            mAdapter.sort();
            int index = mAdapter.getItemId(event.encryptedFile);
            if (index != -1)
                recyclerView.smoothScrollToPosition(index);
        }
    }

    public void onEventMainThread(Vault vault) {
        //The vault finishes initializing, is prepared to be populated.
        secret = vault;
        if (secret == null) {
            Log.d(TAG, "secret.isnull");
        }
        if (secret.isEcbVault()) {
            Log.d(TAG, "secret.isEcbVault()-" + secret.isEcbVault());
            Util.alert(
                    this,
                    getString(R.string.Error__old_vault_format),
                    getString(R.string.Error__old_vault_format_message),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Util.alert(
                                    ActivityFiles.this,
                                    getString(R.string.Upgrade__backup_beforehand),
                                    getString(R.string.Upgrade__backup_beforehand_message),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
//                                            backUp();
                                            finish();
                                        }
                                    },
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            ProgressDialog progress = new ProgressDialog(ActivityFiles.this);
                                            progress.setMessage(getString(R.string.Vault_updating));
                                            progress.setIndeterminate(true);
                                            progress.setCancelable(false);
//                                            progress.show();
//                                            updateVaultInBackground(progress);
                                        }
                                    }
                            );
                        }
                    },
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    }
            );
            return;
        }

        if (secret.wrongPass) {
            Log.d(TAG, "secret.wrongPass-" + secret.wrongPass);
            Util.alert(
                    this,
                    "Vault Failed",
                    "The Password is Incorrect, or the vault has been corrupted",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            finish();
                        }
                    },
                    null
            );
            return;
        }

        Log.d(TAG, "secret.nothing:" + secret.getPath() + ":");
        addFiles();
//        context.setTitle(secret.getName());
        mInitializeDialog.dismiss();
        setupViews();
    }

    public void onEventMainThread(AddingFileEvent event) {
        if (event.vaultToAdd != secret)
            return;
        if (mBuilder != null) {
            mBuilder.setContentText(event.fileToAdd);
            mNotifyManager.notify(NotificationID, mBuilder.build());
        }
    }

    public void onEventMainThread(AddingFileDoneEvent event) {
        if (event.vault != secret)
            return;
        if (mBuilder != null) {
            mBuilder.setProgress(0, 0, false)
                    .setContentText("finish adding files to secrecy")
                    .setOngoing(false);
            mNotifyManager.notify(NotificationID, mBuilder.build());
        }
    }

    /*public void onEventMainThread(DecryptingFileDoneEvent event) {
        mAdapter.notifyItemChanged(event.index);
        decryptCounter--;

        if (decryptCounter == 0 && attached) {
            Util.toast(context, getString(R.string.Files__save_to_SD), Toast.LENGTH_SHORT);
        }
    }*/

    public static class onPauseDecision {
        static Boolean pause = true;

        // An activity is started, should not pause and kill this fragment.
        public static void startActivity() {
            pause = false;
        }

        // Fragment returns to top, allow it to be paused and killed.
        public static void finishActivity() {
            pause = true;
        }

        public static Boolean shouldFinish() {
            return pause;
        }
    }

    /**
     * 想Vault中添加新的加密文件
     */
    void addFile() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = com.ipaulpro.afilechooser.utils.FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.Dialog_header__pick_file));
        try {
            startActivityForResult(intent, REQUEST_CODE);
            onPauseDecision.startActivity();
        } catch (ActivityNotFoundException e) {
            intent = new Intent(ActivityFiles.this, FileChooserActivity.class);
            intent.putStringArrayListExtra(
                    FileChooserActivity.EXTRA_FILTER_INCLUDE_EXTENSIONS,
                    INCLUDE_EXTENSIONS_LIST);
            intent.putExtra(FileChooserActivity.EXTRA_SELECT_FOLDER, false);
            startActivityForResult(intent, REQUEST_CODE);
            onPauseDecision.startActivity();
        }
    }

    //将Vault中的加密过的文件添加到list中用以显示
    void addFiles() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                secret.iterateAllFiles(
                        new Vault.onFileFoundListener() {
                            @Override
                            public void dothis(EncryptedFile encryptedFile) {
                                addToList(encryptedFile);
                            }
                        });
            }
        }).start();
    }


    /**
     * 选中的新添加的待加密文件的返回数据
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        onPauseDecision.finishActivity();
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data.getData() != null) {
            Util.log("intent received=", data.getData().toString(), data.getData().getLastPathSegment());

            /*mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(this);
            mBuilder.setContentTitle("adding files...")
                    .setSmallIcon(R.drawable.ic_stat_alert)
                    .setOngoing(true);
            mBuilder.setProgress(0, 0, true);
            mNotifyManager.notify(NotificationID, mBuilder.build());*/

            addFileInBackground(secret, data.getData());
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Util.toast(ActivityFiles.this, "no file is selected", 4000);
        }
    }

    void addFileInBackground(final Vault secret, final Uri data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                CustomApp.jobManager.addJobInBackground(new AddFileJob(ActivityFiles.this, secret, data));
            }
        }).start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        attached = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        attached = true;
    }

    void setupViews() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                FilesListAdapter.OnItemClickListener onItemClickListener = new FilesListAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(final View view, int position) {
                        if (mActionMode != null) {
                            select(position);
                            return;
                        }
                        if (isGallery) {
                            Intent intent = new Intent(ActivityFiles.this, FilePhotoActivity.class);
                            intent.putExtra(Config.gallery_item_extra, position);
                            onPauseDecision.startActivity();
                            startActivity(intent);
                        } else {
                            EncryptedFile encryptedFile = mAdapter.getItem(position);
                            if (!encryptedFile.getIsDecrypting()) {
//                                switchView(view, R.id.DecryptLayout);
                                Runnable onFinish = new Runnable() {
                                    @Override
                                    public void run() {
//                                        switchView(view, R.id.dataLayout);
                                    }
                                };
                                decrypt(encryptedFile, onFinish);
                            } else
                                Util.toast(ActivityFiles.this, "file is already recrypting", Toast.LENGTH_SHORT);
                        }
                    }
                };

                FilesListAdapter.OnItemLongClickListener onItemLongClickListener = new FilesListAdapter.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(View view, int position) {
                        if (mActionMode == null)
//                            mActionMode = context.startSupportActionMode(mActionModeCallback);
                        // Start the CAB using the ActionMode.Callback defined above
                        select(position);
                        //switchView(view, R.id.file_actions_layout);
                        //mListView.setOnClickListener(null);
                        return true;
                    }
                };

                listAdapter.setOnItemClickListener(onItemClickListener);
                listAdapter.setOnLongClickListener(onItemLongClickListener);

                galleryAdapter.setOnItemClickListener(onItemClickListener);
                galleryAdapter.setOnLongClickListener(onItemLongClickListener);
            }
        });
    }

    void select(int position) {
        mAdapter.select(position);
        mAdapter.notifyItemChanged(position);

        if (mActionMode == null)
            return;

        mActionMode.setTitle(
                String.format("%d file(s) selected",
                        mAdapter.getSelected().size()));
//        MenuItem renameButton = mActionMode.getMenu().findItem(R.id.action_rename);

//        if (mAdapter.getSelected().size() == 1)
//            renameButton.setVisible(true);
//        else
//            renameButton.setVisible(false);

        if (mAdapter.getSelected().size() == 0)
            mActionMode.finish();
    }

    void decrypt(final EncryptedFile encryptedFile, final Runnable onFinish) {
        new AsyncTask<EncryptedFile, Void, File>() {
            @Override
            protected File doInBackground(EncryptedFile... encryptedFiles) {
                return getFile(encryptedFile, onFinish);
            }

            @Override
            protected void onPostExecute(File tempFile){
                if (tempFile != null) {
                    if (tempFile.getParentFile().equals(Storage.getTempFolder())) {
                        tempFile = new File(Storage.getTempFolder(), tempFile.getName());
                    }
                    Uri uri = OurFileProvider.getUriForFile(ActivityFiles.this, OurFileProvider.FILE_PROVIDER_AUTHORITY, tempFile);
                    MimeTypeMap myMime = MimeTypeMap.getSingleton();
                    Intent newIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    String mimeType = myMime.getMimeTypeFromExtension(encryptedFile.getType());
                    newIntent.setDataAndType(uri, mimeType);
                    newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //altIntent: resort to using file provider when content provider does not work.
                    Intent altIntent = new Intent(android.content.Intent.ACTION_VIEW);
                    Uri rawuri = Uri.fromFile(tempFile);
                    altIntent.setDataAndType(rawuri, mimeType);
                    afterDecrypt(newIntent, altIntent);
                }
            }
        }.execute(encryptedFile);
    }

    File getFile(final EncryptedFile encryptedFile, final Runnable onfinish) {
        CryptStateListener listener = new CryptStateListener() {
            @Override
            public void updateProgress(int progress) {
//                updatePBar(encryptedFile, progress);
            }

            @Override
            public void setMax(int max) {
//                maxPBar(encryptedFile, max);
            }

            @Override
            public void onFailed(int statCode) {
                String message;
                switch (statCode) {
                    case Config.wrong_password:
                        message = "The password incorrect! please check.";
                        break;
                    case Config.file_not_found:
                        message = "The file not found! please check.";
                        break;
                    default:
                        message = "It's failed! that's all we know.";
                }
                alert(message);
            }

            @Override
            public void Finished() {

                onfinish.run();
            }
        };
        return encryptedFile.readFile(listener);
    }

    void alert(String message) {
        DialogInterface.OnClickListener click = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                finish();
            }
        };
        Util.alert(ActivityFiles.this,"Decrypt Failed.", message, click, null);
    }

    void afterDecrypt(Intent newIntent, Intent altIntent) {
        try {
            startActivity(newIntent);
            onPauseDecision.startActivity();
        } catch (android.content.ActivityNotFoundException e) {
            try {
                startActivity(altIntent);
                onPauseDecision.startActivity();
            } catch (android.content.ActivityNotFoundException e2) {
                Util.toast(ActivityFiles.this, "No App handles this type of file.", Toast.LENGTH_LONG);
                onPauseDecision.finishActivity();
            }
        } catch (IllegalStateException e) {
            //duh why you leave so early
            onPauseDecision.finishActivity();
        }
    }

    void decrypt_and_save(final int index, final Runnable onFinish) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                EncryptedFile encryptedFile = mAdapter.getItem(index);
                File tempFile = getFile(encryptedFile, onFinish);
                File storedFile = new File(Environment.getExternalStorageDirectory(), encryptedFile.getDecryptedFileName());
                if (tempFile == null) {
                    Util.alert(ActivityFiles.this,
                            "Error decrypting file.",
                            "Please try again later.",
                            Util.emptyClickListener,
                            null
                    );
                    return;
                }
                tempFile.renameTo(storedFile);
            }
        }).start();
    }

    /**
     * 对已加密的文件执行解密操作
     * 解密后生成的源文件默认将保存到/sdcard/目录xia
     * 同时在Vault中任然保留有加密文件
     *
     */
    void decryptSelectedItems() {
        Toast.makeText(this, "this is action_decrypt-" + attached, Toast.LENGTH_SHORT).show();
        for (final Integer index : mAdapter.getSelected()) {
            decryptCounter++;
            if (mAdapter.hasIndex(index)) {
                if (attached) {
                    mAdapter.getItem(index).setIsDecrypting(true);
                    mAdapter.notifyItemChanged(index);
                    decrypt_and_save(index, new Runnable() {
                        @Override
                        public void run() {
                            EventBus.getDefault().post(new DecryptingFileDoneEvent(index));
                        }
                    });
                }
            }
        }
    }


    /**
     * 在对应Vault中删除选中的加密文件，不影响Vault之外的数据
     */
    void deleteSelectedItems() {
        // Hold a local copy of selected values, because action mode is left before
        // Util.alter runs and thus adapter.getSelected is cleared.
        final HashSet<Integer> selected = new HashSet<Integer>(mAdapter.getSelected());
        final List<Integer> deleted = new ArrayList<>();

        String FilesToDelete = "\n";
        for (final Integer index : selected)
            if (mAdapter.hasIndex(index))
                FilesToDelete += "- " + mAdapter.getItem(index).getDecryptedFileName() + "\n";

        Util.alert(this,
                "Delete files",
                String.format("Are you sure to delete the following file(s)? %s", FilesToDelete),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for (final Integer index : selected) {
                            if (mAdapter.hasIndex(index))
                                if (!mAdapter.getItem(index).getIsDecrypting()) {
                                    secret.deleteFile(mAdapter.getItem(index));
                                    deleted.add(index);
                                } else if (attached)
                                    Util.toast(ActivityFiles.this, "Cannot delete decrypting files", Toast.LENGTH_SHORT);
                        }
                        // Don't modify adapter indices before finished.
                        //在操作结束前不要更改adapter目录
                        mAdapter.remove(deleted);
                    }
                },
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                }
        );
    }

}
