package com.missile.demo;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;


public class GlideUtil {

    public static void setImage(Context context, String url, ImageView img) {
        Glide.with(context).load(url).dontAnimate().fitCenter().placeholder(R.mipmap.ic_launcher_round).into(img);
    }
}
