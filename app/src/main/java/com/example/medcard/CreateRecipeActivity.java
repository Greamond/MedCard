package com.example.medcard;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CreateRecipeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recipe);
        findViewById(R.id.saveBtn).setOnClickListener(this::saveClick);
    }
    private void saveClick(View view){
        String name = ((EditText)findViewById(R.id.nameTv)).getText().toString();
        String Dosage = ((EditText)findViewById(R.id.dosageTv)).getText().toString();
        String Reception = ((EditText)findViewById(R.id.receptionTv)).getText().toString();

        BdWork task = new BdWork(new MainActivity.iQuery() {
            @Override
            public void returner(ResultSet result) {
                finish();
            }
        });
        task.execute("Insert into Recipe (Name, Dosage, Reception) values('"+name+"','"+ Dosage+"','"+Reception+"')");
    }
}