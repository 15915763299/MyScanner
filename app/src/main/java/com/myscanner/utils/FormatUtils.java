package com.myscanner.utils;

import net.sourceforge.zbar.Symbol;

public class FormatUtils {

    public static String getCodeName(int type) {
        switch (type) {
            case Symbol.NONE:
                return "No symbol decoded";
            case Symbol.PARTIAL:
                return "Symbol detected but not decoded";
            case Symbol.EAN8:
                return "EAN-8";
            case Symbol.UPCE:
                return "UPC-E";
            case Symbol.ISBN10:
                return "ISBN-10 (from EAN-13)";
            case Symbol.UPCA:
                return "UPC-A";
            case Symbol.EAN13:
                return "EAN-13";
            case Symbol.ISBN13:
                return "ISBN-13 (from EAN-13)";
            case Symbol.I25:
                return "Interleaved 2 of 5";
            case Symbol.DATABAR:
                return "DataBar (RSS-14)";
            case Symbol.DATABAR_EXP:
                return "DataBar Expanded";
            case Symbol.CODABAR:
                return "Codabar";
            case Symbol.CODE39:
                return "Code 39";
            case Symbol.PDF417:
                return "PDF417";
            case Symbol.QRCODE:
                return "QR Code";
            case Symbol.CODE93:
                return "Code 93";
            case Symbol.CODE128:
                return "Code 128";
            default:
                return "";
        }
    }
}
