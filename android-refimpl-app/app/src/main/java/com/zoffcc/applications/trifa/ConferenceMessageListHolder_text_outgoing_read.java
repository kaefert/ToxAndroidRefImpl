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

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.luseen.autolinklibrary.AutoLinkMode;
import com.luseen.autolinklibrary.AutoLinkOnClickListener;
import com.luseen.autolinklibrary.EmojiTextViewLinks;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import static com.zoffcc.applications.trifa.ConferenceMessageListFragment.conf_search_messages_text;
import static com.zoffcc.applications.trifa.HelperFriend.add_friend_real;
import static com.zoffcc.applications.trifa.HelperGeneric.dp2px;
import static com.zoffcc.applications.trifa.HelperGeneric.get_vfs_image_filename_own_avatar;
import static com.zoffcc.applications.trifa.HelperGeneric.long_date_time_format;
import static com.zoffcc.applications.trifa.MainActivity.PREF__global_font_size;
import static com.zoffcc.applications.trifa.MainActivity.VFS_ENCRYPT;
import static com.zoffcc.applications.trifa.MainActivity.selected_messages;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_EMOJI_ONLY_EMOJI_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_EMOJI_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.MESSAGE_TEXT_SIZE;
import static com.zoffcc.applications.trifa.TRIFAGlobals.TOXURL_PATTERN;

