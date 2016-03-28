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

import android.support.annotation.NonNull;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.List;

public class GeneralCellInfoFactory {
    /**
     * Given a subclass of {@link CellInfo}, return a {@link GeneralCellInfo} representation, which
     * has a standard interface to access the various fields of differing cell network types (LTE,
     * GSM, WCDMA)
     *
     * @param cell Any CellInfo subclass, e.g. CellInfoLte
     * @return The GeneralCellInfo equivalent
     */
    public static GeneralCellInfo getInstance(CellInfo cell) {
        if (cell instanceof CellInfoLte) {
            return new GeneralCellInfo((CellInfoLte) cell);
        }
        if (cell instanceof CellInfoGsm) {
            return new GeneralCellInfo((CellInfoGsm) cell);
        }
        if (cell instanceof CellInfoWcdma) {
            return new GeneralCellInfo((CellInfoWcdma) cell);
        }
        if (cell instanceof CellInfoCdma) {
            return new GeneralCellInfo((CellInfoCdma) cell);
        }
        return null;
    }

    /**
     * Given a list of {@link CellInfo} implemented objects from {@link TelephonyManager#getAllCellInfo()}
     * return an equivalent list of {@link GeneralCellInfo} which has a standard interface to access
     * the various fields of differing cell network types (LTE, GSM, WCDMA)
     *
     * @param cells List of CellInfo-implementing objects
     * @return A list of GeneralCellInfo objects
     */
    public static List<GeneralCellInfo> getInstances(List<CellInfo> cells) {
        ArrayList<GeneralCellInfo> generalCells = new ArrayList<GeneralCellInfo>();
        for (CellInfo cell : cells) {
            generalCells.add(getInstance(cell));
        }
        return generalCells;
    }
}
