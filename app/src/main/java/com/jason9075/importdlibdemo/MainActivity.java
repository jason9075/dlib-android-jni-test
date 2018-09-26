package com.jason9075.importdlibdemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {

    private AdapterView.OnItemClickListener onClickListView = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (position) {
                case 0:
                    startActivity(
                        new Intent(MainActivity.this,PhotoDetectActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
                case 1:
                    startActivity(
                        new Intent(MainActivity.this,CameraDetectActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.listview);

        String[] items = {"Photo Detect", "Camera Detect"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(onClickListView);

    }


}