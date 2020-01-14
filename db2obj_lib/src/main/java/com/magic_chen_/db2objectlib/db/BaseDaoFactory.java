package com.magic_chen_.db2objectlib.db;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class BaseDaoFactory {
    private static final BaseDaoFactory mInstance = new BaseDaoFactory();

    private SQLiteDatabase mSqliteDataBase;

    private String sqliteDataBasePath;
    public static final String TAG = "BaseDaoFactory";

    public static BaseDaoFactory getInstance() {
        return mInstance;
    }

    protected Map<String, BaseDao> map = Collections.synchronizedMap(new HashMap<String, BaseDao>());

    public BaseDaoFactory() {
        File file = new File(Environment.getExternalStorageDirectory(), "update");
        if (!file.exists()) {
            file.mkdirs();
        }
        String path = file.getAbsolutePath();
        Log.e(TAG, "BaseDaoFactory: .  path:"+path);
        sqliteDataBasePath = file.getAbsolutePath() + "/user.db";

        Log.e(TAG, "BaseDaoFactory: .  file exist::"+file.exists());

        mSqliteDataBase = SQLiteDatabase.openOrCreateDatabase(sqliteDataBasePath, null);
    }

    public <T> BaseDao<T> getBaseDao(Class<T> entityClass) {
        BaseDao baseDao = null;

        try {
            baseDao = BaseDao.class.newInstance();
            baseDao.init(mSqliteDataBase, entityClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return baseDao;
    }


    public <T extends BaseDao<M>, M> T getBaseDao(Class<T> base, Class<M> entityClass) {
        BaseDao baseDao = null;
        try {
            baseDao = base.newInstance();
            baseDao.init(mSqliteDataBase, entityClass);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;

    }

}
