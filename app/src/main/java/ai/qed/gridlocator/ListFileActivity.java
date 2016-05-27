package ai.qed.gridlocator;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by admin on 10/16/15.
 */
public class ListFileActivity extends ListActivity {

    private static final String EXTRA_PATH = "path";
    public static final String EXTRA_RESULT = "result";

    private List<String> pathHistory = new ArrayList<>();

    private String path;

    public static final Intent makeIntent(Context context, String path) {
        Intent intent = new Intent(context, ListFileActivity.class);
        intent.putExtra(EXTRA_PATH, path);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_files);

        if (getIntent().hasExtra(EXTRA_PATH)) {
            loadPath(getIntent().getStringExtra(EXTRA_PATH), true);
        }
        else {
            loadPath("/", true);
        }
    }

    private void loadPath(String directory, boolean add) {
        // Use the current directory as title
        path = directory;

        if (add) {
            pathHistory.add(path);
        }

        // Read all files sorted into the values-array
        List values = new ArrayList();
        File dir = new File(path);
        if (!dir.canRead()) {
            setTitle(getTitle() + " (inaccessible)");
        }
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                if (!file.startsWith(".")) {
                    values.add(file);
                }
            }
        }
        Collections.sort(values);

        // Put the data into the list
        ArrayAdapter adapter = new ArrayAdapter(this,
                R.layout.file_item, R.id.text1, values);
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String filename = (String) getListAdapter().getItem(position);
        if (path.endsWith(File.separator)) {
            filename = path + filename;
        } else {
            filename = path + File.separator + filename;
        }
        if (new File(filename).isDirectory()) {
            loadPath(filename, true);
        } else {
            Intent data = new Intent();
            data.putExtra(EXTRA_RESULT, filename);
            setResult(RESULT_OK, data);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (pathHistory.size() == 1) {
            super.onBackPressed();
        }
        else {
            pathHistory.remove(pathHistory.size()-1);
            loadPath(pathHistory.get(pathHistory.size()-1), false);
        }
    }
}