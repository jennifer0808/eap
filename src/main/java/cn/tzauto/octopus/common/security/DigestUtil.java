package cn.tzauto.octopus.common.security;

/**
 * Created by gavin on 15/12/9. 解密类，与JCOA同步
 */
public class DigestUtil {

    public static final String HASH_ALGORITHM = "SHA-1";
    public static final int HASH_INTERATIONS = 1024;
    public static final int SALT_SIZE = 8;

    public static String passwordDeEncrypt(String password) {
        String result = "";
        String rc1 = "";
        String temp1;
        int l, k;

        String PKey = "0231042620191022290301120823283206301118152524132717071605210914";
        if (password.length() != 33) {
            result = password;
        } else {
            l = password.charAt(password.length() - 1) - 88;
            for (int i = 1; i <= l; i++) {
                temp1 = PKey.substring(i * 2 - 2, i * 2);
                int mm = Integer.parseInt(temp1);
                temp1 = password.substring(mm - 1, mm);
                mm = (int) temp1.toCharArray()[0];
                k = 126 - mm + 40;
                rc1 = rc1 + (char) k;
            }

            int slength = rc1.length();
            for (int j = 0; j < slength; j = j + 2) {
                temp1 = rc1.substring(j, j + 2);
                k = hexToInt(temp1);
                result = result + (char) k;
                //System.out.println(result);
            }

        }

        return result;
    }

    public static int hexToInt(String str) {
        int res = 0;
        int mm = 0;
        int k = 1, w = 1;
        for (int i = 0; i < str.length(); i++) {
            //mm=Integer.parseInt(String.valueOf(str.charAt(i)));
            mm = str.charAt(i);
            if (mm >= 48 && mm <= 57) {
                k = mm - 48;
            } else if (mm >= 65 && mm <= 70) {
                k = mm - 55;
            } else {
                k = 0;
            }
            res = res * 16 + k;
            //res = res*16  + Integer.parseInt(String.valueOf(str.charAt(i)), 16);
        }
        return res;
    }

    public static void parseList(String... strs) {
        if (strs != null && strs.length > 0) {
            for (String str : strs) {
                System.out.println(str + "------" + passwordDeEncrypt(str));
            }
        }
    }

    public static void main(String[] args) {

        System.out.println(entryptPassword("tzinfo"));
      //  System.out.println(passwordDeEncrypt("511fa94db45856e1986872ed882a89d2ee371011172fa0cc0c91dd88"));

//        System.out.println(passwordDeEncrypt("squpds(sPpstb\\t{]papLosxaukssqmrn"));
//        System.out.println("00007965  施宾:" + passwordDeEncrypt("srrq*sAsCpssQ]t_Pscrxrq2bnpsposrn"));
//        System.out.println("00029832  施宾:" + passwordDeEncrypt("ssusGpvrSstrCn;C)sus`vrBkotpsmnul"));
//        parseList("sonqbs,skstpJ9AU>aus/muNGs/ssostl","tqcpXsNs=osco*sZoteozvpsuusspmutr","ps`q(=AoKp1ty0r]\\@mp4uqULnstpWqah");
//        Scanner sc = new Scanner(System.in); 
//        while(sc.hasNext()){
//            parseList(sc.next());
//        }
//        parseList("sstsTsvsdssnoXsD\\ousSppsmpJssqrmp");
    }

    /**
     * 生成安全的密码 ，生成随机的16位salt并经过1024次 sha -1 hash
     *
     */
    public static String entryptPassword(String plainPassword) {
        byte[] salt = Digests.generateSalt(SALT_SIZE);
        byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
        return Encodes.encodeHex(salt) + Encodes.encodeHex(hashPassword);
    }

    /**
     * 验证密码
     *
     * @param plainPassword 明文密码
     * @param password 密文密码
     * @return 验证成功返回true
     */
    public static boolean validatePassword(String plainPassword, String password) {
        byte[] salt = Encodes.decodeHex(password.substring(0, 16));
        byte[] hashPassword = Digests.sha1(plainPassword.getBytes(), salt, HASH_INTERATIONS);
        return password.equals(Encodes.encodeHex(salt) + Encodes.encodeHex(hashPassword));
    }
}