public class ConferenceMessageListHolder_text_outgoing_read extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    private static final String TAG = "trifa.MessageListHolder";

    private ConferenceMessage message_;
    private Context context;

    EmojiTextViewLinks textView;
    ImageView imageView;
    de.hdodenhof.circleimageview.CircleImageView img_avatar;
    TextView date_time;
    ImageView img_corner;
    LinearLayout text_block_group;
    ViewGroup layout_message_container;
    boolean is_selected = false;

    public ConferenceMessageListHolder_text_outgoing_read(View itemView, Context c)
    {
        super(itemView);

        // Log.i(TAG, "MessageListHolder");

        this.context = c;

        textView = (EmojiTextViewLinks) itemView.findViewById(R.id.m_text);
        imageView = (ImageView) itemView.findViewById(R.id.m_icon);
        img_avatar = (de.hdodenhof.circleimageview.CircleImageView) itemView.findViewById(R.id.img_avatar);
        date_time = (TextView) itemView.findViewById(R.id.date_time);
        img_corner = (ImageView) itemView.findViewById(R.id.img_corner);
        text_block_group = (LinearLayout) itemView.findViewById(R.id.text_block_group);
        layout_message_container = (ViewGroup) itemView.findViewById(R.id.layout_message_container);
    }

    public void bindMessageList(ConferenceMessage m)
    {
        message_ = m;

        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, MESSAGE_TEXT_SIZE[PREF__global_font_size]);

        is_selected = false;
        if (selected_messages.isEmpty())
        {
            is_selected = false;
        }
        else
        {
            is_selected = selected_messages.contains(m.id);
        }

        if (is_selected)
        {
            layout_message_container.setBackgroundColor(Color.GRAY);
        }
        else
        {
            layout_message_container.setBackgroundColor(Color.TRANSPARENT);
        }

        layout_message_container.setOnClickListener(onclick_listener);
        layout_message_container.setOnLongClickListener(onlongclick_listener);

        textView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                layout_message_container.performClick();
            }
        });
        textView.setOnLongClickListener(new View.OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View view)
            {
                layout_message_container.performLongClick();
                return true;
            }
        });

        textView.setCustomRegex(TOXURL_PATTERN);
        textView.addAutoLinkMode(AutoLinkMode.MODE_URL, AutoLinkMode.MODE_EMAIL, AutoLinkMode.MODE_HASHTAG,
                                 AutoLinkMode.MODE_MENTION, AutoLinkMode.MODE_CUSTOM);

        if (com.vanniktech.emoji.EmojiUtils.isOnlyEmojis(m.text))
        {
            // text consits only of emojis -> increase size
            textView.setEmojiSize((int) dp2px(MESSAGE_EMOJI_ONLY_EMOJI_SIZE[PREF__global_font_size]));
        }
        else
        {
            textView.setEmojiSize((int) dp2px(MESSAGE_EMOJI_SIZE[PREF__global_font_size]));
        }

        if ((conf_search_messages_text == null) || (conf_search_messages_text.length() == 0))
        {
            textView.setAutoLinkText(m.text);
        }
        else
        {
            textView.setAutoLinkTextHighlight(m.text, conf_search_messages_text);
        }

        date_time.setText(long_date_time_format(m.sent_timestamp));

        if (!m.read)
        {
            // not yet read
            imageView.setImageResource(R.drawable.circle_red);
        }
        else
        {
            // msg read by other party
            imageView.setImageResource(R.drawable.circle_green);
        }


        // textView.setHashtagModeColor(ContextCompat.getColor(this.context, android.R.color.holo_blue_dark));
        // textView.setPhoneModeColor(ContextCompat.getColor(this.context, android.R.color.holo_red_dark));
        // textView.setCustomModeColor(ContextCompat.getColor(this.context, android.R.color.holo_orange_dark));
        // textView.setUrlModeColor(ContextCompat.getColor(this.context, android.R.color.holo_green_dark));
        // textView.setMentionModeColor(ContextCompat.getColor(this.context, android.R.color.holo_green_light));
        // textView.setEmailModeColor(ContextCompat.getColor(this.context, android.R.color.holo_blue_bright));
        // textView.setSelectedStateColor(ContextCompat.getColor(this.context, android.R.color.holo_blue_dark));
        // textView.enableUnderLine();

        textView.setAutoLinkOnClickListener(new AutoLinkOnClickListener()
        {
            @Override
            public void onAutoLinkTextClick(AutoLinkMode autoLinkMode, String matchedText)
            {
                if (autoLinkMode == AutoLinkMode.MODE_URL)
                {
                    showDialog_url(context, "open URL?", matchedText.replaceFirst("^\\s", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_EMAIL)
                {
                    showDialog_email(context, "send Email?", matchedText.replaceFirst("^\\s", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_MENTION)
                {
                    showDialog_url(context, "open URL?", "https://twitter.com/" +
                                                         matchedText.replaceFirst("^\\s", "").replaceFirst("^@", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_HASHTAG)
                {
                    showDialog_url(context, "open URL?", "https://twitter.com/hashtag/" +
                                                         matchedText.replaceFirst("^\\s", "").replaceFirst("^#", ""));
                }
                else if (autoLinkMode == AutoLinkMode.MODE_CUSTOM) // tox: urls
                {
                    showDialog_tox(context, "add ToxID?", matchedText.replaceFirst("^\\s", ""));
                }
            }
        });


        final Drawable d_lock = new IconicsDrawable(context).icon(FontAwesome.Icon.faw_lock).color(
                context.getResources().getColor(R.color.colorPrimaryDark)).sizeDp(50);
        img_avatar.setImageDrawable(d_lock);

        try
        {
            if (VFS_ENCRYPT)
            {
                String fname = get_vfs_image_filename_own_avatar();

                info.guardianproject.iocipher.File f1 = null;
                try
                {
                    f1 = new info.guardianproject.iocipher.File(fname);
                }
                catch (Exception e)
                {
                    // e.printStackTrace();
                }

                if ((f1 != null) && (fname != null))
                {
                    if (f1.length() > 0)
                    {
                        final RequestOptions glide_options = new RequestOptions().fitCenter();
                        // GLIDE:own-avatar
                        GlideApp.
                                with(context).
                                load(f1).
                                diskCacheStrategy(DiskCacheStrategy.RESOURCE).
                                skipMemoryCache(false).
                                apply(glide_options).
                                into(img_avatar);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v)
    {
        // Log.i(TAG, "onClick");
    }

    @Override
    public boolean onLongClick(final View v)
    {
        // Log.i(TAG, "onLongClick");
        return true;
    }

    private void showDialog_url(final Context c, final String title, final String url1)
    {
        String url2 = url1;

        // check to see if protocol is specified in URL, otherwise add "http://"
        if (!url2.contains("://"))
        {
            url2 = "http://" + url1;
        }
        final String url = url2;

        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(url).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            c.startActivity(intent);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showDialog_email(final Context c, final String title, final String email_addr)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(email_addr).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            Intent emailIntent = new Intent(Intent.ACTION_SENDTO,
                                                            Uri.fromParts("mailto", email_addr, null));
                            emailIntent.setType("message/rfc822");
                            // emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject");
                            // emailIntent.putExtra(Intent.EXTRA_TEXT, "Body");
                            c.startActivity(Intent.createChooser(emailIntent, "Send email..."));
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void showDialog_tox(final Context c, final String title, final String toxid)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setMessage(toxid.toUpperCase()).setTitle(title).
                setCancelable(false).
                setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        try
                        {
                            String friend_tox_id = toxid.toUpperCase().replace(" ", "").replaceFirst("tox:",
                                                                                                     "").replaceFirst(
                                    "TOX:", "").replaceFirst("Tox:", "");
                            add_friend_real(friend_tox_id);
                        }
                        catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                        dialog.dismiss();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
    }

    private View.OnClickListener onclick_listener = new View.OnClickListener()
    {
        @Override
        public void onClick(final View v)
        {
            is_selected = ConferenceMessageListActivity.onClick_message_helper(v, is_selected, message_);
        }
    };

    private View.OnLongClickListener onlongclick_listener = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(final View v)
        {
            ConferenceMessageListActivity.long_click_message_return res = ConferenceMessageListActivity.onLongClick_message_helper(
                    context, v, is_selected, message_);
            is_selected = res.is_selected;
            return res.ret_value;
        }
    };
}
