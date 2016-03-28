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

import android.annotation.TargetApi;
import android.os.Build;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellIdentityWcdma;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrength;
import android.util.Log;

/**
 * Because jerks in standards bodies like to rename shit arbitrarily whe they essentially perform
 * the same function, we have this class.
 *
 * And depending on the device type and the API it uses, we may have to populate this cell
 * information from source that are unrelated to android.telephony.CellInfo.
 *
 * A class to act as a common interface so the app can get information agnostic to the cell network
 * type.
 *
 */
public class GeneralCellInfo {
    private static final String TAG = "GeneralCellInfo";

    public static final String NETWORK_TYPE_LTE = "LTE";
    public static final String NETWORK_TYPE_GSM = "GSM";
    public static final String NETWORK_TYPE_WCDMA = "WCDMA";
    public static final String NETWORK_TYPE_CDMA = "CDMA";

    public String cellType; // LTE, GSM, WCDMA, or CDMA
    public int cellIdentity; // aka CID (GSM, WCDMA) or CI (LTE)
    public int mobileCountryCode;
    public int mobileNetworkCode;
    public int scramblingCode; // aka PSC (GSM, WCDMA) or PCI (LTE)
    public int areaCode; // aka LAC (GSM, WCDMA) or TAC (LTE)
    public CellSignalStrength strength;

    public GeneralCellInfo(String cellType, int cellIdentity, int mobileCountryCode, int mobileNetworkCode, int scramblingCode, int areaCode, CellSignalStrength strength) {
        this.cellType = cellType;
        this.cellIdentity = cellIdentity;
        this.mobileCountryCode = mobileCountryCode;
        this.mobileNetworkCode = mobileNetworkCode;
        this.scramblingCode = scramblingCode;
        this.areaCode = areaCode;
        this.strength = strength;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public GeneralCellInfo(CellInfoLte cell) {
        CellIdentityLte identity = cell.getCellIdentity();
        cellType = NETWORK_TYPE_LTE;
        cellIdentity = identity.getCi();
        mobileCountryCode = identity.getMcc();
        mobileNetworkCode = identity.getMnc();
        scramblingCode = identity.getPci();
        areaCode = identity.getTac();
        strength = cell.getCellSignalStrength();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public GeneralCellInfo(CellInfoGsm cell) {
        CellIdentityGsm identity = cell.getCellIdentity();
        cellType = NETWORK_TYPE_GSM;
        cellIdentity = identity.getCid();
        mobileCountryCode = identity.getMcc();
        mobileNetworkCode = identity.getMnc();
        scramblingCode = Integer.MAX_VALUE; // GSM doesn't have scrambling codes
        areaCode = identity.getLac();
        strength = cell.getCellSignalStrength();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GeneralCellInfo(CellInfoWcdma cell) {
        CellIdentityWcdma identity = cell.getCellIdentity();
        cellType = NETWORK_TYPE_WCDMA;
        cellIdentity = identity.getCid();
        mobileCountryCode = identity.getMcc();
        mobileNetworkCode = identity.getMnc();
        scramblingCode = identity.getPsc();
        areaCode = identity.getLac();
        strength = cell.getCellSignalStrength();
    }

    public GeneralCellInfo(CellInfoCdma cell) {
        throw new UnsupportedOperationException("CDMA is not supported yet");
    }

    /**
     * GSM networks don't have a Primary Scrambling Code (PSC) even though standards describe it.
     * Also, a WCMDA cell's PSC *may* be unknown.
     *
     * @return false if there is no PSC
     */
    public boolean hasScramblingCode() {
        return scramblingCode == Integer.MAX_VALUE;
    }

    public int getDbmStrength() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (strength == null) {
                return Integer.MAX_VALUE;
            }
            return strength.getDbm();
        }
        Log.e(TAG, "getDbmStrength: Unable to get Dbm strength because SDK too low");
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Cell:{");
        sb.append("Type=").append(cellType).append(" ");
        sb.append("CI/CID=").append(cellIdentity).append(" ");
        sb.append("MCC=").append(mobileCountryCode).append(" ");
        sb.append("MNC=").append(mobileNetworkCode).append(" ");
        sb.append("PSC/PCI=").append(scramblingCode).append(" ");
        sb.append("LAC/TAC=").append(areaCode).append(" ");
        sb.append("Dbm=").append(getDbmStrength());
        sb.append("}");
        return sb.toString();
    }
}
