package com.example.medcard;

import static com.example.medcard.MainActivity.SqlSettings.getConnectionString;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    List<Patient> patientList = new ArrayList<>();
    Spinner spinner;
    List<Recipe> RecipeList = new ArrayList<>();
    List<Recipe> pickedRecips = new ArrayList<>();
    Spinner spinnerRecipe;
    AudioRecord recorder = null;
    byte[] micData = new byte[0];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = findViewById(R.id.tv);
        spinner = findViewById(R.id.spinnerV);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Patient patient = patientList.get(position);
                tv.setText(patient.Name);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        spinnerRecipe = findViewById(R.id.spinnerRecipe);
        spinnerRecipe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position==0 && pickedRecips.size()==0)
                    return;

                Recipe recipe = RecipeList.get(position);
                if (!pickedRecips.contains(recipe) && RecipeList.size()<10){
                    pickedRecips.add(recipe);
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText(recipe.Name);

                    LinearLayout holder = findViewById(R.id.reciptHolder);
                    holder.addView(tv);
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    public void goToRecipe(View view){
        Intent intent = new Intent(this,CreateRecipeActivity.class);
        startActivity(intent);
    }
    int bufferSize = 2048;
    boolean isRecording = false;
    public void startRecord(View view){

        if (recorder!=null){
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
        }else{
            int sampleRate = 8000;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

            int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                    channelConfig, audioFormat);

            int internalBufferSize = minInternalBufferSize * 4;
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, internalBufferSize);
            recorder.startRecording();
            isRecording = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[bufferSize];
                    List<Byte> bytes = new ArrayList<>();
                    int readed= 0;
                    while (isRecording){
                        readed = recorder.read(buffer,0,bufferSize);
                        for (int i = 0; i<readed; i++){
                            bytes.add(buffer[i]);
                        }
                    }
                    micData = new byte[bytes.size()];
                    for (int i = 0; i<bytes.size(); i++){
                        micData[i] = bytes.get(i);
                    }
                }
            }).start();
        }
    }


    public void saveMed(View view){
        String idMedCard = patientList.get(spinner.getSelectedItemPosition()).Id;

        String description = ((TextView)findViewById(R.id.simptomsTv)).getText().toString();
        String diagnosis = ((TextView)findViewById(R.id.diagnosisTv)).getText().toString();
        String recommendation = ((TextView)findViewById(R.id.recommendationsTv)).getText().toString();
        String direction = ((TextView)findViewById(R.id.directionTv)).getText().toString();
        String research = ((TextView)findViewById(R.id.researchTv)).getText().toString();
        String procedure = ((TextView)findViewById(R.id.procedureTv)).getText().toString();
        String simptoms = ((TextView)findViewById(R.id.simptomsTv)).getText().toString();



        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection con = DriverManager.getConnection(getConnectionString());
                    PreparedStatement statement = con.prepareStatement("Insert into IllnesHistories (Diagnos, Description, Simptoms, " +
                            "Recommendation, Direction, Research, Procedures, PatientMedicalCardNumber,[DateTime],VoicaData) " +
                            "values(?,?,?,?,?,?,?,?,?,?)");
                    statement.setString(1,diagnosis);
                    statement.setString(2,description);
                    statement.setString(3,simptoms);
                    statement.setString(4,recommendation);
                    statement.setString(5,direction);
                    statement.setString(6,research);
                    statement.setString(7,procedure);
                    statement.setString(8,idMedCard);
                    statement.setString(9,LocalDateTime.now().toString());
                    statement.setBytes(10,micData);
                    statement.executeUpdate();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

    }
    @Override
    protected void onResume() {
        super.onResume();

        RecipeList.clear();
        patientList.clear();

        BdWork task = new BdWork(new iQuery() {
            @Override
            public void returner(ResultSet result) {
                try {
                    while (result.next()) {
                        Patient patient = new Patient();
                        patient.Id = result.getString(1);
                        patient.Name = result.getString(2);
                        patient.Name += " " + result.getString(3);
                        patient.Name += " " + result.getString(4);
                        patientList.add(patient);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            List<String> strings = new ArrayList<>();
                            for (Patient p: patientList) {
                                strings.add(p.Id);
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,strings);
                            spinner.setAdapter(adapter);

                        }
                    });

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        task.execute("Select [MedicalCardNumber],[Name],[SecondName],[LastName] from Patients");

        task = new BdWork(new iQuery() {
            @Override
            public void returner(ResultSet result) {
                try {
                    while (result.next()) {
                        Recipe recipe = new Recipe();
                        recipe.Id = result.getInt(1);
                        recipe.Name = result.getString(2);
                        recipe.Dosage = result.getString(3);
                        recipe.Reception = result.getString(4);

                        RecipeList.add(recipe);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            List<String> strings = new ArrayList<>();
                            for (Recipe p: RecipeList) {
                                strings.add(p.Name);
                            }
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,strings);
                            spinnerRecipe.setAdapter(adapter);

                        }
                    });

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        task.execute("Select [Id],[Name],[Dosage],[Reception] from Recipe");

    }

    public interface iQuery{
        void returner(ResultSet result);
    }
    public class Recipe{
        public int Id;
        public String Name;
        public String Dosage;
        public String Reception;
    }
    private class Patient{
        public String Id;
        public String Name;
    }
    public static class SqlSettings{
        static String instanceName = "student.permaviat.ru";
        static String db = "base2_ISP_21_2_23";
        static String username = "ISP_21_2_23";
        static String password = "3frQxZ83o#";
        static String connectionUrl = "jdbc:jtds:sqlserver://%1$s;databaseName=%2$s;user=%3$s;password=%4$s;Encrypt=false;trusted_connection=false";

        public static String  getConnectionString   (){
            return String.format(connectionUrl, instanceName, db, username, password);
        }
    }
}