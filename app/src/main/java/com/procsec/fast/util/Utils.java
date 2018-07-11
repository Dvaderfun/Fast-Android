package com.procsec.fast.util;

import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.net.*;
import android.preference.*;
import android.support.annotation.*;
import android.support.v4.graphics.*;
import android.support.v7.app.*;
import android.util.*;
import com.procsec.fast.*;
import com.procsec.fast.common.*;
import com.procsec.fast.io.*;
import com.squareup.picasso.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import ru.lischenko_dev.fastmessenger.R;

public class Utils {
	
	public static SimpleDateFormat dateFormatter;
    public static SimpleDateFormat dateMonthFormatter;
    public static SimpleDateFormat dateYearFormatter;
	
	static {
        dateFormatter = new SimpleDateFormat("HH:mm"); // 15:57
        dateMonthFormatter = new SimpleDateFormat("d MMM"); // 23 Окт
        dateYearFormatter = new SimpleDateFormat("d MMM, yyyy"); // 23 Окт, 2015
    }
	
    public static float convertDpToPixel(float dp) {
        Resources resources = FApp.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }
	
	public static String parseDate(long date) {
        Date currentDate = new Date();
        Date msgDate = new Date(date);

        if (currentDate.getYear() > msgDate.getYear()) {
            return dateYearFormatter.format(date);
        } else if (currentDate.getMonth() > msgDate.getMonth()
				   || currentDate.getDate() > msgDate.getDate()) {
            return dateMonthFormatter.format(date);
        }

        return dateFormatter.format(date);
    }
	
	public static long getPeerId(int userId, int chatId, int groupId) {
        return groupId > 0 ? (-groupId)
			: chatId > 0 ? (2_000_000_000 + chatId)
			: userId;
    }
	
	public static byte[] serialize(Object source) {
        try {
            BytesOutputStream bos = new BytesOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);

            out.writeObject(source);
            out.close();
            return bos.getByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
	
	public static Object deserialize(byte[] source) {
        if (ArrayUtil.isEmpty(source)) {
            return null;
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(source);
            ObjectInputStream in = new ObjectInputStream(bis);

            Object o = in.readObject();

            in.close();
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap loadBitmap(String url) {
        Bitmap bm = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            bm = BitmapFactory.decodeStream(bis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }
	
    public static int pxFromDp(int dp) {
        Resources resources = FApp.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    public static float convertPixelsToDp(float px) {
        Resources resources = FApp.context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }

    public static SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(FApp.context);
    }

    public static int getThemeAttrColor(int attr) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = FApp.context.getTheme();
		theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
		@ColorInt int color = typedValue.data;
		
		return color;
    }

    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            return null;
        }
    }

    public static boolean hasConnection(Context c) {
        if (c == null) {
            return false;
        }
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());

    }

    public static int getThemeAttrColor(int attr, float alpha) {
        final int color = getThemeAttrColor(attr);
        final int originalAlpha = Color.alpha(color);
        return ColorUtils.setAlphaComponent(color, Math.round(originalAlpha * alpha));
    }

    public static void showAlert(Context c, String title, String message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(c);
        adb.setTitle(title);
        adb.setMessage(message);
        AlertDialog alert = adb.create();
        alert.show();
    }

    public static class RoundedTransformation implements Transformation {
        int pixels;

        public RoundedTransformation(int pixels) {
            this.pixels = pixels;
        }

        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap output = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, source.getWidth(), source.getHeight());
            final RectF rectF = new RectF(rect);
            final float roundPx = pixels;

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(source, rect, rect, paint);

            if (source != output) {
                source.recycle();
            }

            return output;
        }

        @Override
        public String key() {
            return "round";
        }
    }
}