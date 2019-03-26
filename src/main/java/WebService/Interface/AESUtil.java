package WebService.Interface;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
public class AESUtil {





    /**
     * 测试token加密
     * @param args
     */
        public static void main(String[] args) {
            // 取当前UTC时间
            java.util.Calendar cal = java.util.Calendar.getInstance();
            long tt = cal.getTimeInMillis();
String SysName="HR_EAP";
String ReqTimes="1517390437935";
//            String aesInfo = "HR_EAP@1517390437935";
            String aesInfo=SysName+"@"+ReqTimes;
            System.out.println("加密前:" + aesInfo);
            String aesKey = "370b15019219dfc81a6cc4f1192cf623";
            String sdsd="OQlDNjg3QkYyMDYwRTc0NTg4RTI3Q0NFN0Y2Q0VEQjVGOEJCRDExQUIyQkFCRjY0OUU1MzdCMjVCMzZCMTZBOQ==";
            try {
                String toAes = AesEncrypt(aesInfo, aesKey);
                System.out.println("加密后:" + toAes);
                String aesDecrypt = AesDecrypt(sdsd, aesKey);
                System.out.println("解密后:" + aesDecrypt);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    /**
     * AES加密
     *
     * @param sSrc  加密字符串
     * @param sKey	加密key
     * @return
     * @throws Exception
     */
        public static String AesEncrypt(String sSrc, String sKey) throws Exception {
            if (sKey == null) {
                throw new IllegalArgumentException("Argument sKey is null.");
            }
            // 根据密钥转ASCII编码
            byte[] raw = sKey.substring(0, 16).getBytes("ASCII");
            //byte[] raw = EncodingUtils.getAsciiBytes(sKey.substring(0, 16));
            // 根据字节数组生成AES密钥
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            // 根据指定算法AES自成密码器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // 初始化密码器，第一个参数为加密(Encrypt_mode)或者解密解密(Decrypt_mode)操作，第二个参数为使用的KEY
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            // 根据密码器的初始化方式--加密：将数据加密
            byte[] encrypted = cipher.doFinal(sSrc.getBytes("UTF-8"));
            // 将加密后的数据转换为字符串（二进制转16进制）
            String tempStr = parseByte2HexStr(encrypted);

            // 将数据转换为Base64编码
            return  Base64.encodeBase64String(tempStr.getBytes("UTF-8"));
        }

    /**
     * AES解密
     *
     * @param sSrc  解密字符串
     * @param sKey	解密ey
     * @return
     * @throws Exception
     */
        public static String AesDecrypt(String sSrc, String sKey) throws Exception {
            String originalString = "";
            if (sKey == null) {
                return originalString;
            }
            // ������ԿתASCII����
            byte[] raw = sKey.substring(0, 16).getBytes("ASCII");
            //byte[] raw = EncodingUtils.getAsciiBytes(sKey.substring(0, 16));
            // �����ֽ���������AES��Կ
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            // ����ָ���㷨AES�Գ�������
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // ��ʼ������������һ������Ϊ����(Encrypt_mode)���߽��ܽ���(Decrypt_mode)�������ڶ�������Ϊʹ�õ�KEY
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            // ������ת��ΪBase64����
            Base64 encoder = new Base64();
            byte[] encrypted1 = encoder.decode(sSrc);
            String tempStr = new String(encrypted1, "UTF-8");
            // �����ܺ������ת��Ϊ�ַ�����16����ת�����ƣ�
            encrypted1 = parseHexStr2Byte(tempStr);
            // �����������ĳ�ʼ����ʽ--���ܣ������ݽ���
            byte[] original = cipher.doFinal(encrypted1);

            originalString = new String(original, "UTF-8");
            return originalString;
        }
    /**
     * 将二进制转换为16进制，避免* padded cipher错误
     *
     * @param buf
     * @return
     */
        public static String parseByte2HexStr(byte buf[]) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < buf.length; i++) {
                String hex = Integer.toHexString(buf[i] & 0xFF);
                if (hex.length() == 1) {
                    hex = '0' + hex;
                }
                sb.append(hex.toUpperCase());
            }
            return sb.toString();
        }

        /**
         * ��16����ת��Ϊ������
         *
         * @param hexStr
         *            ת���ַ���
         * @return
         */
        public static byte[] parseHexStr2Byte(String hexStr) {
            if (hexStr.length() < 1)
                return null;
            byte[] result = new byte[hexStr.length() / 2];
            for (int i = 0; i < hexStr.length() / 2; i++) {
                int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
                int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),
                        16);
                result[i] = (byte) (high * 16 + low);
            }
            return result;
        }

        /**
         * ȡ��ǰʱ��UTCֵ
         *
         * @return String
         */
        public static String getNowTimeToUtc() {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            long tt = cal.getTimeInMillis();
            return String.valueOf(tt);
        }








}
