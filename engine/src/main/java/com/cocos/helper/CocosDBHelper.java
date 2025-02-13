package com.cocos.helper;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * @Description: cocos数据库
 * @Author: starzhang
 * @Date: 2024/4/16 09:29
 */
public class CocosDBHelper extends SQLiteOpenHelper {

    public final SQLiteDatabase f34716c;

    public CocosDBHelper(Context context) {
        /**
         * cocos中localstorage就是使用的jsb.sqlite
         */
        super(context, "jsb.sqlite", (SQLiteDatabase.CursorFactory) null, 1);
        this.f34716c = getWritableDatabase();
    }

    public final void delete(String str) {
        try {
            this.f34716c.execSQL("delete from data where key=?", new String[]{str});
        } catch (SQLException e16) {
            Log.e("CocosDBHelper", "remove item:" + e16);
        }
    }

    public final void set(String str, String str2) {
        try {
            this.f34716c.execSQL("replace into data(key,value)values(?,?)", new String[]{str, str2});
        } catch (SQLException e16) {
            Log.e("CocosDBHelper", "set item:" + e16);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        if (sQLiteDatabase != null) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS data(key TEXT PRIMARY KEY,value TEXT);");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
