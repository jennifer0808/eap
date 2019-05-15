package cn.tzauto.octopus.common.resolver;

import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author njtz
 */
public class TransferUtil {

    private static Logger logger = Logger.getLogger(TransferUtil.class.getName());

    public static ArrayList getIDValue(ArrayList list) {
        ArrayList obj = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof long[]) {
                if (((long[]) list.get(i)).length == 0) {
                    obj.add("零长度long[]");
                } else {
                    obj.add(((long[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof int[]) {
                if (((int[]) list.get(i)).length == 0) {
                    obj.add("零长度int[]");
                } else {
                    obj.add(((int[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof String) {
                obj.add(((String) list.get(i)));
                continue;
            }
            if (list.get(i) instanceof String[]) {
                if (((String[]) list.get(i)).length == 0) {
                    obj.add("零长度String[]");
                } else {
                    obj.add(((String[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof float[]) {
                if (((float[]) list.get(i)).length == 0) {
                    obj.add("零长度float[]");
                } else {
                    obj.add(((float[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof byte[]) {
                if (((byte[]) list.get(i)).length == 0) {
                    obj.add("零长度byte[]");
                } else {
                    obj.add(((byte[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof boolean[]) {
                if (((boolean[]) list.get(i)).length == 0) {
                    obj.add("零长度boolean[]");
                } else {
                    obj.add(((boolean[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof double[]) {
                if (((double[]) list.get(i)).length == 0) {
                    obj.add("零长度double[]");
                } else {
                    obj.add(((double[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof char[]) {
                if (((char[]) list.get(i)).length == 0) {
                    obj.add("零长度char[]");
                } else {
                    obj.add(((char[]) list.get(i))[0]);
                }
                continue;
            }
            if (list.get(i) instanceof List) {
                if (((List) list.get(i)).isEmpty()) {
                    obj.add("零长度list");
                } else {
                    ArrayList tmp = getIDValue((ArrayList) list.get(i));
                    for (int j = 0; j < tmp.size(); j++) {
                        obj.add(tmp.get(j));
                    }
                    //obj.add(getIDValue((ArrayList) list.get(i)));
                }
            }
        }
        return obj;
    }

    public static long getPPLength(String recipePath) {
        int length = 0;
        try {
            FileInputStream in = new FileInputStream(recipePath);
            length = in.available();
            //test for fico
            //length=length-2;
        } catch (Exception e) {
        }
        return (long) length;
    }

    public static List getPPBody(int type, String recipePath) {
        //type: 0:string->string    1:byte[]->sring  4 string->byte[]
        ArrayList list = new ArrayList();
        try {
            String ppbody = null;
            if (type == 0 || type == 4) {
                File file = new File(recipePath);
                if (file.isFile() && file.exists()) {
                    InputStreamReader reader = new InputStreamReader(new FileInputStream(file));
                    BufferedReader br = new BufferedReader(reader);
                    String tmpString = "";
                    while ((tmpString = br.readLine()) != null) {
                        ppbody = ppbody + tmpString;
                        ppbody = ppbody.replaceAll("null", "");
                    }
                    br.close();
                    reader.close();
                } else {
                    logger.error("reicpe 文件路径:" + recipePath + ",未找到文件");
                }
            } else {
                FileInputStream in = new FileInputStream(recipePath);
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
//                System.out.println("Recipe length:" + in.available());
                byte[] temp = new byte[1024];
                int size = 0;
                while ((size = in.read(temp)) != -1) {
                    out.write(temp, 0, size);
                }
                if (type == 2) {
                    ppbody = out.toString();
                } else {
                    byte tmp[] = new byte[in.available()];
                    tmp = out.toByteArray();
                    list.add(tmp);
                }
                out.close();
                in.close();
            }
            if (ppbody == null || ppbody.isEmpty() || "".equals(ppbody)) {
                return list;
            } else {
                list.add(ppbody);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public static void setPPBody(Object ppbody, int rcptype, String recipePath) {
        //rcptype:0 string ; 1 byte[]
        try {

            File file = new File(recipePath);
            //if file doesnt exists, then create it

            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();//创建文件夹
                }
                file.createNewFile();//创建文件
            }
            if (rcptype == 0) {
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write((String) ppbody);
//                bw.write(ppbody);
                bw.close();
                fw.close();
            } else {
                FileOutputStream out = new FileOutputStream(recipePath);
                out.write((byte[]) ppbody);
                out.close();
            }
        } catch (Exception e) {
            logger.error(ppbody.toString()+","+rcptype+",存储路径：["+recipePath+"]",e);
            e.printStackTrace();
        }
    }





}
