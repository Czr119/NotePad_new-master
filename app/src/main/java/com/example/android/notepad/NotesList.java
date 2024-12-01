package com.example.android.notepad;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,                   // 0
            NotePad.Notes.COLUMN_NAME_TITLE,     // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE // 2
    };


    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;

    /**
     * 遍历视图树并设置所有 TextView 的字体颜色
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 恢复背景颜色
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        int savedColor = sharedPreferences.getInt("background_color", Color.WHITE); // 默认背景颜色为白色
        View rootView = findViewById(android.R.id.content);
        rootView.setBackgroundColor(savedColor);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         *
         * Please see the introductory note about performing provider operations on the UI thread.
         */
        Cursor cursor = managedQuery(
            getIntent().getData(),            // Use the default content URI for the provider.
            PROJECTION,                       // Return the note ID and title for each note.
            null,                             // No where clause, return all records.
            null,                             // No where clause, therefore no where column values.
            NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );

        /*
         * The following two arrays create a "map" between columns in the cursor and view IDs
         * for items in the ListView. Each element in the dataColumns array represents
         * a column name; each element in the viewID array represents the ID of a View.
         * The SimpleCursorAdapter maps them in ascending order to determine where each column
         * value will appear in the ListView.
         */

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.text2
        };


        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,                             // The Context for the ListView
                R.layout.noteslist_item,          // Points to the XML for a list item
                cursor,                           // The cursor to get items from
                dataColumns,
                viewIDs
        );

