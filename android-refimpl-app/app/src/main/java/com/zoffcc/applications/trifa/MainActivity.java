/**
 * [TRIfA], Java part of Tox Reference Implementation for Android
 * Copyright (C) 2017 Zoff <zoff@zoff.cc>
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * version 2 as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA  02110-1301, USA.
 */

package com.zoffcc.applications.trifa;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LabeledIntent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.github.gfx.android.orma.AccessThreadConstraint;
import com.github.gfx.android.orma.encryption.EncryptedDatabase;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import info.guardianproject.iocipher.VirtualFileSystem;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

import static com.zoffcc.applications.trifa.AudioReceiver.channels_;
import static com.zoffcc.applications.trifa.AudioReceiver.sampling_rate_;
import static com.zoffcc.applications.trifa.CallingActivity.audio_thread;
import static com.zoffcc.applications.trifa.CallingActivity.close_calling_activity;
import static com.zoffcc.applications.trifa.MessageListActivity.ml_friend_typing;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TRIFA_FT_DIRECTION.TRIFA_FT_DIRECTION_INCOMING;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_FILE_DIR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.VFS_TMP_FILE_DIR;
import static com.zoffcc.applications.trifa.TRIFAGlobals.bootstrapping;
import static com.zoffcc.applications.trifa.TRIFAGlobals.cache_ft_fos;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_CANCEL;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_CONTROL.TOX_FILE_CONTROL_RESUME;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_AVATAR;
import static com.zoffcc.applications.trifa.ToxVars.TOX_FILE_KIND.TOX_FILE_KIND_DATA;
import static com.zoffcc.applications.trifa.TrifaToxService.is_tox_started;
import static com.zoffcc.applications.trifa.TrifaToxService.orma;
import static com.zoffcc.applications.trifa.TrifaToxService.vfs;

