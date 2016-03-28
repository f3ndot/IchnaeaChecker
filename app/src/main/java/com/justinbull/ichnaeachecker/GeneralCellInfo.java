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

    protected String mCellType; // LTE, GSM, WCDMA, or CDMA
    protected boolean mIsRegistered;
    protected int mCellIdentity; // aka CID (GSM, WCDMA) or CI (LTE)
    protected int mMobileCountryCode;
    protected int mMobileNetworkCode;
    protected int mScramblingCode; // aka PSC (GSM, WCDMA) or PCI (LTE)
    protected int mAreaCode; // aka LAC (GSM, WCDMA) or TAC (LTE)
    protected CellSignalStrength mStrength;

    public GeneralCellInfo(String cellType, boolean isRegistered, int cellIdentity, int mobileCountryCode, int mobileNetworkCode, int scramblingCode, int areaCode, CellSignalStrength strength) {
        mCellType = cellType;
        mIsRegistered = isRegistered;
        mCellIdentity = cellIdentity;
        mMobileCountryCode = mobileCountryCode;
        mMobileNetworkCode = mobileNetworkCode;
        mScramblingCode = scramblingCode;
        mAreaCode = areaCode;
        mStrength = strength;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public GeneralCellInfo(CellInfoLte cell) {
        CellIdentityLte identity = cell.getCellIdentity();
        mCellType = NETWORK_TYPE_LTE;
        mIsRegistered = cell.isRegistered();
        mCellIdentity = identity.getCi();
        mMobileCountryCode = identity.getMcc();
        mMobileNetworkCode = identity.getMnc();
        mScramblingCode = identity.getPci();
        mAreaCode = identity.getTac();
        mStrength = cell.getCellSignalStrength();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public GeneralCellInfo(CellInfoGsm cell) {
        CellIdentityGsm identity = cell.getCellIdentity();
        mCellType = NETWORK_TYPE_GSM;
        mIsRegistered = cell.isRegistered();
        mCellIdentity = identity.getCid();
        mMobileCountryCode = identity.getMcc();
        mMobileNetworkCode = identity.getMnc();
        mScramblingCode = Integer.MAX_VALUE; // GSM doesn't have scrambling codes
        mAreaCode = identity.getLac();
        mStrength = cell.getCellSignalStrength();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public GeneralCellInfo(CellInfoWcdma cell) {
        CellIdentityWcdma identity = cell.getCellIdentity();
        mCellType = NETWORK_TYPE_WCDMA;
        mIsRegistered = cell.isRegistered();
        mCellIdentity = identity.getCid();
        mMobileCountryCode = identity.getMcc();
        mMobileNetworkCode = identity.getMnc();
        mScramblingCode = identity.getPsc();
        mAreaCode = identity.getLac();
        mStrength = cell.getCellSignalStrength();
    }

    public GeneralCellInfo(CellInfoCdma cell) {
        throw new UnsupportedOperationException("CDMA is not supported yet");
    }

    public String getCellType() {
        return mCellType;
    }

    public boolean isRegistered() {
        return mIsRegistered;
    }

    public int getCellIdentity() {
        return mCellIdentity;
    }

    public boolean isIdentityKnown() {
        return getCellIdentity() != Integer.MAX_VALUE;
    }

    public int getMobileCountryCode() {
        return mMobileCountryCode;
    }

    public boolean isMCCKnown() {
        return getMobileCountryCode() != Integer.MAX_VALUE;
    }

    public int getMobileNetworkCode() {
        return mMobileNetworkCode;
    }

    public boolean isMNCKnown() {
        return getMobileNetworkCode() != Integer.MAX_VALUE;
    }

    public int getScramblingCode() {
        return mScramblingCode;
    }

    public boolean isScramblingCodeKnown() {
        return hasScramblingCode();
    }

    public int getAreaCode() {
        return mAreaCode;
    }

    public boolean isAreaCodeKnown() {
        return getAreaCode() != Integer.MAX_VALUE;
    }


    /**
     * GSM networks don't have a Primary Scrambling Code (PSC) even though standards describe it.
     * Also, a WCMDA cell's PSC *may* be unknown.
     *
     * @return false if there is no PSC
     */
    public boolean hasScramblingCode() {
        return mScramblingCode == Integer.MAX_VALUE;
    }

    public CellSignalStrength getStrength() {
        return mStrength;
    }

    public boolean isStrengthKnown() {
        return getDbmStrength() != Integer.MAX_VALUE;
    }

    public int getDbmStrength() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (mStrength == null) {
                return Integer.MAX_VALUE;
            }
            return mStrength.getDbm();
        }
        Log.e(TAG, "getDbmStrength: Unable to get Dbm strength because SDK too low");
        return Integer.MAX_VALUE;
    }

    @Override
    public String toString() {
        final String unknown = "(UNKNOWN)";
        StringBuffer sb = new StringBuffer();
        sb.append("Cell:{");
        sb.append("isRegistered=").append(Boolean.toString(isRegistered())).append(" ");
        sb.append("Type=").append(mCellType).append(" ");
        sb.append("CI/CID=").append(isIdentityKnown() ? mCellIdentity : unknown).append(" ");
        sb.append("MCC=").append(isMCCKnown() ? mMobileCountryCode : unknown).append(" ");
        sb.append("MNC=").append(isMNCKnown() ? mMobileNetworkCode: unknown).append(" ");
        sb.append("PSC/PCI=").append(isScramblingCodeKnown() ? mScramblingCode : unknown).append(" ");
        sb.append("LAC/TAC=").append(isAreaCodeKnown() ? mAreaCode: unknown).append(" ");
        sb.append("Dbm=").append(isStrengthKnown() ? getDbmStrength() : unknown);
        sb.append("}");
        return sb.toString();
    }
}
