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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

import info.guardianproject.iocipher.File;

import static com.zoffcc.applications.trifa.MainActivity.SD_CARD_TMP_DIR;
import static com.zoffcc.applications.trifa.MainActivity.StringSignature2;
import static com.zoffcc.applications.trifa.MainActivity.VFS_ENCRYPT;
import static com.zoffcc.applications.trifa.MainActivity.copy_vfs_file_to_real_file;

public class ImageviewerActivity extends AppCompatActivity
{
    private static final String TAG = "trifa.ImageviewerActy";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageviewer);

        // TODO: bad!
        String image_filename = "/xx/xyz.png";

        try
        {
            image_filename = getIntent().getStringExtra("image_filename");
            Log.i(TAG, "onCreate:image_filename=" + image_filename);
        }
        catch (Exception e)
        {
            e.getMessage();
        }

        //        final Drawable d3 = new IconicsDrawable(this).
        //                icon(GoogleMaterial.Icon.gmd_photo).
        //                backgroundColor(Color.TRANSPARENT).
        //                color(Color.parseColor("#AA000000")).sizeDp(200);

        //        final Drawable d1 = new IconicsDrawable(ImageviewerActivity.this).
        //                icon(GoogleMaterial.Icon.gmd_insert_photo).
        //                color(ImageviewerActivity.this.getResources().getColor(R.color.colorPrimaryDark)).
        //                sizeDp(200);

        final PhotoView photoView = (PhotoView) findViewById(R.id.big_image);
        photoView.setImageResource(R.drawable.round_loading_animation);

        final Handler imageviewer_handler = new Handler(getMainLooper());

        if (VFS_ENCRYPT)
        {
            final String image_filename_ = image_filename;

            final Thread t_image_loader = new Thread()
            {
                @Override
                public void run()
                {

                    info.guardianproject.iocipher.File f2 = new info.guardianproject.iocipher.File(image_filename_);
                    final String temp_file_name = copy_vfs_file_to_real_file(f2.getParent(), f2.getName(), SD_CARD_TMP_DIR, "_1");
                    Log.i(TAG, "loadData:temp_file_name=" + temp_file_name);

                    final Runnable myRunnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                //                                final Drawable d1 = new IconicsDrawable(ImageviewerActivity.this).
                                //                                        icon(GoogleMaterial.Icon.gmd_insert_photo).
                                //                                        color(ImageviewerActivity.this.getResources().getColor(R.color.colorPrimaryDark)).
                                //                                        sizeDp(200);

                                RequestOptions req_options = new RequestOptions().onlyRetrieveFromCache(true);

                                GlideApp.
                                        with(ImageviewerActivity.this).
                                        load(new File(SD_CARD_TMP_DIR + "/" + temp_file_name)).
                                        diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                                        skipMemoryCache(false).
                                        signature(StringSignature2(image_filename_)).
                                        apply(req_options).
                                        placeholder(R.drawable.round_loading_animation).
                                        listener(new com.bumptech.glide.request.RequestListener<Drawable>()
                                        {
                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource)
                                            {
                                                Log.i(TAG, "glide:onResourceReady:model=" + model);

                                                try
                                                {
                                                    java.io.File f = (java.io.File) model;
                                                    f.delete();
                                                    Log.i(TAG, "glide:cleanup:001");
                                                }
                                                catch (Exception e2)
                                                {
                                                    e2.printStackTrace();
                                                    Log.i(TAG, "glide:onResourceReady:EE:" + e2.getMessage());
                                                }

                                                return false;
                                            }

                                            @Override
                                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target target, boolean isFirstResource)
                                            {
                                                Log.i(TAG, "glide:onLoadFailed:model=" + model);

                                                try
                                                {
                                                    java.io.File f = (java.io.File) model;
                                                    f.delete();
                                                    Log.i(TAG, "glide:cleanup:002");
                                                }
                                                catch (Exception e2)
                                                {
                                                    e2.printStackTrace();
                                                    Log.i(TAG, "glide:onLoadFailed:EE:" + e2.getMessage());
                                                }

                                                return false;
                                            }

                                        }).
                                        into(photoView);
                            }
                            catch (Exception e)
                            {
                                e.printStackTrace();

                                try
                                {
                                    java.io.File f = new java.io.File(SD_CARD_TMP_DIR + "/" + temp_file_name);
                                    f.delete();
                                    Log.i(TAG, "glide:cleanup:003");
                                }
                                catch (Exception e2)
                                {
                                    e2.printStackTrace();
                                    Log.i(TAG, "glide:cleanup:EE2:" + e2.getMessage());
                                }
                            }
                        }
                    };
                    imageviewer_handler.post(myRunnable);
                }
            };
            t_image_loader.start();
        }
    }
}