@RuntimePermissions
public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.MainActivity";
    final static boolean CTOXCORE_NATIVE_LOGGING = true;
    static TextView mt = null;
    static boolean native_lib_loaded = false;
    static String app_files_directory = "";
    // static boolean stop_me = false;
    // static Thread ToxServiceThread = null;
    Handler main_handler = null;
    static Handler main_handler_s = null;
    static Context context_s = null;
    static MainActivity main_activity_s = null;
    static AudioManager audio_manager_s = null;
    static int AudioMode_old;
    static int RingerMode_old;
    static boolean isSpeakerPhoneOn_old;
    static Notification notification = null;
    static NotificationManager nmn3 = null;
    static int NOTIFICATION_ID = 293821038;
    static RemoteViews notification_view = null;
    static long[] friends = null;
    static FriendListFragment friend_list_fragment = null;
    static MessageListFragment message_list_fragment = null;
    static MessageListActivity message_list_activity = null;
    final static String MAIN_DB_NAME = "main.db";
    final static String MAIN_VFS_NAME = "files.db";
    final static boolean ORMA_TRACE = true;
    final static int AddFriendActivity_ID = 10001;
    final static int CallingActivity_ID = 10002;
    final static int ProfileActivity_ID = 10003;
    final static int SettingsActivity_ID = 10004;
    final static int AboutpageActivity_ID = 10005;
    final static int Notification_new_message_ID = 10023;
    static long Notification_new_message_last_shown_timestamp = -1;
    final static long Notification_new_message_every_millis = 2000; // ~2 seconds between notifications
    static String temp_string_a = "";
    static ByteBuffer video_buffer_1 = null;
    static ByteBuffer video_buffer_2 = null;
    final static int audio_in_buffer_max_count = 8;
    static int audio_in_buffer_element_count = 0;
    static ByteBuffer[] audio_buffer_2 = new ByteBuffer[audio_in_buffer_max_count];
    static ByteBuffer audio_buffer_play = null;
    static int audio_buffer_play_length = 0;
    static int[] audio_buffer_2_read_length = new int[audio_in_buffer_max_count];
    static TrifaToxService tox_service_fg = null;
    //
    static boolean PREF__UV_reversed = true; // TODO: on older phone this needs to be "false"
    static boolean PREF__notification_sound = true;
    static boolean PREF__notification_vibrate = false;
    static boolean PREF__notification = true;
    static int PREF__min_audio_samplingrate_out = 8000;
    static String PREF__DB_secrect_key = "98rj93ßjw3j8j4vj9w8p9eüiü9aci092";
    private static final String ALLOWED_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz!§$%&()=?,.;:-_+";
    //
    // YUV conversion -------
    static ScriptIntrinsicYuvToRGB yuvToRgb = null;
    static Allocation alloc_in = null;
    static Allocation alloc_out = null;
    static Bitmap video_frame_image = null;
    static int buffer_size_in_bytes = 0;
    // YUV conversion -------

    // ---- lookup cache ----
    static Map<String, Long> cache_pubkey_fnum = new HashMap<String, Long>();
    static Map<Long, String> cache_fnum_pubkey = new HashMap<Long, String>();
    // ---- lookup cache ----

    // main drawer ----------
    Drawer main_drawer = null;
    AccountHeader main_drawer_header = null;
    // main drawer ----------

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bootstrapping = false;

        audio_manager_s = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // prefs ----------
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        Log.i(TAG, "PREF__notification_sound:2=" + PREF__notification_sound);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", false);
        Log.i(TAG, "PREF__notification_vibrate:2=" + PREF__notification_vibrate);
        PREF__notification = settings.getBoolean("notifications_new_message", true);
        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(settings.getString("min_audio_samplingrate_out", "8000"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = 8000;
        }
        // prefs ----------

        PREF__DB_secrect_key = settings.getString("DB_secrect_key", "");
        if (PREF__DB_secrect_key.isEmpty())
        {
            // TODO: bad, make better
            // create new key -------------
            PREF__DB_secrect_key = getRandomString(20);
            settings.edit().putString("DB_secrect_key", PREF__DB_secrect_key).commit();
            // create new key -------------
        }

        // TODO: don't print this!!
        // ------ don't print this ------
        // ------ don't print this ------
        // ------ don't print this ------
        // ** // Log.i(TAG, "PREF__DB_secrect_key=" + PREF__DB_secrect_key);
        // ------ don't print this ------
        // ------ don't print this ------
        // ------ don't print this ------

        mt = (TextView) this.findViewById(R.id.main_maintext);
        mt.setText("...");

        main_handler = new Handler(getMainLooper());
        main_handler_s = main_handler;
        context_s = this.getBaseContext();
        main_activity_s = this;

        nmn3 = (NotificationManager) context_s.getSystemService(NOTIFICATION_SERVICE);

        // get permission ----------
        MainActivityPermissionsDispatcher.dummyForPermissions001WithCheck(this);
        // get permission ----------

        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName("Profile").withIcon(GoogleMaterial.Icon.gmd_face);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem().withIdentifier(2).withName("Settings").withIcon(GoogleMaterial.Icon.gmd_settings);
        PrimaryDrawerItem item3 = new PrimaryDrawerItem().withIdentifier(3).withName("Logout/Login").withIcon(GoogleMaterial.Icon.gmd_refresh);
        PrimaryDrawerItem item4 = new PrimaryDrawerItem().withIdentifier(4).withName("About").withIcon(GoogleMaterial.Icon.gmd_info);
        PrimaryDrawerItem item5 = new PrimaryDrawerItem().withIdentifier(5).withName("Exit").withIcon(GoogleMaterial.Icon.gmd_exit_to_app);

        Drawable d1 = new IconicsDrawable(this).icon(FontAwesome.Icon.faw_lock).color(getResources().getColor(R.color.colorPrimaryDark)).sizeDp(24);

        // Create the AccountHeader
        main_drawer_header = new AccountHeaderBuilder().withActivity(this).addProfiles(new ProfileDrawerItem().withName("me").withIcon(d1)).withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener()
        {
            @Override
            public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile)
            {
                return false;
            }
        }).build();


        // create the drawer and remember the `Drawer` result object
        main_drawer = new DrawerBuilder().
                withActivity(this).
                addDrawerItems(item1, new DividerDrawerItem(), item2, item3, item4, new DividerDrawerItem(), item5).
                withTranslucentStatusBar(true).withAccountHeader(main_drawer_header).
                withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener()
                {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem)
                    {
                        Log.i(TAG, "drawer:item=" + position);
                        if (position == 1)
                        {
                            // profile
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start profile activity");
                                    Intent intent = new Intent(context_s, ProfileActivity.class);
                                    startActivityForResult(intent, ProfileActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 3)
                        {
                            // settings
                            try
                            {
                                if (Callstate.state == 0)
                                {
                                    Log.i(TAG, "start settings activity");
                                    Intent intent = new Intent(context_s, SettingsActivity.class);
                                    startActivityForResult(intent, SettingsActivity_ID);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 4)
                        {
                            // logout/login
                            try
                            {
                                if (is_tox_started)
                                {
                                    tox_service_fg.stop_tox_fg();
                                }
                                else
                                {
                                    init(app_files_directory);
                                    tox_service_fg.tox_thread_start_fg();
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        else if (position == 5)
                        {
                            // About
                            try
                            {
                                Log.i(TAG, "start aboutpage activity");
                                Intent intent = new Intent(context_s, Aboutpage.class);
                                startActivityForResult(intent, AboutpageActivity_ID);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }

                        else if (position == 7)
                        {
                            // Exit
                            try
                            {
                                if (is_tox_started)
                                {
                                    tox_service_fg.stop_tox_fg();
                                    tox_service_fg.stop_me(true);
                                }
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();
                            }
                        }
                        return true;
                    }
                }).build();

        // -------- drawer ------------
        // -------- drawer ------------
        // -------- drawer ------------

        // reset calling state
        Callstate.state = 0;
        Callstate.tox_call_state = ToxVars.TOXAV_FRIEND_CALL_STATE.TOXAV_FRIEND_CALL_STATE_NONE.value;
        Callstate.call_first_video_frame_received = -1;
        Callstate.call_first_audio_frame_received = -1;
        Callstate.friend_pubkey = "-1";
        Callstate.audio_speaker = true;
        Callstate.other_audio_enabled = 1;
        Callstate.other_video_enabled = 1;
        Callstate.my_audio_enabled = 1;
        Callstate.my_video_enabled = 1;

        if (native_lib_loaded)
        {
            mt.setText("successfully loaded native library");
        }
        else
        {
            mt.setText("loadLibrary jni-c-toxcore failed!");
        }

        String native_api = getNativeLibAPI();
        mt.setText(mt.getText() + "\n" + native_api);
        mt.setText(mt.getText() + "\n" + "c-toxcore:v" + tox_version_major() + "." + tox_version_minor() + "." + tox_version_patch());
        mt.setText(mt.getText() + "\n" + "jni-c-toxcore:v" + jnictoxcore_version());

        // --- forground service ---
        // --- forground service ---
        // --- forground service ---
        Intent i = new Intent(this, TrifaToxService.class);
        startService(i);
        // --- forground service ---
        // --- forground service ---
        // --- forground service ---

        try
        {
            String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;
            Log.i(TAG, "db:path=" + dbs_path);

            File database_dir = new File(new File(dbs_path).getParent());
            database_dir.mkdirs();

            OrmaDatabase.Builder builder = OrmaDatabase.builder(this);
            builder = builder.provider(new EncryptedDatabase.Provider(PREF__DB_secrect_key));
            orma = builder.name(dbs_path).
                    readOnMainThread(AccessThreadConstraint.NONE).
                    writeOnMainThread(AccessThreadConstraint.NONE).
                    trace(ORMA_TRACE).
                    build();
            Log.i(TAG, "db:open=OK:path=" + dbs_path);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "db:EE1:" + e.getMessage());

            String dbs_path = getDir("dbs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_DB_NAME;

            try
            {
                Log.i(TAG, "db:deleting database:" + dbs_path);
                new File(dbs_path).delete();
            }
            catch (Exception e3)
            {
                e3.printStackTrace();
                Log.i(TAG, "db:EE3:" + e3.getMessage());
            }

            Log.i(TAG, "db:path(2)=" + dbs_path);
            OrmaDatabase.Builder builder = OrmaDatabase.builder(this);
            builder = builder.provider(new EncryptedDatabase.Provider(PREF__DB_secrect_key));
            orma = builder.name(dbs_path).
                    readOnMainThread(AccessThreadConstraint.WARNING).
                    writeOnMainThread(AccessThreadConstraint.WARNING).
                    trace(ORMA_TRACE).
                    build();
            Log.i(TAG, "db:open(2)=OK:path=" + dbs_path);
        }

        Log.i(TAG, "db:migrate");
        orma.migrate();
        Log.i(TAG, "db:migrate=OK");

        try
        {
            String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;

            File database_dir = new File(new File(dbFile).getParent());
            database_dir.mkdirs();

            Log.i(TAG, "vfs:path=" + dbFile);
            vfs = VirtualFileSystem.get();
            vfs.createNewContainer(dbFile, PREF__DB_secrect_key);
            vfs.mount(PREF__DB_secrect_key);
            Log.i(TAG, "vfs:open(1)=OK:path=" + dbFile);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "vfs:EE:" + e.getMessage());

            String dbFile = getDir("vfs", MODE_PRIVATE).getAbsolutePath() + "/" + MAIN_VFS_NAME;

            try
            {
                Log.i(TAG, "vfs:deleting database:" + dbFile);
                new File(dbFile).delete();
            }
            catch (Exception e3)
            {
                e3.printStackTrace();
                Log.i(TAG, "vfs:EE3:" + e3.getMessage());
            }

            try
            {
                Log.i(TAG, "vfs:path=" + dbFile);
                vfs = VirtualFileSystem.get();
                vfs.createNewContainer(dbFile, PREF__DB_secrect_key);
                vfs.mount(PREF__DB_secrect_key);
                Log.i(TAG, "vfs:open(2)=OK:path=" + dbFile);
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                Log.i(TAG, "vfs:EE2:" + e.getMessage());
            }
        }


        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        if (vfs.isMounted())
        {
            vfs_listFilesAndFilesSubDirectories("/", 0, "");

            //            try
            //            {
            //                BufferedWriter out = new BufferedWriter(new info.guardianproject.iocipher.FileWriter("test.txt"));
            //                out.write("aString\nthis is a\nttest");
            //                out.close();
            //                Log.i(TAG, "vfs:write:OK");
            //            }
            //            catch (Exception e)
            //            {
            //                e.printStackTrace();
            //                Log.i(TAG, "vfs:EE:write:EE1:" + e.getMessage());
            //            }

            //            try
            //            {
            //                BufferedReader reader = new BufferedReader(new info.guardianproject.iocipher.FileReader("test.txt"));
            //                String txt = reader.readLine();
            //                Log.i(TAG, "vfs:read:res=" + txt);
            //            }
            //            catch (Exception e)
            //            {
            //                e.printStackTrace();
            //                Log.i(TAG, "vfs:EE:read:EE1:" + e.getMessage());
            //            }

        }
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------
        // ---------- DEBUG, just a test ----------

        app_files_directory = getFilesDir().getAbsolutePath();
        tox_thread_start();
    }


    public void vfs_listFilesAndFilesSubDirectories(String directoryName, int depth, String parent)
    {
        info.guardianproject.iocipher.File directory = new info.guardianproject.iocipher.File(directoryName);
        info.guardianproject.iocipher.File[] fList = directory.listFiles();

        for (info.guardianproject.iocipher.File file : fList)
        {
            if (file.isFile())
            {
                // final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                // final String human_datetime = df.format(new Date(file.lastModified()));
                Log.i(TAG, "VFS:f:" + parent + "/" + file.getName() + " bytes=" + file.length());
            }
            else if (file.isDirectory())
            {
                Log.i(TAG, "VFS:d:" + parent + "/" + file.getName() + "/");
                vfs_listFilesAndFilesSubDirectories(file.getAbsolutePath(), depth + 1, parent + "/" + file.getName());
            }
        }
    }


    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    @NeedsPermission({Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA})
    void dummyForPermissions001()
    {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------
    // ------- for runtime permissions -------


    private static String getRandomString(final int sizeOfRandomString)
    {
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(sizeOfRandomString);
        for (int i = 0; i < sizeOfRandomString; ++i)
        {
            sb.append(ALLOWED_CHARACTERS.charAt(random.nextInt(ALLOWED_CHARACTERS.length())));
        }
        return sb.toString();
    }

    void tox_thread_start()
    {
        try
        {
            Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    long counter = 0;
                    while (tox_service_fg == null)
                    {
                        counter++;
                        if (counter > 100)
                        {
                            break;
                        }

                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }

                    try
                    {
                        if (!is_tox_started)
                        {
                            init(app_files_directory);
                        }

                        tox_service_fg.tox_thread_start_fg();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "tox_thread_start:EE:" + e.getMessage());
        }
    }

    //    static void stop_tox()
    //    {
    //        try
    //        {
    //            Thread t = new Thread()
    //            {
    //                @Override
    //                public void run()
    //                {
    //                    long counter = 0;
    //                    while (tox_service_fg == null)
    //                    {
    //                        counter++;
    //                        if (counter > 100)
    //                        {
    //                            break;
    //                        }
    //
    //                        try
    //                        {
    //                            Thread.sleep(100);
    //                        }
    //                        catch (Exception e)
    //                        {
    //                            e.printStackTrace();
    //                        }
    //                    }
    //
    //                    try
    //                    {
    //
    //                        tox_service_fg.stop_tox_fg();
    //                    }
    //                    catch (Exception e)
    //                    {
    //                        e.printStackTrace();
    //                    }
    //                }
    //            };
    //            t.start();
    //        }
    //        catch (Exception e)
    //        {
    //            e.printStackTrace();
    //            Log.i(TAG, "stop_tox:EE:" + e.getMessage());
    //        }
    //    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // prefs ----------
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        PREF__UV_reversed = settings.getBoolean("video_uv_reversed", true);
        PREF__notification_sound = settings.getBoolean("notifications_new_message_sound", true);
        PREF__notification_vibrate = settings.getBoolean("notifications_new_message_vibrate", true);
        try
        {
            if (settings.getString("min_audio_samplingrate_out", "8000").compareTo("Auto") == 0)
            {
                PREF__min_audio_samplingrate_out = 8000;
            }
            else
            {
                PREF__min_audio_samplingrate_out = Integer.parseInt(settings.getString("min_audio_samplingrate_out", "8000"));
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            PREF__min_audio_samplingrate_out = 8000;
        }
        Log.i(TAG, "PREF__UV_reversed:2=" + PREF__UV_reversed);
        Log.i(TAG, "PREF__min_audio_samplingrate_out:2=" + PREF__min_audio_samplingrate_out);
        // prefs ----------
    }

    @Override
    protected void onNewIntent(Intent i)
    {
        Log.i(TAG, "onNewIntent:i=" + i);
        super.onNewIntent(i);
    }

    static FriendList main_get_friend(long friendnum)
    {
        FriendList f;
        List<FriendList> fl = orma.selectFromFriendList().
                tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                toList();
        if (fl.size() > 0)
        {
            f = fl.get(0);
        }
        else
        {
            f = null;
        }

        return f;
    }

    static int is_friend_online(long friendnum)
    {
        try
        {
            return (orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0).TOX_CONNECTION);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return 0;
        }

    }

    synchronized static void set_all_friends_offline()
    {

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                orma.updateFriendList().
                        TOX_CONNECTION(0).
                        execute();
                friend_list_fragment.set_all_friends_to_offline();
            }
        };
        t.start();
    }

    synchronized static void update_friend_in_db(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_string(f.tox_public_key_string).
                name(f.name).
                status_message(f.status_message).
                TOX_CONNECTION(f.TOX_CONNECTION).
                TOX_USER_STATUS(f.TOX_USER_STATUS).
                execute();
    }

    synchronized static void update_friend_in_db_status_message(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                status_message(f.status_message).
                execute();
    }

    synchronized static void update_friend_in_db_status(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                TOX_USER_STATUS(f.TOX_USER_STATUS).
                execute();
    }

    synchronized static void update_friend_in_db_connection_status(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                TOX_CONNECTION(f.TOX_CONNECTION).
                execute();
    }

    synchronized static void update_friend_in_db_name(FriendList f)
    {
        orma.updateFriendList().
                tox_public_key_stringEq(f.tox_public_key_string).
                name(f.name).
                execute();
    }

    synchronized static void update_message_in_db(final Message m)
    {
        final Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    orma.updateMessage().
                            idEq(m.id).
                            read(m.read).
                            text(m.text).
                            sent_timestamp(m.sent_timestamp).
                            rcvd_timestamp(m.rcvd_timestamp).
                            filename_fullpath(m.filename_fullpath).
                            execute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    static void update_message_in_db_read_rcvd_timestamp(final Message m)
    {
        final Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    orma.updateMessage().
                            idEq(m.id).
                            read(m.read).
                            rcvd_timestamp(m.rcvd_timestamp).
                            execute();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    static void change_notification(int a_TOXCONNECTION)
    {
        // crash -----------------
        // crash -----------------
        // crash -----------------
        // crash -----------------
        // crash -----------------
        // crash_app_java(1);
        // crash_app_C();
        // crash -----------------
        // crash -----------------
        // crash -----------------
        // crash -----------------
        // crash -----------------

        Log.i(TAG, "change_notification");
        final int a_TOXCONNECTION_f = a_TOXCONNECTION;
        try
        {
            Thread t = new Thread()
            {
                @Override
                public void run()
                {
                    long counter = 0;
                    while (tox_service_fg == null)
                    {
                        counter++;
                        if (counter > 10)
                        {
                            break;
                        }
                        // Log.i(TAG, "change_notification:sleep");

                        try
                        {
                            Thread.sleep(100);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    Log.i(TAG, "change_notification:change");
                    try
                    {
                        tox_service_fg.change_notification_fg(a_TOXCONNECTION_f);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }


    // -- this is for incoming video --
    // -- this is for incoming video --
    static void allocate_video_buffer_1(int frame_width_px1, int frame_height_px1, long ystride, long ustride, long vstride)
    {
        if (video_buffer_1 != null)
        {
            // video_buffer_1.clear();
            video_buffer_1 = null;
        }

        if (video_frame_image != null)
        {
            video_frame_image.recycle();
            video_frame_image = null;
        }

        /*
        * YUV420 frame with width * height
        *
        * @param y Luminosity plane. Size = MAX(width, abs(ystride)) * height.
        * @param u U chroma plane. Size = MAX(width/2, abs(ustride)) * (height/2).
        * @param v V chroma plane. Size = MAX(width/2, abs(vstride)) * (height/2).
        */
        int y_layer_size = (int) Math.max(frame_width_px1, Math.abs(ystride)) * frame_height_px1;
        int u_layer_size = (int) Math.max((frame_width_px1 / 2), Math.abs(ustride)) * (frame_height_px1 / 2);
        int v_layer_size = (int) Math.max((frame_width_px1 / 2), Math.abs(vstride)) * (frame_height_px1 / 2);

        int frame_width_px = (int) Math.max(frame_width_px1, Math.abs(ystride));
        int frame_height_px = (int) frame_height_px1;

        buffer_size_in_bytes = y_layer_size + v_layer_size + u_layer_size;

        Log.i(TAG, "YUV420 frame w1=" + frame_width_px1 + " h1=" + frame_height_px1 + " bytes=" + buffer_size_in_bytes);
        Log.i(TAG, "YUV420 frame w=" + frame_width_px + " h=" + frame_height_px + " bytes=" + buffer_size_in_bytes);
        Log.i(TAG, "YUV420 frame ystride=" + ystride + " ustride=" + ustride + " vstride=" + vstride);
        video_buffer_1 = ByteBuffer.allocateDirect(buffer_size_in_bytes);
        set_JNI_video_buffer(video_buffer_1, frame_width_px, frame_height_px);

        RenderScript rs = RenderScript.create(context_s);
        yuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));

        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs)).setX(frame_width_px).setY(frame_height_px);
        yuvType.setYuvFormat(ImageFormat.YV12);
        alloc_in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
        Type.Builder rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(frame_width_px).setY(frame_height_px);
        alloc_out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------
        // --------- works !!!!! ---------

        video_frame_image = Bitmap.createBitmap(frame_width_px, frame_height_px, Bitmap.Config.ARGB_8888);
    }
    // -- this is for incoming video --
    // -- this is for incoming video --


    static
    {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    // -------- native methods --------
    // -------- native methods --------
    // -------- native methods --------
    public native void init(@NonNull String data_dir);

    public native String getNativeLibAPI();

    public static native void update_savedata_file();

    public static native String get_my_toxid();

    public static native void bootstrap();

    public static native int add_tcp_relay_single(String ip, String key_hex, long port);

    public static native int bootstrap_single(String ip, String key_hex, long port);

    public static native void init_tox_callbacks();

    public static native long tox_iteration_interval();

    public static native long tox_iterate();

    public static native long tox_kill();

    public static native void exit();

    public static native long tox_friend_send_message(long friendnum, int a_TOX_MESSAGE_TYPE, @NonNull String message);

    public static native long tox_version_major();

    public static native long tox_version_minor();

    public static native long tox_version_patch();

    public static native String jnictoxcore_version();

    public static native long tox_max_filename_length();

    public static native long tox_file_id_length();

    public static native long tox_max_message_length();

    public static native long tox_friend_add(@NonNull String toxid_str, @NonNull String message);

    public static native long tox_friend_add_norequest(@NonNull String public_key_str);

    public static native long tox_self_get_friend_list_size();

    public static native long tox_friend_by_public_key(@NonNull String friend_public_key_string);

    public static native String tox_friend_get_public_key(long friend_number);

    public static native long[] tox_self_get_friend_list();

    public static native int tox_self_set_name(@NonNull String name);

    public static native int tox_self_set_status_message(@NonNull String status_message);

    public static native void tox_self_set_status(int a_TOX_USER_STATUS);

    public static native int tox_self_set_typing(long friend_number, int typing);

    public static native int tox_friend_get_connection_status(long friend_number);

    public static native int tox_friend_delete(long friend_number);

    public static native String tox_self_get_name();

    public static native long tox_self_get_name_size();

    public static native long tox_self_get_status_message_size();

    public static native String tox_self_get_status_message();

    public static native int tox_file_control(long friend_number, long file_number, int a_TOX_FILE_CONTROL);

    // --------------- AV -------------
    // --------------- AV -------------
    // --------------- AV -------------
    public static native int toxav_answer(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native long toxav_iteration_interval();

    public static native int toxav_call(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native int toxav_bit_rate_set(long friendnum, long audio_bit_rate, long video_bit_rate);

    public static native int toxav_call_control(long friendnum, int a_TOXAV_CALL_CONTROL);

    public static native int toxav_video_send_frame_uv_reversed(long friendnum, int frame_width_px, int frame_height_px);

    public static native int toxav_video_send_frame(long friendnum, int frame_width_px, int frame_height_px);

    public static native long set_JNI_video_buffer(ByteBuffer buffer, int frame_width_px, int frame_height_px);

    public static native void set_JNI_video_buffer2(ByteBuffer buffer2, int frame_width_px, int frame_height_px);

    public static native void set_JNI_audio_buffer(ByteBuffer audio_buffer);

    // buffer2 is for incoming audio
    public static native void set_JNI_audio_buffer2(ByteBuffer audio_buffer2);

    /**
     * Send an audio frame to a friend.
     * <p>
     * The expected format of the PCM data is: [s1c1][s1c2][...][s2c1][s2c2][...]...
     * Meaning: sample 1 for channel 1, sample 1 for channel 2, ...
     * For mono audio, this has no meaning, every sample is subsequent. For stereo,
     * this means the expected format is LRLRLR... with samples for left and right
     * alternating.
     *
     * @param friend_number The friend number of the friend to which to send an
     *                      audio frame.
     * @param sample_count  Number of samples in this frame. Valid numbers here are
     *                      ((sample rate) * (audio length) / 1000), where audio length can be
     *                      2.5, 5, 10, 20, 40 or 60 millseconds.
     * @param channels      Number of audio channels. Supported values are 1 and 2.
     * @param sampling_rate Audio sampling rate used in this frame. Valid sampling
     *                      rates are 8000, 12000, 16000, 24000, or 48000.
     */
    public static native int toxav_audio_send_frame(long friend_number, long sample_count, int channels, long sampling_rate);
    // --------------- AV -------------
    // --------------- AV -------------
    // --------------- AV -------------

    // -------- native methods --------
    // -------- native methods --------
    // -------- native methods --------

    // -------- called by AV native methods --------
    // -------- called by AV native methods --------
    // -------- called by AV native methods --------

    static void android_toxav_callback_call_cb_method(long friend_number, int audio_enabled, int video_enabled)
    {
        if (Callstate.state != 0)
        {
            // don't accept a new call if we already are in a call
            return;
        }


        Log.i(TAG, "toxav_call:from=" + friend_number + " audio=" + audio_enabled + " video=" + video_enabled);
        final long fn = friend_number;
        final int f_audio_enabled = audio_enabled;
        final int f_video_enabled = video_enabled;

        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (Callstate.state == 0)
                    {
                        Log.i(TAG, "CALL:start:show activity");
                        Callstate.state = 1;
                        Callstate.accepted_call = 0;
                        Callstate.call_first_video_frame_received = -1;
                        Callstate.call_first_audio_frame_received = -1;
                        Callstate.call_start_timestamp = -1;
                        Callstate.audio_speaker = true;
                        Callstate.other_audio_enabled = 1;
                        Callstate.other_video_enabled = 1;
                        Callstate.my_audio_enabled = 1;
                        Callstate.my_video_enabled = 1;
                        Intent intent = new Intent(context_s, CallingActivity.class);
                        Callstate.friend_pubkey = tox_friend_get_public_key__wrapper(fn);
                        try
                        {
                            Callstate.friend_name = orma.selectFromFriendList().
                                    tox_public_key_stringEq(Callstate.friend_pubkey).
                                    toList().get(0).name;
                        }
                        catch (Exception e)
                        {
                            Callstate.friend_name = "Unknown";
                            e.printStackTrace();
                        }
                        Callstate.other_audio_enabled = f_audio_enabled;
                        Callstate.other_video_enabled = f_video_enabled;
                        Callstate.call_init_timestamp = System.currentTimeMillis();
                        main_activity_s.startActivityForResult(intent, CallingActivity_ID);
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    Log.i(TAG, "CALL:start:EE:" + e.getMessage());
                }
            }
        };
        main_handler_s.post(myRunnable);
    }

    synchronized static void android_toxav_callback_video_receive_frame_cb_method(long friend_number, long frame_width_px, long frame_height_px, long ystride, long ustride, long vstride)
    {
        if (Callstate.other_video_enabled == 0)
        {
            return;
        }

        if (tox_friend_by_public_key__wrapper(Callstate.friend_pubkey) != friend_number)
        {
            // not the friend we are in call with now
            return;
        }

        // Log.i(TAG, "toxav_video_receive_frame:from=" + friend_number + " video width=" + frame_width_px + " video height=" + frame_height_px);
        if (Callstate.call_first_video_frame_received == -1)
        {
            Callstate.call_first_video_frame_received = System.currentTimeMillis();

            // allocate new video buffer on 1 frame
            allocate_video_buffer_1((int) frame_width_px, (int) frame_height_px, ystride, ustride, vstride);

            temp_string_a = "" + (int) ((Callstate.call_first_video_frame_received - Callstate.call_start_timestamp) / 1000) + "s";
            CallingActivity.update_top_text_line(temp_string_a, 3);
        }

        try
        {
            alloc_in.copyFrom(video_buffer_1.array());
            yuvToRgb.setInput(alloc_in);
            yuvToRgb.forEach(alloc_out);
            alloc_out.copyTo(video_frame_image);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    CallingActivity.mContentView.setImageBitmap(video_frame_image);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        main_handler_s.post(myRunnable);
    }

    static void android_toxav_callback_call_state_cb_method(long friend_number, int a_TOXAV_FRIEND_CALL_STATE)
    {
        if (tox_friend_by_public_key__wrapper(Callstate.friend_pubkey) != friend_number)
        {
            // not the friend we are in call with now
            return;
        }

        Log.i(TAG, "toxav_call_state:from=" + friend_number + " state=" + a_TOXAV_FRIEND_CALL_STATE);
        Log.i(TAG, "Callstate.tox_call_state=" + a_TOXAV_FRIEND_CALL_STATE + " old=" + Callstate.tox_call_state);

        if (Callstate.state == 1)
        {
            int old_value = Callstate.tox_call_state;
            Callstate.tox_call_state = a_TOXAV_FRIEND_CALL_STATE;

            if ((a_TOXAV_FRIEND_CALL_STATE & (4 + 8 + 16 + 32)) > 0)
            {
                Log.i(TAG, "toxav_call_state:from=" + friend_number + " call starting");
                Callstate.call_start_timestamp = System.currentTimeMillis();

                Runnable myRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            CallingActivity.accept_button.setVisibility(View.GONE);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                CallingActivity.callactivity_handler_s.post(myRunnable);
            }
            else if ((a_TOXAV_FRIEND_CALL_STATE & (2)) > 0)
            {
                Log.i(TAG, "toxav_call_state:from=" + friend_number + " call ending(1)");
                close_calling_activity();
            }
            else if ((old_value > 0) && (a_TOXAV_FRIEND_CALL_STATE == 0))
            {
                Log.i(TAG, "toxav_call_state:from=" + friend_number + " call ending(2)");
                close_calling_activity();
            }

        }
    }

    static void android_toxav_callback_bit_rate_status_cb_method(long friend_number, long audio_bit_rate, long video_bit_rate)
    {
        if (tox_friend_by_public_key__wrapper(Callstate.friend_pubkey) != friend_number)
        {
            // not the friend we are in call with now
            return;
        }

        Log.i(TAG, "toxav_bit_rate_status:from=" + friend_number + " audio_bit_rate=" + audio_bit_rate + " video_bit_rate=" + video_bit_rate);

        if (Callstate.state == 1)
        {
            Callstate.audio_bitrate = audio_bit_rate;
            Callstate.video_bitrate = video_bit_rate;
        }
    }

    static void android_toxav_callback_audio_receive_frame_cb_method(long friend_number, long sample_count, int channels, long sampling_rate)
    {
        if (tox_friend_by_public_key__wrapper(Callstate.friend_pubkey) != friend_number)
        {
            // not the friend we are in call with now
            return;
        }

        if (Callstate.other_audio_enabled == 0)
        {
            if (Callstate.call_first_audio_frame_received == -1)
            {
                sampling_rate_ = sampling_rate;
                Log.i(TAG, "audio_play:read:incoming sampling_rate[0]=" + sampling_rate + " kHz");
                channels_ = channels;
            }

            return;
        }

        if (Callstate.call_first_audio_frame_received == -1)
        {
            Callstate.call_first_audio_frame_received = System.currentTimeMillis();

            sampling_rate_ = sampling_rate;
            Log.i(TAG, "audio_play:read:incoming sampling_rate[1]=" + sampling_rate + " kHz");
            channels_ = channels;

            Log.i(TAG, "audio_play:read:init sample_count=" + sample_count + " channels=" + channels + " sampling_rate=" + sampling_rate);


            temp_string_a = "" + (int) ((Callstate.call_first_audio_frame_received - Callstate.call_start_timestamp) / 1000) + "s";
            CallingActivity.update_top_text_line(temp_string_a, 4);

            // AudioReceiver.buffer_size = AudioTrack.getMinBufferSize((int) sampling_rate, channels, AudioFormat.ENCODING_PCM_16BIT);
            // Log.i(TAG, "audio_play:read:init min buffer size(calc)=" + AudioReceiver.buffer_size);

            // HINT: PCM_16 needs 2 bytes per sample per channel
            AudioReceiver.buffer_size = (int) ((sample_count * channels) * 2); // TODO: this is really bad
            AudioReceiver.sleep_millis = (int) (((float) sample_count / (float) sampling_rate) * 1000.0f * 0.9f); // TODO: this is bad also
            Log.i(TAG, "audio_play:read:init buffer_size=" + AudioReceiver.buffer_size);
            Log.i(TAG, "audio_play:read:init sleep_millis=" + AudioReceiver.sleep_millis);

            // reset audio in buffers
            int i = 0;
            for (i = 0; i < audio_in_buffer_max_count; i++)
            {
                try
                {
                    audio_buffer_2[i].clear();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    audio_buffer_2[i] = null;
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                audio_buffer_2[i] = ByteBuffer.allocateDirect(AudioReceiver.buffer_size);
                audio_buffer_2_read_length[i] = 0;
                Log.i(TAG, "audio_play:audio_buffer_2[" + i + "] size=" + AudioReceiver.buffer_size);
            }

            audio_in_buffer_element_count = 0;
            audio_buffer_play = ByteBuffer.allocateDirect(AudioReceiver.buffer_size);

            // always write to buffer[0] in the pipeline !! -----------
            set_JNI_audio_buffer2(audio_buffer_2[0]);
            // always write to buffer[0] in the pipeline !! -----------

            Log.i(TAG, "audio_play:audio_buffer_play size=" + AudioReceiver.buffer_size);
        }

        // TODO: dirty hack, "make good"
        try
        {
            audio_buffer_read_write(sample_count, channels, sampling_rate, true);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "audio_play:EE3:" + e.getMessage());
        }
    }

    // -------- called by AV native methods --------
    // -------- called by AV native methods --------
    // -------- called by AV native methods --------


    // -------- called by native methods --------
    // -------- called by native methods --------
    // -------- called by native methods --------

    static void android_tox_callback_self_connection_status_cb_method(int a_TOX_CONNECTION)
    {
        Log.i(TAG, "self_connection_status:" + a_TOX_CONNECTION);

        if (bootstrapping)
        {
            Log.i(TAG, "self_connection_status:bootstrapping=true");
            // we just went online
            if (a_TOX_CONNECTION != 0)
            {
                Log.i(TAG, "self_connection_status:bootstrapping set to false");
                bootstrapping = false;
            }
        }

        // -- notification ------------------
        // -- notification ------------------
        change_notification(a_TOX_CONNECTION);
        // -- notification ------------------
        // -- notification ------------------
    }

    static void android_tox_callback_friend_name_cb_method(long friend_number, String friend_name, long length)
    {
        Log.i(TAG, "friend_name:friend:" + friend_number + " name:" + friend_name);

        if (friend_list_fragment != null)
        {
            FriendList f = main_get_friend(friend_number);
            if (f != null)
            {
                f.name = friend_name;
                update_friend_in_db_name(f);
                friend_list_fragment.modify_friend(f, friend_number);
            }
        }
    }

    static void android_tox_callback_friend_status_message_cb_method(long friend_number, String status_message, long length)
    {
        Log.i(TAG, "friend_status_message:friend:" + friend_number + " status message:" + status_message);

        if (friend_list_fragment != null)
        {
            FriendList f = main_get_friend(friend_number);
            if (f != null)
            {
                f.status_message = status_message;
                update_friend_in_db_status_message(f);
                friend_list_fragment.modify_friend(f, friend_number);
            }
        }
    }

    static void android_tox_callback_friend_status_cb_method(long friend_number, int a_TOX_USER_STATUS)
    {
        Log.i(TAG, "friend_status:friend:" + friend_number + " status:" + a_TOX_USER_STATUS);

        FriendList f = main_get_friend(friend_number);
        if (f != null)
        {
            f.TOX_USER_STATUS = a_TOX_USER_STATUS;
            update_friend_in_db_status(f);

            try
            {
                message_list_activity.set_friend_status_icon();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            try
            {
                friend_list_fragment.modify_friend(f, friend_number);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    static void android_tox_callback_friend_connection_status_cb_method(long friend_number, int a_TOX_CONNECTION)
    {
        Log.i(TAG, "friend_connection_status:friend:" + friend_number + " connection status:" + a_TOX_CONNECTION);
        if (friend_list_fragment != null)
        {
            FriendList f = main_get_friend(friend_number);
            if (f != null)
            {
                f.TOX_CONNECTION = a_TOX_CONNECTION;
                update_friend_in_db_connection_status(f);

                try
                {
                    message_list_activity.set_friend_connection_status_icon();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }

                try
                {
                    friend_list_fragment.modify_friend(f, friend_number);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    static void android_tox_callback_friend_typing_cb_method(long friend_number, final int typing)
    {
        Log.i(TAG, "friend_typing_cb:fn=" + friend_number + " typing=" + typing);
        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    if (typing == 1)
                    {
                        ml_friend_typing.setText("friend is typing ...");
                    }
                    else
                    {
                        ml_friend_typing.setText("");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        main_handler_s.post(myRunnable);
    }

    static void android_tox_callback_friend_read_receipt_cb_method(long friend_number, long message_id)
    {
        Log.i(TAG, "friend_read_receipt:friend:" + friend_number + " message_id:" + message_id);

        try
        {
            // there can be older messages with same message_id for this friend! so always take the latest one! -------
            final Message m = orma.selectFromMessage().
                    message_idEq(message_id).
                    tox_friendpubkeyEq(tox_friend_get_public_key__wrapper(friend_number)).
                    directionEq(1).
                    orderByIdDesc().
                    toList().get(0);
            // there can be older messages with same message_id for this friend! so always take the latest one! -------

            // Log.i(TAG, "friend_read_receipt:m=" + m);
            Log.i(TAG, "friend_read_receipt:m:message_id=" + m.message_id + " text=" + m.text + " friendpubkey=" + m.tox_friendpubkey + " read=" + m.read + " direction=" + m.direction);

            if (m != null)
            {
                Runnable myRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            m.rcvd_timestamp = System.currentTimeMillis();
                            m.read = true;
                            update_message_in_db_read_rcvd_timestamp(m);

                            // TODO this updates all messages. should be done nicer and faster!
                            update_message_view();
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                main_handler_s.post(myRunnable);
            }
        }
        catch (Exception e)
        {
            Log.i(TAG, "friend_read_receipt:EE:" + e.getMessage());
            e.printStackTrace();
        }
    }

    static void android_tox_callback_friend_request_cb_method(String friend_public_key, String friend_request_message, long length)
    {
        Log.i(TAG, "friend_request:friend:" + friend_public_key + " friend request message:" + friend_request_message);

        final String friend_public_key__final = friend_public_key;

        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(20); // wait a bit
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                // ---- auto add all friends ----
                // ---- auto add all friends ----
                // ---- auto add all friends ----
                long friendnum = tox_friend_add_norequest(friend_public_key__final); // add friend
                update_savedata_file(); // save toxcore datafile (new friend added)

                FriendList f = new FriendList();
                f.tox_public_key_string = friend_public_key__final;
                f.TOX_USER_STATUS = 0;
                f.TOX_CONNECTION = 0;

                try
                {
                    orma.insertIntoFriendList(f);
                }
                catch (android.database.sqlite.SQLiteConstraintException e)
                {
                }

                // ---- auto add all friends ----
                // ---- auto add all friends ----
                // ---- auto add all friends ----
            }
        };
        t.start();
    }

    static void android_tox_callback_friend_message_cb_method(long friend_number, int message_type, String friend_message, long length)
    {
        Log.i(TAG, "friend_message:friend:" + friend_number + " message:" + friend_message);

        Message m = new Message();
        // m.tox_friendnum = friend_number;
        m.tox_friendpubkey = tox_friend_get_public_key__wrapper(friend_number);
        m.direction = 0; // msg received
        m.TOX_MESSAGE_TYPE = 0;
        m.rcvd_timestamp = System.currentTimeMillis();
        m.text = friend_message;

        insert_into_message_db(m, true);

        try
        {
            // update "new" status on friendlist fragment
            FriendList f = orma.selectFromFriendList().tox_public_key_stringEq(m.tox_friendpubkey).toList().get(0);
            friend_list_fragment.modify_friend(f, friend_number);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "update *new* status:EE1:" + e.getMessage());
        }

        // start "new" notification
        Runnable myRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    // allow notification every n seconds
                    if ((Notification_new_message_last_shown_timestamp + Notification_new_message_every_millis) < System.currentTimeMillis())
                    {

                        if (PREF__notification)
                        {
                            Notification_new_message_last_shown_timestamp = System.currentTimeMillis();

                            Intent notificationIntent = new Intent(context_s, MainActivity.class);
                            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            PendingIntent pendingIntent = PendingIntent.getActivity(context_s, 0, notificationIntent, 0);

                            // -- notification ------------------
                            // -- notification ------------------

                            NotificationCompat.Builder b = new NotificationCompat.Builder(context_s);
                            b.setContentIntent(pendingIntent);
                            b.setSmallIcon(R.drawable.circle_orange);
                            b.setLights(Color.parseColor("#ffce00"), 500, 500);
                            Uri default_notification_sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                            if (PREF__notification_sound)
                            {
                                b.setSound(default_notification_sound);
                            }

                            if (PREF__notification_vibrate)
                            {
                                long[] vibrate_pattern = {100, 300};
                                b.setVibrate(vibrate_pattern);
                            }

                            b.setContentTitle("TRIfA");
                            b.setAutoCancel(true);
                            b.setContentText("new Message");

                            Notification notification3 = b.build();
                            nmn3.notify(Notification_new_message_ID, notification3);
                            // -- notification ------------------
                            // -- notification ------------------
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };
        try
        {
            main_handler_s.post(myRunnable);
        }
        catch (Exception e)
        {
        }
    }

    static void android_tox_callback_file_recv_control_cb_method(long friend_number, long file_number, int control)
    {
        Log.i(TAG, "file_recv_control:" + friend_number + ":" + file_number + ":" + control);
    }

    static void android_tox_callback_file_chunk_request_cb_method(long friend_number, long file_number, long position, long length)
    {
        Log.i(TAG, "file_chunk_request:" + friend_number + ":" + file_number + ":" + position + ":" + length);
    }

    static void android_tox_callback_file_recv_cb_method(long friend_number, long file_number, int a_TOX_FILE_KIND, long file_size, String filename, long filename_length)
    {
        Log.i(TAG, "file_recv:" + friend_number + ":" + file_number + ":" + a_TOX_FILE_KIND + ":" + file_size + ":" + filename + ":" + filename_length);

        if (a_TOX_FILE_KIND == TOX_FILE_KIND_AVATAR.value)
        {
            Log.i(TAG, "file_recv:incoming avatar");

            Filetransfer f = new Filetransfer();
            f.tox_public_key_string = tox_friend_get_public_key__wrapper(friend_number);
            f.direction = TRIFA_FT_DIRECTION_INCOMING.value;
            f.file_number = file_number;
            f.kind = a_TOX_FILE_KIND;
            f.state = TOX_FILE_CONTROL_CANCEL.value;
            f.path_name = VFS_TMP_FILE_DIR + "/" + f.tox_public_key_string + "/";
            f.file_name = filename.substring(0, (int) filename_length);
            f.filesize = file_size;
            f.current_position = 0;

            insert_into_filetransfer_db(f);

            // TODO: we just accept incoming avatar, maybe make some checks first?
            tox_file_control(friend_number, file_number, TOX_FILE_CONTROL_RESUME.value);
        }
        else
        {
            Log.i(TAG, "file_recv:incoming regular file");
        }
    }

    static void android_tox_callback_file_recv_chunk_cb_method(long friend_number, long file_number, long position, byte[] data, long length)
    {
        Log.i(TAG, "file_recv_chunk:" + friend_number + ":" + file_number + ":position=" + position + ":length=" + length + ":data len=" + data.length + ":data=" + data);

        Filetransfer f = null;
        try
        {
            f = orma.selectFromFiletransfer().file_numberEq(file_number).and().tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friend_number)).toList().get(0);

            if (position == 0)
            {
                // file start. just to be sure, make directories
                info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File(f.path_name + "/" + f.file_name);
                info.guardianproject.iocipher.File f2 = new info.guardianproject.iocipher.File(f1.getParent());
                Log.i(TAG, "file_recv_chunk:f1=" + f1.getAbsolutePath());
                Log.i(TAG, "file_recv_chunk:f2=" + f2.getAbsolutePath());
                f2.mkdirs();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (length == 0)
        {
            try
            {
                Log.i(TAG, "file_recv_chunk:file fully received");

                info.guardianproject.iocipher.FileOutputStream fos = null;
                fos = cache_ft_fos.get(tox_friend_get_public_key__wrapper(friend_number) + ":" + friend_number);

                if (f.fos_open)
                {
                    try
                    {
                        fos.close();
                    }
                    catch (Exception e3)
                    {
                        Log.i(TAG, "file_recv_chunk:EE3:" + e3.getMessage());
                    }
                }
                f.fos_open = false;
                update_filetransfer_db_fos_open(f);

                move_tmp_file_to_real_file(f.path_name, f.file_name, VFS_FILE_DIR + "/" + f.tox_public_key_string + "/", f.file_name);

                // put into "File" table
                com.zoffcc.applications.trifa.File file_ = new com.zoffcc.applications.trifa.File();
                file_.kind = f.kind;
                file_.direction = f.direction;
                file_.tox_public_key_string = f.tox_public_key_string;
                file_.path_name = VFS_FILE_DIR + "/" + f.tox_public_key_string + "/";
                file_.file_name = f.file_name;
                orma.insertIntoFile(file_);

                Log.i(TAG, "file_recv_chunk:kind=" + f.kind);
                if (f.kind == TOX_FILE_KIND_AVATAR.value)
                {
                    set_friend_avatar(tox_friend_get_public_key__wrapper(friend_number), VFS_FILE_DIR + "/" + f.tox_public_key_string + "/", f.file_name);
                }

                // remove FT from DB
                orma.deleteFromFiletransfer().tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friend_number)).and().
                        file_numberEq(file_number).execute();
            }
            catch (Exception e2)
            {
                e2.printStackTrace();
                Log.i(TAG, "file_recv_chunk:EE2:" + e2.getMessage());
            }
        }
        else
        {
            try
            {
                info.guardianproject.iocipher.FileOutputStream fos = null;
                if (!f.fos_open)
                {
                    fos = new info.guardianproject.iocipher.FileOutputStream(f.path_name + "/" + f.file_name);
                    cache_ft_fos.put(tox_friend_get_public_key__wrapper(friend_number) + ":" + friend_number, fos);
                    f.fos_open = true;
                }
                else
                {
                    fos = cache_ft_fos.get(tox_friend_get_public_key__wrapper(friend_number) + ":" + friend_number);
                }
                fos.write(data);
                f.current_position = position;
                update_filetransfer_db_full(f);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.i(TAG, "file_recv_chunk:EE1:" + e.getMessage());
            }
        }
    }

    // void test(int i)
    // {
    //    Log.i(TAG, "test:" + i);
    // }

    static void android_tox_log_cb_method(int a_TOX_LOG_LEVEL, String file, long line, String function, String message)
    {
        if (CTOXCORE_NATIVE_LOGGING)
        {
            Log.i(TAG, "C-TOXCORE:" + ToxVars.TOX_LOG_LEVEL.value_str(a_TOX_LOG_LEVEL) + ":file=" + file + ":linenum=" + line + ":func=" + function + ":msg=" + message);
        }
    }

    static void logger(int level, String text)
    {
        Log.i(TAG, text);
    }
    // -------- called by native methods --------
    // -------- called by native methods --------
    // -------- called by native methods --------

    /*
     * this is used to load the native library on
	 * application startup. The library has already been unpacked at
	 * installation time by the package manager.
	 */
    static
    {
        try
        {
            System.loadLibrary("jni-c-toxcore");
            native_lib_loaded = true;
            Log.i(TAG, "successfully loaded native library");
        }
        catch (java.lang.UnsatisfiedLinkError e)
        {
            native_lib_loaded = false;
            Log.i(TAG, "loadLibrary jni-c-toxcore failed!");
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        // just in case, update own activity pointer!
        main_activity_s = this;
    }

    public static long tox_friend_by_public_key__wrapper(@NonNull String friend_public_key_string)
    {
        if (cache_pubkey_fnum.containsKey(friend_public_key_string))
        {
            // Log.i(TAG, "cache hit:1");
            return cache_pubkey_fnum.get(friend_public_key_string);
        }
        else
        {
            if (cache_pubkey_fnum.size() >= 20)
            {
                // TODO: bad!
                cache_pubkey_fnum.clear();
            }
            long result = tox_friend_by_public_key(friend_public_key_string);
            cache_pubkey_fnum.put(friend_public_key_string, result);
            return result;
        }
    }

    public static String tox_friend_get_public_key__wrapper(long friend_number)
    {
        if (cache_fnum_pubkey.containsKey(friend_number))
        {
            // Log.i(TAG, "cache hit:2");
            return cache_fnum_pubkey.get(friend_number);
        }
        else
        {
            if (cache_fnum_pubkey.size() >= 20)
            {
                // TODO: bad!
                cache_fnum_pubkey.clear();
            }
            String result = tox_friend_get_public_key(friend_number);
            cache_fnum_pubkey.put(friend_number, result);
            return result;
        }
    }

    public void show_add_friend(View view)
    {
        Intent intent = new Intent(this, AddFriendActivity.class);
        // intent.putExtra("key", value);
        startActivityForResult(intent, AddFriendActivity_ID);
    }


    static void update_filetransfer_db_fos_open(final Filetransfer f)
    {
        orma.updateFiletransfer().
                tox_public_key_stringEq(f.tox_public_key_string).
                file_numberEq(f.file_number).
                fos_open(f.fos_open).
                execute();
    }

    static void update_filetransfer_db_full(final Filetransfer f)
    {
        orma.updateFiletransfer().
                tox_public_key_stringEq(f.tox_public_key_string).
                file_numberEq(f.file_number).
                direction(f.direction).
                kind(f.kind).
                state(f.state).
                path_name(f.path_name).
                file_name(f.file_name).
                fos_open(f.fos_open).
                filesize(f.filesize).
                current_position(f.current_position).
                execute();
    }

    static void insert_into_filetransfer_db(final Filetransfer f)
    {
        //Thread t = new Thread()
        //{
        //    @Override
        //    public void run()
        //    {
        orma.insertIntoFiletransfer(f);
        //    }
        //};
        //t.start();
    }

    static void set_friend_avatar(String friend_pubkey, String avatar_path_name, String avatar_file_name)
    {
        try
        {
            Log.i(TAG, "set_friend_avatar:update:pubkey=" + friend_pubkey + " path=" + avatar_path_name + " file=" + avatar_file_name);

            orma.updateFriendList().tox_public_key_stringEq(friend_pubkey).
                    avatar_pathname(avatar_path_name).
                    avatar_filename(avatar_file_name).
                    execute();

            update_display_friend_avatar(friend_pubkey, avatar_path_name, avatar_file_name);
        }
        catch (Exception e)
        {
            Log.i(TAG, "set_friend_avatar:EE:" + e.getMessage());
            e.printStackTrace();
        }
    }

    static void update_display_friend_avatar(String friend_pubkey, String avatar_path_name, String avatar_file_name)
    {
        // try to load avatar image, and set in friendlist fragment
        // zzzzzzzzzz
    }

    static void move_tmp_file_to_real_file(String src_path_name, String src_file_name, String dst_path_name, String dst_file_name)
    {
        Log.i(TAG, "move_tmp_file_to_real_file:" + src_path_name + "/" + src_file_name + " -> " + dst_path_name + "/" + dst_file_name);
        try
        {
            info.guardianproject.iocipher.File f1 = new info.guardianproject.iocipher.File(src_path_name + "/" + src_file_name);
            info.guardianproject.iocipher.File f2 = new info.guardianproject.iocipher.File(dst_path_name + "/" + dst_file_name);
            info.guardianproject.iocipher.File dst_dir = new info.guardianproject.iocipher.File(dst_path_name + "/");
            dst_dir.mkdirs();
            f1.renameTo(f2);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    static void insert_into_message_db(final Message m, final boolean update_message_view_flag)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                // Log.i(TAG, "insert_into_message_db:m=" + m);
                orma.insertIntoMessage(m);
                if (update_message_view_flag)
                {
                    update_message_view();
                }
            }
        };
        t.start();
    }

    static void insert_into_friendlist_db(final FriendList f)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                orma.insertIntoFriendList(f);
            }
        };
        t.start();
    }

    static void delete_friend_all_messages(final long friendnum)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                orma.deleteFromMessage().tox_friendpubkeyEq(tox_friend_get_public_key__wrapper(friendnum)).execute();
            }
        };
        t.start();
    }


    static void delete_friend(final String friend_pubkey)
    {
        Thread t = new Thread()
        {
            @Override
            public void run()
            {
                orma.deleteFromFriendList().
                        tox_public_key_stringEq(friend_pubkey).
                        execute();
            }
        };
        t.start();
    }

    static void update_message_view()
    {
        try
        {
            Log.i(TAG, "update_message_view:001 " + message_list_fragment);
            Log.i(TAG, "update_message_view:002 " + message_list_fragment.isAdded() + " " + message_list_fragment.isVisible());
            // update the message view (if possbile)
            if ((message_list_fragment.isAdded()) && (message_list_fragment.isVisible()))
            {
                Log.i(TAG, "update_message_view:003");
                MainActivity.message_list_fragment.update_all_messages();
                Log.i(TAG, "update_message_view:004");
            }
        }

        catch (Exception e)
        {
            e.printStackTrace();
            Log.i(TAG, "update_message_view:EE:" + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AddFriendActivity_ID)
        {
            if (resultCode == RESULT_OK)
            {
                String friend_tox_id1 = data.getStringExtra("toxid");
                String friend_tox_id = "";
                friend_tox_id = friend_tox_id1.toUpperCase().replace(" ", "").replaceFirst("tox:", "").replaceFirst("TOX:", "").replaceFirst("Tox:", "");

                add_friend_real(friend_tox_id);
            }
            else
            {
                // (resultCode == RESULT_CANCELED)
            }
        }
    }

    static void add_friend_real(String friend_tox_id)
    {
        Log.i(TAG, "add friend ID:" + friend_tox_id);

        // add friend ---------------
        long friendnum = tox_friend_add(friend_tox_id, "please add me"); // add friend
        Log.i(TAG, "add friend  #:" + friendnum);
        update_savedata_file(); // save toxcore datafile (new friend added)

        if (friendnum > -1)
        {
            // nospam=8 chars, checksum=4 chars
            String friend_public_key = friend_tox_id.substring(0, friend_tox_id.length() - 12);
            Log.i(TAG, "add friend PK:" + friend_public_key);

            FriendList f = new FriendList();
            f.tox_public_key_string = friend_public_key;
            try
            {
                // set name as the last 5 char of TOXID (until we get a name sent from friend)
                f.name = friend_public_key.substring(friend_public_key.length() - 5, friend_public_key.length());
            }
            catch (Exception e)
            {
                e.printStackTrace();
                f.name = "Unknown";
            }
            f.TOX_USER_STATUS = 0;
            f.TOX_CONNECTION = 0;

            try
            {
                insert_into_friendlist_db(f);
            }
            catch (android.database.sqlite.SQLiteConstraintException e)
            {
                e.printStackTrace();
            }

            friend_list_fragment.modify_friend(f, friendnum);
        }

        if (friendnum == -1)
        {
            Log.i(TAG, "friend already added, or request already sent");
        }
        // add friend ---------------
    }

    static String get_friend_name_from_num(long friendnum)
    {
        String result = "Unknown";
        try
        {
            result = orma.selectFromFriendList().
                    tox_public_key_stringEq(tox_friend_get_public_key__wrapper(friendnum)).
                    toList().get(0).name;
        }
        catch (Exception e)
        {
            result = "Unknown";
            e.printStackTrace();
        }

        return result;
    }

    synchronized static boolean audio_buffer_read_write(long sample_count, int channels, long sampling_rate, boolean write)
    {
        if (write)
        {
            // Log.i(TAG, "audio_buffer_read_write:write:START");
            int j = 0;
            if (audio_in_buffer_element_count < audio_in_buffer_max_count)
            {
                if (audio_in_buffer_element_count > 0)
                {
                    for (j = 0; j < audio_in_buffer_element_count; j++)
                    {
                        audio_buffer_2[audio_in_buffer_element_count - 1 - j].rewind();
                        audio_buffer_2[audio_in_buffer_element_count - j].rewind();
                        // Log.i(TAG, "audio_play:write:buffer size src=" + audio_buffer_2[audio_in_buffer_element_count - 1 - j].limit());
                        // Log.i(TAG, "audio_play:write:buffer pos src=" + audio_buffer_2[audio_in_buffer_element_count - 1 - j].position());
                        // Log.i(TAG, "audio_play:write:buffer size dst=" + audio_buffer_2[audio_in_buffer_element_count - j].limit());
                        // Log.i(TAG, "audio_play:write:buffer pos dst=" + audio_buffer_2[audio_in_buffer_element_count - j].position());
                        // audio_buffer_2[audio_in_buffer_element_count - j].put(audio_buffer_2[audio_in_buffer_element_count - 1 - j].array());
                        audio_buffer_2[audio_in_buffer_element_count - j].put(audio_buffer_2[audio_in_buffer_element_count - 1 - j].array(), 0, AudioReceiver.buffer_size);
                        audio_buffer_2[audio_in_buffer_element_count - j].rewind();
                        audio_buffer_2_read_length[audio_in_buffer_element_count - j] = audio_buffer_2_read_length[audio_in_buffer_element_count - 1 - j];
                        // Log.i(TAG, "audio_play:write:mv " + (audio_in_buffer_element_count - 1 - j + " -> " + (audio_in_buffer_element_count - j)));
                    }
                }
                // Log.i(TAG, "audio_play:write:set buffer 0:len=" + sample_count);
                audio_buffer_2_read_length[0] = (int) (sample_count * channels * 2);
                audio_in_buffer_element_count++;
                // Log.i(TAG, "audio_play:write:element count new=" + audio_in_buffer_element_count);
                // Log.i(TAG, "audio_play:write:element count new=" + audio_in_buffer_element_count);

                // wake up audio thread -----------
                try
                {
                    audio_thread.interrupt();
                }
                catch (Exception e)
                {
                    Log.i(TAG, "audio_buffer_read_write:write:wake up audio thread:EE:" + e.getMessage());
                }
                // wake up audio thread -----------
            }
            else
            {
                Log.i(TAG, "audio_buffer_read_write:write:* buffer FULL *");
            }

            // Log.i(TAG, "audio_buffer_read_write:write:END");

            return true;
        }
        else // read
        {
            // Log.i(TAG, "audio_buffer_read_write:READ:START");

            if (audio_in_buffer_element_count > 0)
            {
                // Log.i(TAG, "audio_play:read:load buffer " + (audio_in_buffer_element_count - 1) + ":len=" + audio_buffer_2_read_length[audio_in_buffer_element_count - 1]);

                audio_buffer_play.rewind();
                audio_buffer_play.put(audio_buffer_2[audio_in_buffer_element_count - 1].array(), 0, AudioReceiver.buffer_size);
                audio_buffer_play_length = audio_buffer_2_read_length[audio_in_buffer_element_count - 1];
                audio_in_buffer_element_count--;
                // Log.i(TAG, "audio_play:read:element count new=" + audio_in_buffer_element_count);

                // Log.i(TAG, "audio_buffer_read_write:READ:END01");

                return true;
            }

            // Log.i(TAG, "audio_buffer_read_write:READ:END02");

            return false;
        }
    }

    static int add_tcp_relay_single_wrapper(String ip, long port, String key_hex)
    {
        return add_tcp_relay_single(ip, key_hex, port);
    }

    static int bootstrap_single_wrapper(String ip, long port, String key_hex)
    {
        return bootstrap_single(ip, key_hex, port);
    }


    void sendEmailWithAttachment(Context c, final String recipient, final String subject, final String message, final String full_file_name, final String full_file_name_suppl)
    {
        try
        {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", recipient, null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            ArrayList<Uri> uris = new ArrayList<>();
            uris.add(Uri.parse("file://" + full_file_name));
            try
            {
                if (new File(full_file_name_suppl).length() > 0)
                {
                    uris.add(Uri.parse("file://" + full_file_name_suppl));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(emailIntent, 0);
            List<LabeledIntent> intents = new ArrayList<>();

            if (resolveInfos.size() != 0)
            {
                for (ResolveInfo info : resolveInfos)
                {
                    Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    System.out.println("email:" + "comp=" + info.activityInfo.packageName + " " + info.activityInfo.name);
                    intent.setComponent(new ComponentName(info.activityInfo.packageName, info.activityInfo.name));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{recipient});
                    if (subject != null)
                    {
                        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                    }
                    if (message != null)
                    {
                        intent.putExtra(Intent.EXTRA_TEXT, message);
                    }
                    intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    intents.add(new LabeledIntent(intent, info.activityInfo.packageName, info.loadLabel(getPackageManager()), info.icon));
                }
                Intent chooser = Intent.createChooser(intents.remove(intents.size() - 1), "Send email with attachments");
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toArray(new LabeledIntent[intents.size()]));
                startActivity(chooser);
            }
            else
            {
                System.out.println("email:" + "No Email App found");
                new AlertDialog.Builder(c).setMessage("No Email App found").setPositiveButton("Ok", null).show();
            }
        }
        catch (ActivityNotFoundException e)
        {
            // cannot send email for some reason
        }
    }


    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------
    public static void crash_app_java(int type)
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:J =======+++++");
        System.out.println("+++++======================+++++");
        if (type == 1)
        {
            Java_Crash_001();
        }
        else if (type == 2)
        {
            Java_Crash_002();
        }
        else
        {
            stackOverflow();
        }
    }

    public static void Java_Crash_001()
    {
        Integer i = null;
        i.byteValue();
    }

    public static void Java_Crash_002()
    {
        View v = null;
        v.bringToFront();
    }

    public static void stackOverflow()
    {
        stackOverflow();
    }

    public static void crash_app_C()
    {
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======= CRASH  =======+++++");
        System.out.println("+++++======================+++++");
        System.out.println("+++++======= TYPE:C =======+++++");
        System.out.println("+++++======================+++++");
        AppCrashC();
    }

    public static native void AppCrashC();
    // --------- make app crash ---------
    // --------- make app crash ---------
    // --------- make app crash ---------

}

