package cn.tzauto.octopus.common.util.tool;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class SwingUtil {

    public static final Color LIST_OPTION_BORDER = new Color(122, 138, 151);
    //窗口外观风格常量
    public static final int SYSTEM_STYLE = '0';
    public static final int JAVA_STYLE = '1';
    public static final int METAL_STYLE = '2';

    /**=======================================================================**
     *      [## private sunswing() {} ]:        构造函数
     *          参数   ：无
     *          返回值 ：无
     *          修饰符 ：private
     *          功能   ：防止实例化sunswing对象
     **=======================================================================**
     */
    private SwingUtil() {
    }

    /**=======================================================================**
     *      [## public static void setWindowCenter(Component cp) {} ]:  
     *          参数   ：Component对象,表示要居中的窗口
     *          返回值 ：无
     *          修饰符 ：public static 可以不实例化对象而直接调用方法
     *          功能   ：设置窗口在屏幕居中
     **=======================================================================**
     */
    public static void setWindowCenter(Component cp) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        cp.setLocation(screenSize.width / 2 - (cp.getWidth() / 2),
                screenSize.height / 2 - (cp.getHeight() / 2));
    }

    /**
     * 
     * <一句话功能简述>
     * <功能详细描述>
     * @see [类、类#方法、类#成员]
     */
    public static boolean alertNullInfomation(Component parentComponent, JTextField field, String meassage, String title) {
        if (field == null || field.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(parentComponent, meassage, title, JOptionPane.INFORMATION_MESSAGE);
            field.requestFocus();
            return false;
        }
        return true;
    }
}
