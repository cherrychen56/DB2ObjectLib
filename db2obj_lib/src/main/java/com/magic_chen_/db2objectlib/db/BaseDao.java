package com.magic_chen_.db2objectlib.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;


import com.magic_chen_.db2objectlib.annotation.DbField;
import com.magic_chen_.db2objectlib.annotation.DbTable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BaseDao<T> implements IBaseDao<T> {



    private SQLiteDatabase mSqLiteDatabase;

    private String mTableName;

    private Class<T> mEntityClass;

    private boolean isInit = false;

    private HashMap<String, Field> cacheMap;


    public void init(SQLiteDatabase sqLiteDatabase, Class<T> entityClass) {
        this.mSqLiteDatabase = sqLiteDatabase;
        this.mEntityClass = entityClass;

        if (!isInit) {
            if (mEntityClass.getAnnotation(DbTable.class) == null) {
                mTableName = mEntityClass.getSimpleName();
            } else {
                mTableName = mEntityClass.getAnnotation(DbTable.class).value();
            }

            String sqlString = getStringSql();
            mSqLiteDatabase.execSQL(sqlString);
            cacheMap = new HashMap<>();
            initCacheMap();
            isInit = true;
        }

    }

    private void initCacheMap() {
        String sql = "select * from " + mTableName + " limit 1,0";//空表
        Cursor cursor = mSqLiteDatabase.rawQuery(sql, null);
        String[] columnNames = cursor.getColumnNames();

        Field[] declareFields = mEntityClass.getDeclaredFields();
        for (Field f : declareFields) {
            f.setAccessible(true);
        }
        for (String columName : columnNames) {
            Field columnField = null;

            for (Field f : declareFields) {
                String fieldName;
                if (f.getAnnotation(DbField.class) != null) {
                    fieldName = f.getAnnotation(DbField.class).value();
                } else {
                    fieldName = f.getName();
                }

                if (fieldName.equals(columName)) {
                    columnField = f;
                    break;
                }

            }

            if (columnField != null) {
                cacheMap.put(columName, columnField);
            }

        }


    }

    private String getStringSql() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("create table if not exists ");
        stringBuffer.append(mTableName + "(");
        Field[] fields = mEntityClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Class type = field.getType();
            if (field.getAnnotation(DbField.class) != null) {
                if (type == String.class) {
                    stringBuffer.append(field.getAnnotation(DbField.class).value() + " TEXT,");
                } else if (type == Integer.class) {
                    if(field.getAnnotation(DbField.class).value().equals("id")){
                        stringBuffer.append(field.getAnnotation(DbField.class).value() + " INTEGER PRIMARY KEY AUTOINCREMENT,");
                    }else {
                        stringBuffer.append(field.getAnnotation(DbField.class).value() + " INTEGER,");
                    }

                } else if (type == Long.class) {
                    stringBuffer.append(field.getAnnotation(DbField.class).value() + " BIGINT,");
                } else if (type == Double.class) {
                    stringBuffer.append(field.getAnnotation(DbField.class).value() + " DOUBLE,");
                } else if (type == byte[].class) {
                    stringBuffer.append(field.getAnnotation(DbField.class).value() + " BLOB,");
                } else {
                    continue;
                }
            } else {
                if (type == String.class) {
                    stringBuffer.append(field.getName() + " TEXT,");
                } else if (type == Integer.class) {
                    if(field.getName().equals("id")){
                        stringBuffer.append(field.getName() + " INTEGER PRIMARY KEY AUTOINCREMENT,");
                    }else {
                        stringBuffer.append(field.getName() + " INTEGER,");
                    }
                } else if (type == Long.class) {
                    stringBuffer.append(field.getName() + " BIGINT,");
                } else if (type == Double.class) {
                    stringBuffer.append(field.getName() + " DOUBLE,");
                } else if (type == byte[].class) {
                    stringBuffer.append(field.getName() + " BLOB,");
                } else {
                    continue;
                }
            }
        }
        if (stringBuffer.charAt(stringBuffer.length() - 1) == ',') {
            stringBuffer.deleteCharAt(stringBuffer.length() - 1);
        }
        stringBuffer.append(")");
        return stringBuffer.toString();
    }

    @Override
    public long insert(T entity) {
        Map<String, String> map = getValues(entity);

        Iterator<Field> iterator = cacheMap.values().iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            field.setAccessible(true);

            try {
                Object object = field.get(entity);
                if (object == null) {
                    continue;
                }
                String value = object.toString();
                String key = "";
                if (field.getAnnotation(DbField.class) != null) {
                    key = field.getAnnotation(DbField.class).value();
                } else {
                    key = field.getName();
                }
                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        ContentValues contentValues = getContentValues(map);

       long ret = mSqLiteDatabase.insert(mTableName, null, contentValues);
        return ret;
    }


    private ContentValues getContentValues(Map<String, String> map) {
        ContentValues contentValues = new ContentValues();
        Set keys = map.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            String value = map.get(key);
            if (!TextUtils.isEmpty(value)) {
                contentValues.put(key, value);
            }
        }
        return contentValues;
    }

    private Map<String, String> getValues(T entity) {

        HashMap<String, String> map = new HashMap<>();
        Iterator<Field> iterator = cacheMap.values().iterator();
        while (iterator.hasNext()) {
            Field field = iterator.next();
            field.setAccessible(true);

            try {
                Object obj = field.get(entity);
                if (obj == null) {
                    continue;
                }

                String value = obj.toString();

                String key = "";
                if (field.getAnnotation(DbField.class) != null) {
                    key = field.getAnnotation(DbField.class).value();
                } else {
                    key = field.getName();
                }

                if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                    map.put(key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return map;

    }

    @Override
    public long update(T entity, T where) {
        int ret = -1;
        Map<String, String> map = getValues(entity);
        ContentValues contentValues = getContentValues(map);

        Map<String, String> whereMap = getValues(where);
        Condition condition = new Condition(whereMap);

        ret = mSqLiteDatabase.update(mTableName, contentValues, condition.whereClauseString, condition.whereArgs);
        return ret;
    }


    private class Condition {
        private String whereClauseString;
        private String[] whereArgs;

        public Condition(Map<String, String> whereClause) {
            Set<String> keys = whereClause.keySet();
            Iterator<String> iterator = keys.iterator();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append("1=1");
            List<String> argsList = new ArrayList<>();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = whereClause.get(key);
                if (value != null) {
                    stringBuffer.append(" and ");
                    stringBuffer.append(key + "=?");
                    argsList.add(value);
                }
            }
            whereClauseString = stringBuffer.toString();
            if (argsList.size() > 0){
                whereArgs =  argsList.toArray(new String[argsList.size()]);
            }

        }

    }

    @Override
    public int delete(T where) {
        int ret = -1;
        Map<String, String> whereMap = getValues(where);
        Condition condition = new Condition(whereMap);
        ret = mSqLiteDatabase.delete(mTableName, condition.whereClauseString, condition.whereArgs);
        return ret;
    }

    @Override
    public List query(T where) {
        return query(where,null,null,null);
    }

    @Override
    public List<T> query(T where, String orderBy, Integer startIndex, Integer limit) {

        Map<String, String> whereMap = getValues(where);
        Condition condition = new Condition(whereMap);
        String limitString = "";
        if(startIndex != null && limit !=null){
            limitString = startIndex+" , "+limit;
        }
        Cursor cursor = mSqLiteDatabase.query(mTableName, null, condition.whereClauseString,
                condition.whereArgs, null, orderBy, limitString);
        List<T> ret = getResult(cursor,where);
        return ret;
    }


    private List<T> getResult(Cursor cursor, T where){
        List<T> list = new ArrayList();
        Object item = null;
        while (cursor.moveToNext()){
            try {
                item = where.getClass().newInstance();
                Iterator<Map.Entry<String, Field>> iterator = cacheMap.entrySet().iterator();
                while (iterator.hasNext()){
                    Map.Entry<String, Field> next = iterator.next();
                    String columnName = next.getKey();
                    int columnIndex = cursor.getColumnIndex(columnName);
                    Field value = next.getValue();
                    Class<?> type = value.getType();
                    if(columnIndex != -1){
                        if(type == String.class){
                            value.set(item,cursor.getString(columnIndex));
                        }else if(type == Double.class){
                            value.set(item,cursor.getDouble(columnIndex));
                        }else if(type == Long.class){
                            value.set(item,cursor.getLong(columnIndex));
                        }else if(type == byte[].class){
                            value.set(item,cursor.getBlob(columnIndex));
                        }else if(type == Integer.class){
                            value.set(item,cursor.getInt(columnIndex));
                        }else {
                            continue;
                        }
                    }
                }
                list.add((T) item);
            } catch (IllegalAccessException e) {
                Log.e("BaseDao "," error:"+e.toString());
                e.printStackTrace();
            } catch (InstantiationException e) {
                Log.e("BaseDao "," error:"+e.toString());
                e.printStackTrace();
            }
        }
        return list;
    }
}
