package com.lht.paintviewdemo.util;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.lht.paintviewdemo.R;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by lht-Mac on 2017/9/29.
 */

public class StatusBarUtil {

    /**
     * 修改状态栏颜色，支持4.4以上版本
     * @param activity
     * @param colorId
     */
    public static void setStatusBarColor(Activity activity, int colorId) {
        if (!isEMUI3_1() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColorApi21(activity, activity.getResources().getColor(colorId));
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 将状态栏设置为透明，使用背景颜色
            transparencyBar(activity);
            activity.getWindow().getDecorView().setBackgroundResource(R.color.colorPrimary);
        }
    }

    /**
     * 19以上修改状态栏为全透明
     * @param activity
     */
    public static void transparencyBar(Activity activity){
        if(!isEMUI3_1() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setStatusBarColorApi21(activity, Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window window =activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    /**
     * 21以上设置状态栏背景颜色
     * @param activity
     * @param color
     */
    private static void setStatusBarColorApi21(Activity activity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = activity.getWindow();
            // 取消设置透明状态栏，不然颜色设置不生效
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            // 设置此flag才可对状态栏进行颜色设置 21
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 设置状态栏颜色 21
            window.setStatusBarColor(color);
        }
    }

    private static boolean isEMUI3_1() {
        if ("EmotionUI_3.1".equals(getEmuiVersion())) {
            return true;
        }
        return false;
    }

    /**
     * 获取华为EMUI版本
     * @return
     */
    private static String getEmuiVersion(){
        Class<?> classType = null;
        try {
            classType = Class.forName("android.os.SystemProperties");
            Method getMethod = classType.getDeclaredMethod("get", String.class);
            return (String)getMethod.invoke(classType, "ro.build.version.emui");
        } catch (Exception e) {
            //TODO
        }
        return "";
    }

    /**
     * 状态栏亮色模式，文字、图标黑色
     * 适配4.4以上版本MIUI V6、Flyme和6.0以上版本其他Android
     * @param activity
     * @return is success
     */
    public static boolean setStatusBarLightMode(Activity activity) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                (setStatusBarModeMIUI(activity, false) ||
                        setStatusBarModeFlyme(activity, false) ||
                        setStatusBarModeOrigin(activity, false));
    }

    /**
     * 状态栏暗色模式，文字、图标白色
     * @param activity
     * @return
     */
    public static boolean setStatusBarDarkMode(Activity activity) {
        return setStatusBarModeMIUI(activity, false) ||
                setStatusBarModeFlyme(activity, false) ||
                setStatusBarModeOrigin(activity, false);
    }

    /**
     * 设置系统状态栏
     * @param activity
     * @param dark
     * @return
     */
    private static boolean setStatusBarModeOrigin(Activity activity, boolean dark) {
        boolean result = false;

        if (dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            result = true;
        } else if (!dark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            result = true;
        }

        return result;
    }

    /**
     * 设置状态栏图标为深色和魅族特定的文字风格
     * 可以用来判断是否为Flyme用户
     * @param activity 需要设置的Activity
     * @param dark 是否把状态栏文字及图标颜色设置为深色
     * @return 成功执行返回true
     *
     */
    private static boolean setStatusBarModeFlyme(Activity activity, boolean dark) {
        boolean result = false;

        Window window = activity.getWindow();
        if (window != null) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field darkFlag = WindowManager.LayoutParams.class
                        .getDeclaredField("MEIZU_FLAG_DARK_STATUS_BAR_ICON");
                Field meizuFlags = WindowManager.LayoutParams.class
                        .getDeclaredField("meizuFlags");
                darkFlag.setAccessible(true);
                meizuFlags.setAccessible(true);

                int bit = darkFlag.getInt(null);
                int value = meizuFlags.getInt(lp);
                if (dark) {
                    value |= bit;
                } else {
                    value &= ~bit;
                }

                meizuFlags.setInt(lp, value);
                window.setAttributes(lp);
                result = true;
            } catch (Exception e) {
                //TODO
            }
        }
        return result;
    }

    /**
     * 需要MIUI V6以上
     * @param activity
     * @param dark 是否把状态栏文字及图标颜色设置为深色
     * @return  boolean 成功执行返回true
     *
     */
    private static boolean setStatusBarModeMIUI(Activity activity, boolean dark) {
        boolean result = false;

        Window window = activity.getWindow();
        if (window != null) {
            Class clazz = window.getClass();
            try {
                int darkModeFlag;
                Class layoutParams = Class.forName("android.view.MiuiWindowManager$LayoutParams");
                Field field = layoutParams.getField("EXTRA_FLAG_STATUS_BAR_DARK_MODE");
                darkModeFlag = field.getInt(layoutParams);
                Method extraFlagField = clazz.getMethod("setExtraFlags", int.class, int.class);

                if(dark){
                    extraFlagField.invoke(window,darkModeFlag,darkModeFlag);//状态栏透明且黑色字体
                }else{
                    extraFlagField.invoke(window, 0, darkModeFlag);//清除黑色字体
                }

                result=true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    //开发版 7.7.13 及以后版本采用了系统API，旧方法无效但不会报错，所以两个方式都要加上
                    if(dark){
                        activity.getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN| View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    }else {
                        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                    }
                }
            }catch (Exception e){
                //TODO
            }
        }
        return result;
    }
}
