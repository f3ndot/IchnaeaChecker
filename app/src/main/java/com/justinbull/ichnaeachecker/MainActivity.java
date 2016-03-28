/*
 * MIT License
 *
 * Copyright (c) 2016 Justin A. S. Bull, https://www.justinbull.ca
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.justinbull.ichnaeachecker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private List<GeneralCellInfo> mVisibleCells;
    private List<GeneralCellInfo> mRegisteredCells = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setCellInfo();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_DENIED) {
                    setCellInfo();
                }
                final Snackbar snack = Snackbar.make(view, "Checking Mozilla Location Services database...", Snackbar.LENGTH_INDEFINITE);
                snack.show();
                final RequestHandle handler = getIchnaeaLookup(snack);
                assert handler != null;
                snack.setAction("CANCEL", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        snack.setText("Cancelling lookup request...");
                        if (!handler.isFinished()) {
                            handler.cancel(true);
                        } else {
                            snack.setText("Request already finished!");
                            snack.setDuration(Snackbar.LENGTH_LONG);
                        }
                    }
                })
                        .show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == Consts.REQUEST_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setCellInfo();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void setCellInfo() {
        int tmPermCheck = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION);
        if (tmPermCheck == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1 && tm.getAllCellInfo() != null) {
                mVisibleCells = GeneralCellInfoFactory.getInstances(tm.getAllCellInfo());
                mRegisteredCells.clear();
                if (mVisibleCells.size() == 0) {
                    Log.w(TAG, "setCellInfo: No visible cells (primary or neighbours), unable to do anything");
                }
                for (GeneralCellInfo cell : mVisibleCells) {
                    Log.i(TAG, "Device aware of " + cell.toString());
                    if (cell.isRegistered()) {
                        mRegisteredCells.add(cell);
                    }
                }
            } else {
                Log.e(TAG, "setCellInfo: Android device too old to use getAllCellInfo(), need to implement getCellLocation() fallback!");
            }
        } else if (tmPermCheck == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(MainActivity.this, "Dude we need permissions", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    Consts.REQUEST_COARSE_LOCATION);
        } else {
            Log.wtf(TAG, "setCellInfo: Received unknown int from ContextCompat.checkSelfPermission()");
        }
    }

    public RequestHandle getIchnaeaLookup(final Snackbar snack) {
        if (mRegisteredCells.size() > 1) {
            Log.w(TAG, "getIchnaeaLookup: Registered to more than one cell, defaulting to strongest one");
            Collections.sort(mRegisteredCells, new Comparator<GeneralCellInfo>() {
                @Override
                public int compare(GeneralCellInfo lhs, GeneralCellInfo rhs) {
                    int lhsDbm = lhs.getDbmStrength();
                    int rhsDbm = rhs.getDbmStrength();
                    if (lhsDbm == rhsDbm) {
                        return 0;
                    }
                    return lhsDbm < rhsDbm ? -1 : 1;
                }
            });
        }
        final GeneralCellInfo cell = mRegisteredCells.get(0);
        Log.i(TAG, "getIchnaeaLookup: Looking up " + cell);
        return IchnaeaRestClient.geolocate(cell, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.i(TAG, "onSuccess: Cell in database! Location is: " + response.get("location") + " for " + cell);
                    Toast.makeText(MainActivity.this, "Cell in Ichnaea database!", Toast.LENGTH_LONG).show();
                    TextView text = (TextView) findViewById(R.id.topLevelText);
                    assert text != null;
                    text.setText("Cell in database!");
                    TextView textLat = (TextView) findViewById(R.id.latitudeText);
                    TextView textLng = (TextView) findViewById(R.id.longitudeText);
                    TextView textAcurracy = (TextView) findViewById(R.id.accuracyText);
                    assert textLat != null;
                    assert textLng != null;
                    assert textAcurracy != null;
                    textLat.setText("Latitude: " + Double.toString(response.getJSONObject("location").getDouble("lat")));
                    textLng.setText("Longitude: " + Double.toString(response.getJSONObject("location").getDouble("lng")));
                    textAcurracy.setText("Accuracy: " + Double.toString(response.getDouble("accuracy")) + " meters");
                    textLat.setVisibility(View.VISIBLE);
                    textLng.setVisibility(View.VISIBLE);
                    textAcurracy.setVisibility(View.VISIBLE);
                    snack.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                try {
                    if (statusCode == 404 && errorResponse.getJSONObject("error").getInt("code") == 404) {
                        Log.i(TAG, "onFailure: Cell is not in database: " + cell);
                        TextView text = (TextView) findViewById(R.id.topLevelText);
                        assert text != null;
                        text.setText("NOT IN DATABASE!");
                        TextView textLat = (TextView) findViewById(R.id.latitudeText);
                        TextView textLng = (TextView) findViewById(R.id.longitudeText);
                        TextView textAcurracy = (TextView) findViewById(R.id.accuracyText);
                        textLat.setVisibility(View.INVISIBLE);
                        textLng.setVisibility(View.INVISIBLE);
                        textAcurracy.setVisibility(View.INVISIBLE);
                    } else {
                        Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Unknown error occurred", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancel() {
                super.onCancel();
                snack.dismiss();
            }
        });
    }
}

