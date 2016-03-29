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

import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

/**
 * Assumes JSON payloads and requests.
 *
 * Created by justinbull on 2016-03-27.
 */
public class IchnaeaRestClient {
    private static final String TAG = "IchnaeaRestClient";

    private static final String BASE_URL = "https://location.services.mozilla.com/";
    private static final String API_VERSION = "v1";
    private static final String API_KEY = "test"; // Official test API key for Ichnaea

    private static AsyncHttpClient client = new AsyncHttpClient();

    /**
     * Determine if cell is in Mozilla Location Service (MLS/Ichnaea) database
     *
     * @see #geolocate(String, int, int, int, int, int, AsyncHttpResponseHandler)
     * @param cell
     * @return
     */
    public static RequestHandle geolocate(GeneralCellInfo cell, AsyncHttpResponseHandler responseHandler) {
        if (!cell.isFullyKnown()) {
            throw new IllegalArgumentException("Insufficient cell information for Ichnaea lookup");
        }
        return geolocate(cell.getCellType(), cell.getMobileCountryCode(),
                cell.getMobileNetworkCode(), cell.getAreaCode(), cell.getCellIdentity(),
                cell.getDbmStrength(), responseHandler);
    }

    /**
     * A partially implemented call to Geolocate API.
     *
     * Attempts to find the lat/lng location
     * @param radioType
     * @param mcc
     * @param mnc
     * @param lac
     * @param cellId
     * @param signalStrength
     */
    @Nullable
    public static RequestHandle geolocate(String radioType, int mcc, int mnc, int lac, int cellId, int signalStrength, AsyncHttpResponseHandler responseHandler) {
        try {
            JSONObject fallbacks = new JSONObject();
            fallbacks.put("ipf", false);
            fallbacks.put("lacf", false);

            JSONObject cellTower = new JSONObject();
            cellTower.put("radioType", radioType.toLowerCase());
            cellTower.put("mobileCountryCode", mcc);
            cellTower.put("mobileNetworkCode", mnc);
            cellTower.put("locationAreaCode", lac);
            cellTower.put("cellId", cellId);
            cellTower.put("signalStrength", signalStrength);

            // Final request JSON object
            JSONObject jsonParams = new JSONObject();
            jsonParams.put("fallbacks", fallbacks);

            JSONArray towers = new JSONArray();
            towers.put(cellTower);
            jsonParams.put("cellTowers", towers);

            return jsonPost("geolocate", jsonParams, responseHandler);
        } catch (JSONException e) {
            Log.e(TAG, "geolocate: Unable to assemble JSON payload. Silently failing");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Make a HTTP POST request to a URL with optional parameters
     * @param url URL to make to POST request to
     * @param payload The POST parameters
     * @param responseHandler The callback object that will handle request results
     */
    private static RequestHandle jsonPost(String url, JSONObject payload, AsyncHttpResponseHandler responseHandler) {
        try {
            StringEntity entity = new StringEntity(payload.toString());
            entity.setContentType("application/json");
            return client.post(null, getAbsoluteUrl(url), entity, "application/json", responseHandler);
        } catch (UnsupportedEncodingException e) {
            // TODO deal with it
            Log.e(TAG, "jsonPost: Unable to serialize JSON payload. Silently failing");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Combine the relative URL with API's base URL and version
     * @param relativeUrl
     * @return String of full URL
     */
    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + API_VERSION + "/" + relativeUrl + "?key=" + API_KEY;
    }
}