// 设置 ViewBinder 格式化时间戳
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text2) {
                    long timestamp = cursor.getLong(columnIndex);

                    // 设置为北京时间
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                    sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));

                    String formattedDate = sdf.format(new java.util.Date(timestamp));
                    ((TextView) view).setText(formattedDate);
                    return true;
                }
                return false;
            }
        });


        setListAdapter(adapter);


        // Sets the ListView's adapter to be the cursor adapter that was just created.
        setListAdapter(adapter);
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * Sets up a menu that provides the Insert option plus a list of alternative actions for
     * this Activity. Other applications that want to handle notes can "register" themselves in
     * Android by providing an intent filter that includes the category ALTERNATIVE and the
     * mimeTYpe NotePad.Notes.CONTENT_TYPE. If they do this, the code in onCreateOptionsMenu()
     * will add the Activity that contains the intent filter to its list of options. In effect,
     * the menu will offer the user other applications that can handle notes.
     * @param menu A Menu object, to which menu items should be added.
     * @return True, always. The menu should be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        // Generate any additional actions that can be performed on the
        // overall list.  In a normal install, there are no additional
        // actions found here, but this allows other applications to extend
        // our menu with their own actions.
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);


        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection.  This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                Menu.NONE,                  // A unique item ID is not required.
                Menu.NONE,                  // The alternatives don't need to be in order.
                null,                       // The caller's name is not excluded from the group.
                specifics,                  // These specific options must appear first.
                intent,                     // These Intent objects map to the options in specifics.
                Menu.NONE,                  // No flags are required.
                items                       // The menu items generated from the specifics-to-
                                            // Intents mapping
            );
                // If the Edit menu item exists, adds shortcuts for it.
                if (items[0] != null) {

                    // Sets the Edit menu item shortcut to numeric "1", letter "e"
                    items[0].setShortcut('1', 'e');
                }
            } else {
                // If the list is empty, removes any existing alternative actions from the menu
                menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
            }

        // Displays the menu
        return true;
    }

    private void showColorPicker() {
        // 定义预设颜色及名称（移除黑色背景颜色）
        final int[] colors = {
                Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
                Color.WHITE, Color.GRAY, Color.CYAN, Color.MAGENTA
        };

        final String[] colorNames = {
                "Red", "Green", "Blue", "Yellow",
                "White", "Gray", "Cyan", "Magenta"
        };

        // 创建一个简单的 ListView 作为选择界面
        ListView listView = new ListView(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, colorNames);
        listView.setAdapter(adapter);

        // 设置点击事件来改变背景颜色
        listView.setOnItemClickListener((parent, view, position, id) -> {
            int selectedColor = colors[position];

            // 更改背景颜色
            View rootView = findViewById(android.R.id.content);
            rootView.setBackgroundColor(selectedColor);

            // 保存用户选择的颜色到 SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("background_color", selectedColor);
            editor.apply();

            // 提示用户已更改
            Toast.makeText(this, "Background color changed to " + colorNames[position], Toast.LENGTH_SHORT).show();
        });

        // 创建一个 AlertDialog 展示列表
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Background Color")
                .setView(listView)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;

            case R.id.menu_paste:
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;

            case R.id.menu_search:
                showSearchDialog();
                return true;

            case R.id.menu_home:
                // 返回原始的笔记列表
                Cursor cursor = managedQuery(
                        NotePad.Notes.CONTENT_URI,
                        PROJECTION,
                        null,
                        null,
                        NotePad.Notes.DEFAULT_SORT_ORDER
                );
                ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
                return true;

            case R.id.menu_change_color:
                showColorPicker();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showSearchDialog() {
        // 创建一个输入框
        final EditText input = new EditText(this);
        input.setHint("Enter search query");

        // 使用 android.app.AlertDialog 创建对话框
        new android.app.AlertDialog.Builder(this)
                .setTitle("Search Notes")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {
                    String query = input.getText().toString().trim(); // 获取输入的查询关键字

                    // 检查输入是为空
                    if (query.isEmpty()) {
                        Toast.makeText(this, "Search query cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 使用 rawQuery 执行查询
                    String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                            NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";

                    // 参数绑定
                    String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};

                    // 查询数据库
                    Cursor cursor = getContentResolver().query(
                            NotePad.Notes.CONTENT_URI,       // 查询的 URI
                            PROJECTION,                      // 查询的列
                            selection,                       // 查询条件
                            selectionArgs,                   // 查询参数
                            NotePad.Notes.DEFAULT_SORT_ORDER // 排序
                    );

                    if (cursor != null && cursor.getCount() > 0) {
                        // 新列表视图
                        ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
                    } else {
                        Toast.makeText(this, "No notes found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    /**
     * This method is called when the user context-clicks a note in the list. NotesList registers
     * itself as the handler for context menus in its ListView (this is done in onCreate()).
     *
     * The only available options are COPY and DELETE.
     *
     * Context-click is equivalent to long-press.
     *
     * @param menu A ContexMenu object to which items should be added.
     * @param view The View for which the context menu is being constructed.
     * @param menuInfo Data associated with view.
     * @throws ClassCastException
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {

        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        // Tries to get the position of the item in the ListView that was long-pressed.
        try {
            // Casts the incoming data object into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            // If the menu object can't be cast, logs an error.
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        /*
         * Gets the data associated with the item at the selected position. getItem() returns
         * whatever the backing adapter of the ListView has associated with the item. In NotesList,
         * the adapter associated all of the data for a note with its list item. As a result,
         * getItem() returns that data as a Cursor.
         */
        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        // If the cursor is empty, then for some reason the adapter can't get the data from the
        // provider, so returns null to the caller.
        if (cursor == null) {
            // For some reason the requested item isn't available, do nothing
            return;
        }

        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        // Sets the menu header to be the title of the selected note.
        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        // Append to the
        // menu items for any other activities that can do stuff with it
        // as well.  This does a query on the system for any activities that
        // implement the ALTERNATIVE_ACTION for our data, adding a menu item
        // for each one that is found.
        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                                        Integer.toString((int) info.id) ));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    /**
     * This method is called when the user selects an item from the context menu
     * (see onCreateContextMenu()). The only menu items that are actually handled are DELETE and
     * COPY. Anything else is an alternative option, for which default handling should be done.
     *
     * @param item The selected menu item
     * @return True if the menu item was DELETE, and no default processing is need, otherwise false,
     * which triggers the default handling of the item.
     * @throws ClassCastException
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // The data from the menu item.
        AdapterView.AdapterContextMenuInfo info;

        /*
         * Gets the extra info from the menu item. When an note in the Notes list is long-pressed, a
         * context menu appears. The menu items for the menu automatically get the data
         * associated with the note that was long-pressed. The data comes from the provider that
         * backs the list.
         *
         * The note's data is passed to the context menu creation routine in a ContextMenuInfo
         * object.
         *
         * When one of the context menu items is clicked, the same data is passed, along with the
         * note ID, to onContextItemSelected() via the item parameter.
         */
        try {
            // Casts the data object in the item into the type for AdapterView objects.
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {

            // If the object can't be cast, logs an error
            Log.e(TAG, "bad menuInfo", e);

            // Triggers default processing of the menu item.
            return false;
        }
        // Appends the selected note's ID to the URI sent with the incoming Intent.
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        /*
         * Gets the menu item's ID and compares it to known actions.
         */
        switch (item.getItemId()) {
        case R.id.context_open:
            // Launch activity to view/edit the currently selected item
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
//BEGIN_INCLUDE(copy)
        case R.id.context_copy:
            // Gets a handle to the clipboard service.
            ClipboardManager clipboard = (ClipboardManager)
                    getSystemService(Context.CLIPBOARD_SERVICE);

            // Copies the notes URI to the clipboard. In effect, this copies the note itself
            clipboard.setPrimaryClip(ClipData.newUri(   // new clipboard item holding a URI
                    getContentResolver(),               // resolver to retrieve URI info
                    "Note",                             // label for the clip
                    noteUri)                            // the URI
            );

            // Returns to the caller and skips further processing.
            return true;
//END_INCLUDE(copy)
        case R.id.context_delete:
            getContentResolver().delete(
                noteUri,
                null,
                null
            );
            return true;
        case R.id.context_export:
            exportNote(info.id);
            return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

    /**
     * This method is called when the user clicks a note in the displayed list.
     *
     * This method handles incoming actions of either PICK (get data from the provider) or
     * GET_CONTENT (get or create data). If the incoming action is EDIT, this method sends a
     * new Intent to start NoteEditor.
     * @param l The ListView that contains the clicked item
     * @param v The View of the individual item
     * @param position The position of v in the displayed list
     * @param id The row ID of the clicked item
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {

        // Constructs a new URI from the incoming URI and the row ID
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);

        // Gets the action from the incoming Intent
        String action = getIntent().getAction();

        // Handles requests for note data
        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {

            // Sets the result to return to the component that called this Activity. The
            // result contains the new URI
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {

            // Sends out an Intent to start an Activity that can handle ACTION_EDIT. The
            // Intent's data is the note ID URI. The effect is to call NoteEdit.
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

    private void exportNote(long noteId) {
        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), noteId);
        Cursor cursor = getContentResolver().query(
            noteUri,
            new String[] { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE },
            null,
            null,
            null
        );

        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE));
            String content = cursor.getString(cursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE));
            cursor.close();

            try {
                // 获取外部存储目录
                File notesFolder = new File(Environment.getExternalStorageDirectory(), "NotePad");
                if (!notesFolder.exists()) {
                    notesFolder.mkdirs();
                }

                // 创建文件
                String fileName = title.replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
                File file = new File(notesFolder, fileName);

                // 写入内容
                FileWriter writer = new FileWriter(file);
                writer.write(title + "\n\n");
                writer.write(content);
                writer.close();

                Toast.makeText(this, "已导出到: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Toast.makeText(this, "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

}
