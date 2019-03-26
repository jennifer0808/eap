/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.util.tool;

import java.util.ArrayList;

/**
 *
 * @author njtz
 */
public class ByteTool {
    /*
     * hx2dc
     */

    public static void hx2dec(String str) {
        String[] chars = str.split(" ");
        for (int j = 0; j < chars.length; j++) {
            System.out.print(Integer.parseInt(chars[j], 16));
        }
    }
    /*
     * string ascii2string
     */

    public static void ascii2string(String s) {
        String[] chars = s.split(" ");
        for (int j = 0; j < chars.length; j++) {
//            if (Integer.parseInt(chars[j]) < 0) {
//                continue;
//            }          
            System.out.print((char) Integer.parseInt(chars[j]));
        }
    }

    public static void getNameLength(String str) {
        String[] st = str.split(",");
        for (int i = 0; i < st.length; i++) {
            System.out.println(st[i]);
        }
    }
    /*
     * arraylist ascii2string
     */

    public static String asc2string(ArrayList s) {
        String str = null;
        for (int i = 0; i < s.size(); i++) {
            String tmp = (String) s.get(i);
            str = str + (char) Integer.parseInt(tmp);
        }
        //str = str.replaceAll(" ", "");
        return str;
    }
    /*
     * hexString2binaryString
     */

    public static String hexString2binaryString(String hexString) {
        hexString = hexString.replaceAll(" ", "");
        if (hexString == null || hexString.length() % 2 != 0) {
            return null;
        }
        String bString = "", tmp;
        for (int i = 0; i < hexString.length(); i++) {
            tmp = "0000"
                    + Integer.toBinaryString(Integer.parseInt(hexString.substring(i, i + 1), 16));
            bString += tmp.substring(tmp.length() - 4);
        }
        return bString;
    }
    // 转化十六进制编码为字符串 

    public static String toStringHex(String s) {

        String s1 = "Big5,Big5-HKSCS,EUC-JP,EUC-KR,GB18030,GB2312,GBK,IBM-Thai,IBM00858,IBM01140,IBM01141,IBM01142,IBM01143,IBM01144,IBM01145,IBM01146,IBM01147,IBM01148,IBM01149,IBM037,IBM1026,IBM1047,IBM273,IBM277,IBM278,IBM280,IBM284,IBM285,IBM297,IBM420,IBM424,IBM437,IBM500,IBM775,IBM850,IBM852,IBM855,IBM857,IBM860,IBM861,IBM862,IBM863,IBM864,IBM865,IBM866,IBM868,IBM869,IBM870,IBM871,IBM918,ISO-2022-CN,ISO-2022-JP,ISO-2022-JP-2,ISO-2022-KR,ISO-8859-1,ISO-8859-13,ISO-8859-15,ISO-8859-2,ISO-8859-3,ISO-8859-4,ISO-8859-5,ISO-8859-6,ISO-8859-7,ISO-8859-8,ISO-8859-9,JIS_X0201,JIS_X0212-1990,KOI8-R,KOI8-U,Shift_JIS,TIS-620,US-ASCII,UTF-16,UTF-16BE,UTF-16LE,UTF-32,UTF-32BE,UTF-32LE,UTF-8,windows-1250,windows-1251,windows-1252,windows-1253,windows-1254,windows-1255,windows-1256,windows-1257,windows-1258,windows-31j,x-Big5-Solaris,x-euc-jp-linux,x-EUC-TW,x-eucJP-Open,x-IBM1006,x-IBM1025,x-IBM1046,x-IBM1097,x-IBM1098,x-IBM1112,x-IBM1122,x-IBM1123,x-IBM1124,x-IBM1381,x-IBM1383,x-IBM33722,x-IBM737,x-IBM834,x-IBM856,x-IBM874,x-IBM875,x-IBM921,x-IBM922,x-IBM930,x-IBM933,x-IBM935,x-IBM937,x-IBM939,x-IBM942,x-IBM942C,x-IBM943,x-IBM943C,x-IBM948,x-IBM949,x-IBM949C,x-IBM950,x-IBM964,x-IBM970,x-ISCII91,x-ISO-2022-CN-CNS,x-ISO-2022-CN-GB,x-iso-8859-11,x-JIS0208,x-JISAutoDetect,x-Johab,x-MacArabic,x-MacCentralEurope,x-MacCroatian,x-MacCyrillic,x-MacDingbat,x-MacGreek,x-MacHebrew,x-MacIceland,x-MacRoman,x-MacRomania,x-MacSymbol,x-MacThai,x-MacTurkish,x-MacUkraine,x-MS932_0213,x-MS950-HKSCS,x-mswin-936,x-PCK,x-SJIS_0213,x-UTF-16LE-BOM,X-UTF-32BE-BOM,X-UTF-32LE-BOM,x-windows-50220,x-windows-50221,x-windows-874,x-windows-949,x-windows-950,x-windows-iso2022jp";
        String[] s11 = s1.split(",");
        s = s.replaceAll(" ", "");
        String str = null;
        for (int i = 0; i < s11.length; i++) {
            byte[] baKeyword = new byte[s.length() / 2];
            for (int j = 0; j < baKeyword.length; j++) {
                try {
                    baKeyword[j] = (byte) (0xff & Integer.parseInt(s.substring(j * 2, j * 2 + 2), 16));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
//            s = new String(baKeyword, "utf-8");//UTF-16le:Not 
                str = "编码：" + s11[i] + str + new String(baKeyword, s11[i]) + "--------***********************-------";
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        }

        return str;
    }
    /*
     * string2hx
     */

    public static String convertStringToHex(String str) {
        char[] chars = str.toCharArray();
        StringBuffer hex = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            hex.append(Integer.toHexString((int) chars[i]));
        }
        return hex.toString();
    }
    /*
     * hx2string
     */

    public static String convertHexToString(String hex) {
        hex = hex.replaceAll(" ", "");
        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {
            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    /*
     * byte[]2double
     */
    public static double arr2double(byte[] arr, int start) {
        int i = 0;
        int len = 8;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for (i = start; i < (start + len); i++) {
            tmp[cnt] = arr[i];
//System.out.println(java.lang.Byte.toString(arr[i]) + " " + i);
            cnt++;
        }
        long accum = 0;
        i = 0;
        for (int shiftBy = 0; shiftBy < 64; shiftBy += 8) {
            accum |= ((long) (tmp[i] & 0xff)) << shiftBy;
            i++;
        }
        return Double.longBitsToDouble(accum);
    }
}
