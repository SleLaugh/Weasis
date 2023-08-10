package org.weasis.dicom.explorer.print;

import org.weasis.dicom.explorer.Messages;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印 选择项管理
 * sle 2023年8月10日11:33:15
 */
public class SelectOptionManager {
    private static class DataItem {
        private String title;
        private String value;
        private String type;

        public DataItem(String title, String value, String type) {
            this.title = title;
            this.value = value;
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public String getValue() {
            return value;
        }

        public String getType() {
            return type;
        }
    }

    private static final DataItem[] DEFAULT_ITEMS = {
            // 介质类型
            new DataItem("BLUE FILM", Messages.getString("mediumType.blue_flim"), "mediumType"),
            new DataItem("CLEAR FILM", Messages.getString("mediumType.clear_film"), "mediumType"),
            new DataItem("MAMMO CLEAR FILM", Messages.getString("mediumType.mammo_clear_film"), "mediumType"),
            new DataItem("MAMMO BLUE FILM", Messages.getString("mediumType.mammo_blue_film"), "mediumType"),
            new DataItem("PAPER", Messages.getString("mediumType.paper"), "mediumType"),
            // 优先级
            new DataItem("LOW", Messages.getString("priority.low"), "priority"),
            new DataItem("MED", Messages.getString("priority.med"), "priority"),
            new DataItem("HIGH", Messages.getString("priority.high"), "priority"),
            // 胶片终点
            new DataItem("MAGAZINE", Messages.getString("filmDestination.magazine"), "filmDestination"),
            new DataItem("PROCESSOR", Messages.getString("filmDestination.processor"), "filmDestination"),
            // 胶片定向
            new DataItem("PORTRAIT", Messages.getString("filmOrientation.portrait"), "filmOrientation"),
            new DataItem("LANDSCAPE", Messages.getString("filmOrientation.landscape"), "filmOrientation"),
            // 缩放插值类型
            new DataItem("REPLICATE", Messages.getString("magnificationType.replicate"), "magnificationType"),
            new DataItem("BILINEAR", Messages.getString("magnificationType.bilinear"), "magnificationType"),
            new DataItem("CUBIC", Messages.getString("magnificationType.cubic"), "magnificationType"),
            // 平滑类型
            new DataItem("MEDIUM", Messages.getString("smoothingType.medium"), "smoothingType"),
            new DataItem("SHARP", Messages.getString("smoothingType.sharp"), "smoothingType"),
            new DataItem("SMOOTH", Messages.getString("smoothingType.smooth"), "smoothingType"),
            // 颜色
            new DataItem("WHITE", Messages.getString("color.white"), "color"),
            new DataItem("BLACK", Messages.getString("color.black"), "color"),
            // 是否
            new DataItem("YES", Messages.getString("whether.yes"), "whether"),
            new DataItem("NO", Messages.getString("whether.no"), "whether")
    };

    public static String[] GetSelectOptions(String type) {
        List<String> values = new ArrayList<String>();
        for (DataItem item : DEFAULT_ITEMS) {
            if (item.getType().equals(type)) {
                values.add(item.getValue());
            }
        }
        return values.toArray(new String[0]);
    }

    public static String GetOptionTitle(String type, String value) {
        for (DataItem item : DEFAULT_ITEMS) {
            if (item.getType().equals(type) && item.getValue().equals(value)) {
                return item.getTitle();
            }
        }
        return null;
    }

    public static String GetFirstOptionValue(String type) {
        for (DataItem item : DEFAULT_ITEMS) {
            if (item.getType().equals(type)) {
                return item.getValue();
            }
        }
        return null;
    }
}
