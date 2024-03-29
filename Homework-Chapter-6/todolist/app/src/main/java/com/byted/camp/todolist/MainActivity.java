package com.byted.camp.todolist;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.byted.camp.todolist.beans.Note;
import com.byted.camp.todolist.beans.Priority;
import com.byted.camp.todolist.beans.State;
import com.byted.camp.todolist.db.TodoContract.TodoNote;
import com.byted.camp.todolist.db.TodoDbHelper;
import com.byted.camp.todolist.debug.DebugActivity;
import com.byted.camp.todolist.debug.FileDemoActivity;
import com.byted.camp.todolist.debug.SpDemoActivity;
import com.byted.camp.todolist.ui.NoteListAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    private TodoDbHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        dbHelper = new TodoDbHelper(this);
        database = dbHelper.getWritableDatabase();

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                // TODO: 2021/7/19 7. 此处删除数据库数据
                if(database == null){
                    Toast.makeText(MainActivity.this, "database is empty", Toast.LENGTH_SHORT);
                    return;
                }
                String selection = TodoNote._ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(note.id)};
                int deleteRows = database.delete(TodoNote.Name_Table, selection, selectionArgs);
                if(deleteRows > 0) notesAdapter.refresh(loadNotesFromDatabase());
            }

            @Override
            public void updateNote(Note note) {
                // TODO: 2021/7/19 7. 此处更新数据库数据
                if(database == null){
                    Toast.makeText(MainActivity.this, "database is empty", Toast.LENGTH_SHORT);
                    return;
                }
                ContentValues values = new ContentValues();
                values.put(TodoNote.State_Attribute, note.getState().intValue);
                String selection = TodoNote._ID + " = ?";
                String[] selectionArgs = new String[]{String.valueOf(note.id)};

                int count = database.update(
                        TodoNote.Name_Table,
                        values,
                        selection,
                        selectionArgs
                );
                if(count > 0) notesAdapter.refresh(loadNotesFromDatabase());
            }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
        database = null;
        dbHelper.close();
        dbHelper = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            case R.id.action_file:
                startActivity(new Intent(this, FileDemoActivity.class));
                return true;
            case R.id.action_sp:
                startActivity(new Intent(this, SpDemoActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        if (database == null) {
            return Collections.emptyList();
        }
        List<Note> result = new LinkedList<>();
        // TODO: 2021/7/19 7. 此处query数据库数据
        Cursor cursor = null;
        try{
            cursor = database.query(TodoNote.Name_Table,
                    null,
                    null,
                    null,
                    null,
                    null,
                    TodoNote.Date_Attribute + " DESC");
            while(cursor.moveToNext()){
                int id = cursor.getInt(cursor.getColumnIndex(TodoNote._ID));
                int state = cursor.getInt(cursor.getColumnIndex(TodoNote.State_Attribute));
                int date = cursor.getInt(cursor.getColumnIndex(TodoNote.Date_Attribute));
                int priority = cursor.getInt(cursor.getColumnIndex(TodoNote.Priority_Attribute));
                String content = cursor.getString(cursor.getColumnIndex(TodoNote.Content_Attribute));
                Note note = new Note(id);
                note.setContent(content);
                note.setDate(new Date(date));
                note.setPriority(Priority.from(priority));
                note.setState(State.from(state));
                result.add(note);
            }
        } finally {
            if(cursor != null) cursor.close();
        }
        return result;
    }
}
