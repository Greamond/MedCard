package com.example.medcard;

import static com.example.medcard.MainActivity.SqlSettings.getConnectionString;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class BdWork extends AsyncTask<String, Void, ResultSet> {

    MainActivity.iQuery iQuery;
    public BdWork(MainActivity.iQuery iQuery){
        this.iQuery = iQuery;
    }
    @Override
    protected ResultSet doInBackground(String... query) {

        JSONArray resultSet = new JSONArray();
        try {
            Connection con = DriverManager.getConnection(getConnectionString());

            if (con != null) {
                Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(query[0]+";Select SCOPE_IDENTITY()");
                return rs;
            }
        } catch (SQLException ex) {
            Log.w("SQLException error: ", ex.getMessage());
        } catch (Exception ex) {
            Log.w("Exception error: ", ex.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(ResultSet result) {
        iQuery.returner(result);
    }

}

