package cn.tzauto.octopus.common.resolver.nitto;

import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class NittoRecipeUtil {

    // 将乱码文件转成BYTE数组
    public static byte[] trans(String recipepath) {
        byte[] buffer = null;
        try {
            File file = new File(recipepath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream(1000);
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }

    // 1byte、2byte和4byte的转换方法
    public static String tranOx(String num) {
        String[] nums = num.split(",");
        String oxStr = "";
        if (nums.length == 1) {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            oxStr = oxStr1;
        } else if (nums.length == 2) {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String numOx2 = Integer.toHexString(Integer.parseInt(nums[1]));
            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            String oxStr2 = "";
            if (Integer.parseInt(nums[1]) < 0) {
                oxStr2 = numOx2.substring(6, numOx2.length());
            } else {
                oxStr2 = numOx2;
            }
            oxStr = oxStr1 + oxStr2;

        } else {
            String numOx1 = Integer.toHexString(Integer.parseInt(nums[0]));
            String numOx2 = Integer.toHexString(Integer.parseInt(nums[1]));
            String numOx3 = Integer.toHexString(Integer.parseInt(nums[2]));
            String numOx4 = Integer.toHexString(Integer.parseInt(nums[3]));

            String oxStr1 = "";
            if (Integer.parseInt(nums[0]) < 0) {
                oxStr1 = numOx1.substring(6);
            } else {
                oxStr1 = numOx1;
            }
            String oxStr2 = "";
            if (Integer.parseInt(nums[1]) < 0) {
                oxStr2 = numOx2.substring(6, numOx2.length());
            } else {
                oxStr2 = numOx2;
            }
            String oxStr3 = "";
            if (Integer.parseInt(nums[2]) < 0) {
                oxStr3 = numOx3.substring(6, numOx3.length());
            } else {
                oxStr3 = numOx3;

            }
            String oxStr4 = "";
            if (Integer.parseInt(nums[3]) < 0) {
                oxStr4 = numOx4.substring(6, numOx4.length());
            } else {
                oxStr4 = numOx4;
            }
            oxStr = oxStr1 + oxStr2 + oxStr3 + oxStr4;

        }
        return Integer.valueOf(oxStr, 16).toString();
    }

    // 将byte数组导入到excel表中
    /**
     * @param filePath
     * @param savedFilePath
     */
    @SuppressWarnings("null")
    public static List<RecipePara> tPRecipeTran(String filePath) {
        List<RecipePara> recipeParas = new ArrayList<>();
//		WritableWorkbook wwb = null;
        BufferedReader br = null;
        try {
            // getFileName()方法为获取filePath路径下的所有文件名的字符串数组
//			String[] fileNames = getFileName(filePath);
//			for (int j1 = 0; j1 < fileNames.length; j1++) {
//				String saveExcelName = fileNames[j1];
            File cfgfile = new File(filePath);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(cfgfile), "UTF-8"));
//				// 获取文件名中最后一个“ . ”出现的位置
//				int dot = saveExcelName.lastIndexOf(".");
//				// 判断是否有后文件后缀，如果有，就截取去除文件后缀后的文件名
//				if ((dot > -1) && (dot < saveExcelName.length())) {
//					saveExcelName = saveExcelName.substring(0, dot);
//				}
//				File sFile = new File(savedFilePath + saveExcelName + ".xls");
//				// 判断文件是否已经解析过，如果解析过，就继续下一轮循环
//				if (sFile.exists() && sFile.isFile()) {
//					continue;
//				}
//				OutputStream os = new FileOutputStream(savedFilePath + saveExcelName + ".xls");
//				wwb = Workbook.createWorkbook(os);
//				jxl.write.WritableSheet ws = wwb.createSheet("Sheet 1", 0);
//				// 在EXCEL表中的（0，0）坐标写入数据“KEY”，下面四行代码为定义两列的列名
//				Label l1 = new Label(0, 0, "id");
//				ws.addCell(l1);
//				Label l2 = new Label(1, 0, "name");
//				ws.addCell(l2);
//				Label l3 = new Label(2, 0, "unit");
//				ws.addCell(l3);
//				Label l4 = new Label(3, 0, "real");
//				ws.addCell(l4);
//				Label l5 = new Label(4, 0, "ecmin");
//				ws.addCell(l5);
//				Label l6 = new Label(5, 0, "ecmax");
//				ws.addCell(l6);
//				Label l7 = new Label(6, 0, "description");
//				ws.addCell(l7);

            String xx = cfgfile.getAbsolutePath();
            byte[] b = trans(xx);

            if (xx != null) {
                String aa = "";
                for (int n = 0; n < 17; n++) {
                    String s = b[n] + " ";
                    String[] chars = s.split(" ");
                    for (int y = 0; y < chars.length; y++) {
                        char ch = (char) Integer.parseInt(chars[y]);
                        String strStringType = String.valueOf(ch);
                        aa = aa + strStringType;
                    }
                }

                RecipePara recipePara1 = new RecipePara();
                recipePara1.setParaCode("1");
                recipePara1.setParaName("RecipeName");
                recipePara1.setParaMeasure("--");
                recipePara1.setSetValue(aa);
                recipePara1.setMinValue("");
                recipePara1.setMaxValue("");
                recipePara1.setRemarks("Recipe Name is Used. ASCII character List");
                recipeParas.add(recipePara1);

                for (int s = 0; s < b.length; s++) {
                    RecipePara recipePara = new RecipePara();
                    if (s == 16) {
                        recipePara.setParaCode("2");
                        recipePara.setParaName("WaferSize");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("200mm");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("300mm");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Wafer Size");

                    } else if (s == 17) {
                        recipePara.setParaCode("3");
                        recipePara.setParaName("WaferType");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NOTCH");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("FLAT");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Wafer Type.");

                    } else if (s == 18) {
                        recipePara.setParaCode("4");
                        recipePara.setParaName("CassetteMode");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("PARALLEL");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("CROSS");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("SINGLE");
                        } else if ("3".equals(b[s] + "")) {
                            recipePara.setSetValue("25 * 13");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Cassette Mode.");

                    } else if (s == 19) {
                        recipePara.setParaCode("5");
                        recipePara.setParaName("TableSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NORMAL");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("OVER CUT");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("UNIVERSAL");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Table Select.");

                    } else if (s == 20) {
                        recipePara.setParaCode("6");
                        recipePara.setParaName("LeftCassetteSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("FOUP");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("OPEN CASSETTE/ FOSB");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("UNIVERSAL");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Wafer Left Cassette Type.");

                    } else if (s == 21) {
                        recipePara.setParaCode("7");
                        recipePara.setParaName("LeftCassetteTheachingType");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("T1");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("T2");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("T3");
                        } else if ("3".equals(b[s] + "")) {
                            recipePara.setSetValue("T4");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Left Cassette Teaching Type.");

                    } else if (s == 22) {
                        recipePara.setParaCode("8");
                        recipePara.setParaName("RightCassetteSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("FOUP");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("OPEN CASSETTE/ FOSB");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("DSC");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Wafer Right Cassette Type.");

                    } else if (s == 23) {
                        recipePara.setParaCode("9");
                        recipePara.setParaName("RightCassetteTheachingType");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("T1");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("T2");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("T3");
                        } else if ("3".equals(b[s] + "")) {
                            recipePara.setSetValue("T4");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Right Cassette Teaching Type.");

                    } else if (s == 24) {
                        recipePara.setParaCode("10");
                        recipePara.setParaName("ApplyTension");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape applies tension.");

                    } else if (s == 25) {
                        recipePara.setParaCode("11");
                        recipePara.setParaName("AfterApplyTension");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("After Tape apply tension.");

                    } else if (s == 26) {
                        recipePara.setParaCode("12");
                        recipePara.setParaName("ReturnTension");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Return Tension.");

                    } else if (s == 27) {
                        recipePara.setParaCode("13");
                        recipePara.setParaName("TapePitchTension");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape pitch tension.");

                    } else if (s == 28) {
                        recipePara.setParaCode("14");
                        recipePara.setParaName("ManualFeedTension");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape feed tension.");

                    } else if (s == 29) {
                        recipePara.setParaCode("15");
                        recipePara.setParaName("LinerSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("WEAK");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("STRONG");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Liner Select.");

                    } else if (s == 30) {
                        recipePara.setParaCode("16");
                        recipePara.setParaName("AfterApplyingLinerWind");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("After Applying Liner Wind.");

                    } else if (s == 31) {
                        recipePara.setParaCode("17");
                        recipePara.setParaName("AfterReturnUnitLinerWind");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("After Return Unit Liner Wind.");

                    } else if (s == 32) {
                        recipePara.setParaCode("18");
                        recipePara.setParaName("DeliveryMoveSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Tape delivery roll motor rotate select.");

                    } else if (s == 33) {
                        recipePara.setParaCode("19");
                        recipePara.setParaName("DeliveryPointA");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point A.");

                    } else if (s == 35) {
                        recipePara.setParaCode("20");
                        recipePara.setParaName("DeliveryPointB");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point B.");

                    } else if (s == 37) {
                        recipePara.setParaCode("21");
                        recipePara.setParaName("DeliveryPointC");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point C.");

                    } else if (s == 39) {
                        recipePara.setParaCode("22");
                        recipePara.setParaName("DeliveryPointD");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point D.");

                    } else if (s == 41) {
                        recipePara.setParaCode("23");
                        recipePara.setParaName("DeliveryPointE");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point E.");

                    } else if (s == 43) {
                        recipePara.setParaCode("24");
                        recipePara.setParaName("DeliveryPointF");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Delivery Point F.");

                    } else if (s == 45) {
                        recipePara.setParaCode("25");
                        recipePara.setParaName("DeliverySpeedA");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point A.");

                    } else if (s == 46) {
                        recipePara.setParaCode("26");
                        recipePara.setParaName("DeliverySpeedB");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point B.");

                    } else if (s == 47) {
                        recipePara.setParaCode("27");
                        recipePara.setParaName("DeliverySpeedC");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point C.");

                    } else if (s == 48) {
                        recipePara.setParaCode("28");
                        recipePara.setParaName("DeliverySpeedD");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point D.");

                    } else if (s == 49) {
                        recipePara.setParaCode("29");
                        recipePara.setParaName("DeliverySpeedE");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point E.");

                    } else if (s == 50) {
                        recipePara.setParaCode("30");
                        recipePara.setParaName("DeliverySpeedF");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("10");
                        recipePara.setRemarks("Delivery Speed for Control Point F.");

                    } else if (s == 51) {
                        recipePara.setParaCode("31");
                        recipePara.setParaName("ApplyPointA");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Apply Point A.");

                    } else if (s == 53) {
                        recipePara.setParaCode("32");
                        recipePara.setParaName("ApplyPointB");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Apply Point B.");

                    } else if (s == 55) {
                        recipePara.setParaCode("33");
                        recipePara.setParaName("ApplyPointC");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Apply Point C.");

                    } else if (s == 57) {
                        recipePara.setParaCode("34");
                        recipePara.setParaName("ApplyPointD");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Apply Point D.");

                    } else if (s == 59) {
                        recipePara.setParaCode("35");
                        recipePara.setParaName("ApplyPointE");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Apply Point E.");

                    } else if (s == 61) {
                        recipePara.setParaCode("36");
                        recipePara.setParaName("ApplyPressureA");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply pressure value for Control Point A.");

                    } else if (s == 62) {
                        recipePara.setParaCode("37");
                        recipePara.setParaName("ApplyPressureB");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply pressure value for Control Point B.");

                    } else if (s == 63) {
                        recipePara.setParaCode("38");
                        recipePara.setParaName("ApplyPressureC");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply pressure value for Control Point C.");

                    } else if (s == 64) {
                        recipePara.setParaCode("39");
                        recipePara.setParaName("ApplyPressureD");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply pressure value for Control Point D.");

                    } else if (s == 65) {
                        recipePara.setParaCode("40");
                        recipePara.setParaName("ApplyPressureE");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply pressure value for Control Point E.");

                    } else if (s == 66) {
                        recipePara.setParaCode("41");
                        recipePara.setParaName("TapeExpandSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Tape Expand Select.");

                    } else if (s == 67) {
                        recipePara.setParaCode("42");
                        recipePara.setParaName("ApplyRollerRewind");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Tape rewind for Apply Roll down select.");

                    } else if (s == 68) {
                        recipePara.setParaCode("43");
                        recipePara.setParaName("AfterApplyUnitMoveSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("2mm MOVE");
                        } else if ("2".equals(b[s] + "")) {
                            recipePara.setSetValue("3mm MOVE");
                        } else if ("3".equals(b[s] + "")) {
                            recipePara.setSetValue("4mm MOVE");
                        } else if ("4".equals(b[s] + "")) {
                            recipePara.setSetValue("5mm MOVE");
                        } else if ("5".equals(b[s] + "")) {
                            recipePara.setSetValue("6mm MOVE");
                        } else if ("6".equals(b[s] + "")) {
                            recipePara.setSetValue("7mm MOVE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("After apply Unit Move tape sag adjust Select.");

                    } else if (s == 69) {
                        recipePara.setParaCode("44");
                        recipePara.setParaName("LinerDeliverySelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Liner delivery before apply roll down select.");

                    } else if (s == 70) {
                        recipePara.setParaCode("45");
                        recipePara.setParaName("TapeWindTimer");
                        recipePara.setParaMeasure("Sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("99");
                        recipePara.setRemarks("Tape winding timer.");

                    } else if (s == 71) {
                        recipePara.setParaCode("46");
                        recipePara.setParaName("PeelRollerRotateSpeed");
                        recipePara.setParaMeasure("Mm/sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("20");
                        recipePara.setRemarks("Peel Roller Rotate Speed.");

                    } else if (s == 72) {
                        recipePara.setParaCode("47");
                        recipePara.setParaName("ApplyingLate");
                        recipePara.setParaMeasure("Msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("300");
                        recipePara.setRemarks("Applying Acceleration.");

                    } else if (s == 74) {
                        recipePara.setParaCode("48");
                        recipePara.setParaName("TapePitchTension2");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape pitch tension2.");

                    } else if (s == 75) {
                        recipePara.setParaCode("49");
                        recipePara.setParaName("TapePitchTension3");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape pitch tension3.");

                    } else if (s == 76) {
                        recipePara.setParaCode("50");
                        recipePara.setParaName("ApplyingHspd");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("300");
                        recipePara.setRemarks("Applying High speed.");

                    } else if (s == 78) {
                        recipePara.setParaCode("51");
                        recipePara.setParaName("PeelingLate");
                        recipePara.setParaMeasure("Msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("300");
                        recipePara.setRemarks("Peeling Acceleration.");

                    } else if (s == 80) {
                        recipePara.setParaCode("52");
                        recipePara.setParaName("AfterApplyTension2");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("After Tape apply tension2.");

                    } else if (s == 81) {
                        recipePara.setParaCode("53");
                        recipePara.setParaName("AfterApplyTension3");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("After Tape apply tension3.");

                    } else if (s == 82) {
                        recipePara.setParaCode("54");
                        recipePara.setParaName("PeelingHspd");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("300");
                        recipePara.setRemarks("Peeling High speed.");

                    } else if (s == 84) {
                        recipePara.setParaCode("55");
                        recipePara.setParaName("LinerWindPressure");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Liner Wind Pressure");

                    } else if (s == 85) {
                        recipePara.setParaCode("56");
                        recipePara.setParaName("WasteWindPressure");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Waste tape Wind Pressure");

                    } else if (s == 86) {
                        recipePara.setParaCode("57");
                        recipePara.setParaName("TapeExpandPressure");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Tape Expand Pressure");

                    } else if (s == 87) {
                        recipePara.setParaCode("58");
                        recipePara.setParaName("PeelingRollSpeed");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Peeling Roll Speed.");

                    } else if (s == 88) {
                        recipePara.setParaCode("59");
                        recipePara.setParaName("CuttingOFPosition1");
                        recipePara.setParaMeasure("*0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Position1.");

                    } else if (s == 90) {
                        recipePara.setParaCode("60");
                        recipePara.setParaName("CuttingOFPosition2");
                        recipePara.setParaMeasure("*0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Position2.");

                    } else if (s == 92) {
                        recipePara.setParaCode("61");
                        recipePara.setParaName("CuttingOFPosition3");
                        recipePara.setParaMeasure("*0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Position3.");

                    } else if (s == 94) {
                        recipePara.setParaCode("62");
                        recipePara.setParaName("CuttingOFRate1");
                        recipePara.setParaMeasure("*0.1msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Acceleration1.");

                    } else if (s == 96) {
                        recipePara.setParaCode("63");
                        recipePara.setParaName("CuttingOFRate2");
                        recipePara.setParaMeasure("*0.1msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Acceleration2.");

                    } else if (s == 98) {
                        recipePara.setParaCode("64");
                        recipePara.setParaName("CuttingOFRate3");
                        recipePara.setParaMeasure("*0.1msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Flat Wafer Cutting Acceleration3.");

                    } else if (s == 100) {
                        recipePara.setParaCode("65");
                        recipePara.setParaName("ApplyRollHeaterSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Apply Roll Heater Select");

                    } else if (s == 101) {
                        recipePara.setParaCode("66");
                        recipePara.setParaName("ApplyRollTemp");
                        recipePara.setParaMeasure("deg.");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Apply Roll Temp");

                    } else if (s == 102) {
                        recipePara.setParaCode("67");
                        recipePara.setParaName("Robot_vacuum_check_sel ");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Robot vacuum check select.");

                    } else if (s == 103) {
                        recipePara.setParaCode("68");
                        recipePara.setParaName("TapeVacuumSelect ");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("No Use");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("Use");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Table Vacuum Select");

                    } else if (s == 104) {
                        recipePara.setParaCode("69");
                        recipePara.setParaName("TapePinchSelect ");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("No Use");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("Use");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Table Pinch Select");

                    } else if (s == 105) {
                        recipePara.setParaCode("70");
                        recipePara.setParaName("UnitReturnFeedSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("No Use");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("Use");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Unit Return Feed Select.");

                    } else if (s == 106) {
                        recipePara.setParaCode("71");
                        recipePara.setParaName("CuttingOFHspd1");
                        recipePara.setParaMeasure("*0.1mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("3000");
                        recipePara.setRemarks("Flat Wafer Cutting Speed1.");

                    } else if (s == 108) {
                        recipePara.setParaCode("72");
                        recipePara.setParaName("CuttingOFHspd2");
                        recipePara.setParaMeasure("*0.1mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("3000");
                        recipePara.setRemarks("Flat Wafer Cutting Speed2.");

                    } else if (s == 110) {
                        recipePara.setParaCode("73");
                        recipePara.setParaName("CuttingOFHspd3");
                        recipePara.setParaMeasure("*0.1mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("3000");
                        recipePara.setRemarks("Flat Wafer Cutting Speed3.");

                    } else if (s == 112) {
                        recipePara.setParaCode("74");
                        recipePara.setParaName("CuttingVPoint");
                        recipePara.setParaMeasure("*0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("9999");
                        recipePara.setRemarks("Notch Wafer Cutting Point1.");

                    } else if (s == 114) {
                        recipePara.setParaCode("75");
                        recipePara.setParaName("CuttingVRate");
                        recipePara.setParaMeasure("*0.1 msec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("3000");
                        recipePara.setRemarks("Notch Wafer Cutting Acceleration1.");

                    } else if (s == 116) {
                        recipePara.setParaCode("76");
                        recipePara.setParaName("TapeDeliveryPinchSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("No Use");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("Use");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Tape Delivery Pinch Select");

                    } else if (s == 117) {
                        recipePara.setParaCode("77");
                        recipePara.setParaName("PeelRollPinchSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("No Use");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("Use");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Peel Roll Pinch Select");

                    } else if (s == 118) {
                        recipePara.setParaCode("78");
                        recipePara.setParaName("CuttingVHspd");
                        recipePara.setParaMeasure("mm/sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("3000");
                        recipePara.setRemarks("Notch Wafer Cutting Speed1.");

                    } else if (s == 120) {
                        recipePara.setParaCode("79");
                        recipePara.setParaName("CutterStepDownSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Cutter Step Down Select");

                    } else if (s == 121) {
                        recipePara.setParaCode("80");
                        recipePara.setParaName("CutterAngleChangeSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Cutter angle change select.");

                    } else if (s == 122) {
                        recipePara.setParaCode("81");
                        recipePara.setParaName("CutterCountSet");
                        recipePara.setParaMeasure("count");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("500");
                        recipePara.setRemarks("Cutting count setting.");

                    } else if (s == 124) {
                        recipePara.setParaCode("82");
                        recipePara.setParaName("StepDownUnitDownPitch");
                        recipePara.setParaMeasure("mm");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("30");
                        recipePara.setRemarks("Tape cutter unit down pitch.(Step Down)");

                    } else if (s == 125) {
                        recipePara.setParaCode("83");
                        recipePara.setParaName("StepDownCutCountSet");
                        recipePara.setParaMeasure("--");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("1");
                        recipePara.setMaxValue("50");
                        recipePara.setRemarks("Tape cutting count set.(Step Down)");

                    } else if (s == 127) {
                        recipePara.setParaCode("84");
                        recipePara.setParaName("ManualTapeFeedLinerWindPressur e");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Liner Wind Pressure at  Manual Tape Feed");

                    } else if (s == 128) {
                        recipePara.setParaCode("85");
                        recipePara.setParaName("ManualTapeFeedWastWindPressur e");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Waste tape Wind Pressure at  Manual Tape Feed");

                    } else if (s == 129) {
                        recipePara.setParaCode("86");
                        recipePara.setParaName("ManualTapeFeedTapeDeliverySpee d");
                        recipePara.setParaMeasure("mmc");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("30");
                        recipePara.setRemarks("Tape Delivery Speed at Manual Tape Feed");

                    } else if (s == 130) {
                        recipePara.setParaCode("87");
                        recipePara.setParaName("CutterHeaterSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Cutter Heater select.");

                    } else if (s == 131) {
                        recipePara.setParaCode("88");
                        recipePara.setParaName("TableHeaterSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Table Heater select.");

                    } else if (s == 132) {
                        recipePara.setParaCode("89");
                        recipePara.setParaName("CutterTempSetting");
                        recipePara.setParaMeasure("Deg");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("200");
                        recipePara.setRemarks("Cutter Heater Temperature setting.");

                    } else if (s == 133) {
                        recipePara.setParaCode("90");
                        recipePara.setParaName("TableTempSetting");
                        recipePara.setParaMeasure("Deg");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("70");
                        recipePara.setRemarks("Table Heater Temperature setting.");

                    } else if (s == 134) {
                        recipePara.setParaCode("91");
                        recipePara.setParaName("ScannerSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("OFF");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("ON");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("");

                    } else if (s == 135) {
                        recipePara.setParaCode("92");
                        recipePara.setParaName("MapStartPosLeft");
                        recipePara.setParaMeasure("--");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("");

                    } else if (s == 139) {
                        recipePara.setParaCode("93");
                        recipePara.setParaName("CassCapacityLeft");
                        recipePara.setParaMeasure("Slot");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("25");
                        recipePara.setRemarks("");

                    } else if (s == 140) {
                        recipePara.setParaCode("94");
                        recipePara.setParaName("CassPitchLeft");
                        recipePara.setParaMeasure("um");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("20000");
                        recipePara.setRemarks("");

                    } else if (s == 144) {
                        recipePara.setParaCode("95");
                        recipePara.setParaName("WaferThicknessLeft");
                        recipePara.setParaMeasure("um");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("900");
                        recipePara.setRemarks("");

                    } else if (s == 148) {
                        recipePara.setParaCode("96");
                        recipePara.setParaName("MapStartPosRight");
                        recipePara.setParaMeasure("--");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("");

                    } else if (s == 152) {
                        recipePara.setParaCode("97");
                        recipePara.setParaName("CassCapacityRight");
                        recipePara.setParaMeasure("Slot");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("25");
                        recipePara.setRemarks("");

                    } else if (s == 153) {
                        recipePara.setParaCode("98");
                        recipePara.setParaName("CassPitchRight");
                        recipePara.setParaMeasure("um");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("20000");
                        recipePara.setRemarks("");

                    } else if (s == 157) {
                        recipePara.setParaCode("99");
                        recipePara.setParaName("WaferThicknessRight");
                        recipePara.setParaMeasure("um");
                        recipePara.setSetValue(tranOx(b[s + 3] + "," + b[s + 2] + "," + b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("900");
                        recipePara.setRemarks("");

                    } else if (s == 161) {
                        recipePara.setParaCode("100");
                        recipePara.setParaName("OcrSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USEE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("(Option)  OCR Select (Ioss).");

                    } else if (s == 162) {
                        recipePara.setParaCode("101");
                        recipePara.setParaName("CassetteBarCodeSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("(Option)Cassette Bar Code Select.");

                    } else if (s == 163) {
                        recipePara.setParaCode("102");
                        recipePara.setParaName("OcrDetectPosition");
                        recipePara.setParaMeasure("0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("-80");
                        recipePara.setMaxValue("700");
                        recipePara.setRemarks("(Option)OCR Detect Position.");

                    } else if (s == 165) {
                        recipePara.setParaCode("103");
                        recipePara.setParaName("T7ReadPosition");
                        recipePara.setParaMeasure("0.1degree");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("-1800");
                        recipePara.setMaxValue("1800");
                        recipePara.setRemarks("(Option)T7ReadPosition .");

                    } else if (s == 167) {
                        recipePara.setParaCode("104");
                        recipePara.setParaName("LinerWindStopDetect ");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("(Option)Liner Wind Stop Detec.");

                    } else if (s == 168) {
                        recipePara.setParaCode("105");
                        recipePara.setParaName("TapeFeedTimer");
                        recipePara.setParaMeasure("0.1sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("300");
                        recipePara.setRemarks("Liner Tape Feed Timer");

                    } else if (s == 170) {
                        recipePara.setParaCode("106");
                        recipePara.setParaName("PeelRollMultiplierSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("PeelingRoll Multiplier Select");

                    } else if (s == 171) {
                        recipePara.setParaCode("107");
                        recipePara.setParaName("TableWaitTimer");
                        recipePara.setParaMeasure("0.1 sec");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("99");
                        recipePara.setRemarks("Table Wait Timer");

                    } else if (s == 173) {
                        recipePara.setParaCode("108");
                        recipePara.setParaName("AirBlowPressure");
                        recipePara.setParaMeasure("%");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("100");
                        recipePara.setRemarks("Air Blow Pressure");

                    } else if (s == 175) {
                        recipePara.setParaCode("109");
                        recipePara.setParaName("AirBlowSelect");
                        recipePara.setParaMeasure("--");
                        if ("0".equals(b[s] + "")) {
                            recipePara.setSetValue("NO USE");
                        } else if ("1".equals(b[s] + "")) {
                            recipePara.setSetValue("USE");
                        } else {
                            System.out.println("无此数据");
                        }
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipePara.setRemarks("Air Blow Select");

                    } else if (s == 176) {
                        recipePara.setParaCode("110");
                        recipePara.setParaName("TableAlarmRange");
                        recipePara.setParaMeasure("0.1deg.C");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("30");
                        recipePara.setMaxValue("200");
                        recipePara.setRemarks("(Option) Table Alarm Range.");

                    } else if (s == 178) {
                        recipePara.setParaCode("111");
                        recipePara.setParaName("ApplyWaitTimer");
                        recipePara.setParaMeasure("0.1sec");
                        recipePara.setSetValue(tranOx(b[s] + ""));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("150");
                        recipePara.setRemarks("Apply Wait Timer.");

                    } else if (s == 179) {
                        recipePara.setParaCode("112");
                        recipePara.setParaName("CutterStartOffset");
                        recipePara.setParaMeasure("0.1mm");
                        recipePara.setSetValue(tranOx(b[s + 1] + "," + b[s]));
                        recipePara.setMinValue("0");
                        recipePara.setMaxValue("6450");
                        recipePara.setRemarks("CutterStartOffset.");
                    }
                    if (recipePara.getParaCode() != null) {
                        recipePara.setMinValue("");
                        recipePara.setMaxValue("");
                        recipeParas.add(recipePara);
                    }
                }

//					for (int j = 0; j < recipeParas.size(); j++) {
//						Label label1 = new Label(0, j + 1, recipeParas.get(j).getId());
//						ws.addCell(label1);
//						Label label2 = new Label(1, j + 1, recipeParas.get(j).getName());
//						ws.addCell(label2);
//						Label label3 = new Label(2, j + 1, recipeParas.get(j).getUnit());
//						ws.addCell(label3);
//						Label label4 = new Label(3, j + 1, recipeParas.get(j).getReal());
//						ws.addCell(label4);
//						Label label5 = new Label(4, j + 1, recipeParas.get(j).getEcmin());
//						ws.addCell(label5);
//						Label label6 = new Label(5, j + 1, recipeParas.get(j).getEcmax());
//						ws.addCell(label6);
//						Label label7 = new Label(6, j + 1, recipeParas.get(j).getDescription());
//						ws.addCell(label7);
//					}
//				}
                // 插入EXCEL文件完成后清空LIST，继续下次循环
//				recipeParas.clear();
                br.close();
//				wwb.write();
//				wwb.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recipeParas;
    }

    public static String[] getFileName(String path) {
        File file = new File(path);
        String[] fileName = file.list();
        return fileName;
    }

    public static void main(String[] args) {
        List<RecipePara> l = new NittoRecipeUtil().tPRecipeTran("D:\\RECIPE\\12-V0-260-N0V_V0.txt");
        for (int i = 0; i < l.size(); i++) {
//            System.out.println(l.get(i).toString());
            System.out.println("paraName:" + l.get(i).getParaName());
            System.out.println("paraValue:" + l.get(i).getSetValue());
        }

    }
}
