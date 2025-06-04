package com.example.todolist.Utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.todolist.Model.ToDoModel;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static final int VERSION = 3;
    private static final String NAME = "toDoListDatabase";
    private static final String TODO_TABLE ="todo";
    private static final String ID = "id";
    private static final String TASK = "task";
    private static final String STATUS = "status";
    private static final String CATEGORY = "category";
    private static final String PRIORITY = "priority";
    private static final String DUEDATE = "duedate";
    private static final String CREATE_TODO_TABLE = "CREATE TABLE " + TODO_TABLE + "("
            + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + TASK + " TEXT, "
            + CATEGORY + " TEXT, "
            + PRIORITY + " TEXT, "
            + DUEDATE + " TEXT, "
            + STATUS + " INTEGER)";
    private SQLiteDatabase db;

    public DatabaseHandler(Context context){
        super(context,NAME,null,VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL(CREATE_TODO_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        db.execSQL("DROP TABLE IF EXISTS " + TODO_TABLE);
        onCreate(db);
    }

    public void openDatabase(){
        db = this.getWritableDatabase();
    }

    public void insertTask(ToDoModel task){
        ContentValues cv = new ContentValues();
        cv.put(TASK, task.getTask());
        cv.put(STATUS,0);
        cv.put(CATEGORY, task.getCategory());
        cv.put(PRIORITY, task.getPriority());
        cv.put(DUEDATE, task.getDueDate());
        long result = db.insert(TODO_TABLE,null,cv);
        android.util.Log.d("DatabaseHandler", "Insert result: " + result);
    }



    @SuppressLint("Range")
    public List<ToDoModel> getAllTasks(){
        List<ToDoModel> taskList = new ArrayList<>();
        Cursor cur = null;
        db.beginTransaction();
        try {
            cur = db.query(TODO_TABLE, null, null, null, null, null, null);
            if (cur != null && cur.moveToFirst()) {
                do {
                    ToDoModel task = new ToDoModel();
                    task.setId(cur.getInt(cur.getColumnIndex(ID)));
                    task.setTask(cur.getString(cur.getColumnIndex(TASK)));
                    task.setStatus(cur.getInt(cur.getColumnIndex(STATUS))); // Corrected method call to setStatus
                    task.setCategory(cur.getString(cur.getColumnIndex(CATEGORY)));
                    task.setPriority(cur.getString(cur.getColumnIndex(PRIORITY)));
                    task.setDueDate(cur.getString(cur.getColumnIndex(DUEDATE)));
                    taskList.add(task);
                } while (cur.moveToNext());
            }
            db.setTransactionSuccessful();
        } finally {
            if (cur != null && !cur.isClosed()) { // Check if cursor is not null and not closed
                cur.close();
            }
            db.endTransaction();
        }
        return taskList;
    }


    public void updateStatus(int id, int status){
        ContentValues cv = new ContentValues();
        cv.put(STATUS,status);
        db.update(TODO_TABLE, cv,ID+ "=?", new String[] {String.valueOf(id)});
    }

    public void updateTask(ToDoModel task){
        ContentValues cv = new ContentValues();
        cv.put(TASK, task.getTask());
        cv.put(CATEGORY, task.getCategory());
        cv.put(PRIORITY, task.getPriority());
        cv.put(DUEDATE, task.getDueDate());
        cv.put(STATUS, task.getStatus());
        db.update(TODO_TABLE, cv, ID + "=?", new String[]{String.valueOf(task.getId())});    }
    public void deleteTask(int id){
        db.delete(TODO_TABLE, ID + "=?", new String[] {String.valueOf(id)});
    }

    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT DISTINCT " + CATEGORY + " FROM " + TODO_TABLE + " WHERE " + CATEGORY + " IS NOT NULL", null);
            if (cursor.moveToFirst()) {
                do {
                    String category = cursor.getString(0);
                    if (category != null && !category.trim().isEmpty()) {
                        categories.add(category);
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return categories;
    }

}
