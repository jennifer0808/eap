package cn.tzauto.octopus.common.util.tool;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class dragUtil {

    //窗体拉伸属性
    private static boolean isRight;// 是否处于右边界调整窗口状态
    private static boolean isBottomRight;// 是否处于右下角调整窗口状态
    private static boolean isBottom;// 是否处于下边界调整窗口状态
    private static boolean isLeft;// 是否处于右边界调整窗口状态
    private static boolean isBottomLeft;// 是否处于右下角调整窗口状态
    private static boolean isTop;// 是否处于下边界调整窗口状态
    private final static int RESIZE_WIDTH = 5;// 判定是否为调整窗口状态的范围与边界距离
    private final static double MIN_WIDTH = 1024;// 窗口最小宽度
    private final static double MIN_HEIGHT = 630;// 窗口最小高度

    public static void addDrawFunc(Stage stage, VBox vbox,GridPane root) {


        vbox.setOnMouseMoved((MouseEvent event) -> {
            event.consume();
            double x = event.getSceneX();
            double y = event.getSceneY();
            double width = stage.getWidth();
            double height = stage.getHeight();
            Cursor cursorType = Cursor.DEFAULT;// 鼠标光标初始为默认类型，若未进入调整窗口状态，保持默认类型
            // 先将所有调整窗口状态重置
            isRight = isBottomRight = isBottom = false;
            if (y >= height - RESIZE_WIDTH) {
                if (x <= RESIZE_WIDTH) {// 左下角调整窗口状态
//                    isBottomLeft=true;
//                    cursorType = Cursor.SW_RESIZE;
                } else if (x >= width - RESIZE_WIDTH) {// 右下角调整窗口状态
                    isBottomRight = true;
                    cursorType = Cursor.SE_RESIZE;
                } else {// 下边界调整窗口状态
                    isBottom = true;
                    cursorType = Cursor.S_RESIZE;
                }
            } else if (x >= width - RESIZE_WIDTH) {// 右边界调整窗口状态
                isRight = true;
                cursorType = Cursor.E_RESIZE;
            }
            // 最后改变鼠标光标
            vbox.setCursor(cursorType);
        });

        vbox.setOnMouseDragged((MouseEvent event) -> {
            double x = event.getSceneX();
            double y = event.getSceneY();
            // 保存窗口改变后的x、y坐标和宽度、高度，用于预判是否会小于最小宽度、最小高度
            double nextX = stage.getX();
            double nextY = stage.getY();
            double nextWidth = stage.getWidth();
            double nextHeight = stage.getHeight();
            if (isRight || isBottomRight ) {// 所有右边调整窗口状态
                nextWidth = x;
            }
            if (isBottomRight || isBottom ) {// 所有下边调整窗口状态
                nextHeight = y;
            }
            if(isLeft||isBottomLeft){

            }
            if (nextWidth <= MIN_WIDTH) {// 如果窗口改变后的宽度小于最小宽度，则宽度调整到最小宽度
                nextWidth = MIN_WIDTH;
            }
            if (nextHeight <= MIN_HEIGHT) {// 如果窗口改变后的高度小于最小高度，则高度调整到最小高度
                nextHeight = MIN_HEIGHT;
            }
            // 最后统一改变窗口的x、y坐标和宽度、高度，可以防止刷新频繁出现的屏闪情况
            stage.setX(nextX);
            stage.setY(nextY);
            stage.setWidth(nextWidth);
            stage.setHeight(nextHeight);
            root.setPrefHeight(stage.getHeight());
        });
    }
    public static void addDragListener(Stage stage, Node root) {
      new  DragListener(stage).enableDrag(root);
    }
    /**
     * 拖拽监听器
     * @author Light
     */
    public static class DragListener implements EventHandler<MouseEvent> {

        private double xOffset = 0;
        private double yOffset = 0;
        private final Stage stage;

        public DragListener(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(MouseEvent event) {
            event.consume();
            if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                stage.setX(event.getScreenX() - xOffset);
                if(event.getScreenY() - yOffset < 0) {
                    stage.setY(0);
                }else {
                    stage.setY(event.getScreenY() - yOffset);
                }
            }
        }

        public void enableDrag(Node node) {
            node.setOnMousePressed(this);
            node.setOnMouseDragged(this);
        }
    }
}
