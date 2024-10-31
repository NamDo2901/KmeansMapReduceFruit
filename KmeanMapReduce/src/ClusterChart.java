import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClusterChart extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Phân Phối Điểm Trong Các Cụm");

        // Định nghĩa các trục
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Các Cụm");
        yAxis.setLabel("Số Điểm");

        // Tạo biểu đồ cột
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Phân Phối Điểm Trong Các Cụm");

        // Đọc dữ liệu và làm đầy biểu đồ
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Số Điểm");

        Map<String, Integer> dataset = readClusterData("D:\\Eclipse_java\\clustered_points.txt");

        // Mảng màu để gán cho các cụm
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
            Color.ORANGE, Color.PURPLE, Color.PINK, Color.BROWN
        };

        // Sử dụng AtomicInteger để theo dõi chỉ số màu
        AtomicInteger index = new AtomicInteger(0);

        // Thêm dữ liệu vào biểu đồ với màu sắc
        for (Map.Entry<String, Integer> entry : dataset.entrySet()) {
            String cluster = entry.getKey();
            Integer count = entry.getValue();

            // Tạo điểm dữ liệu cho biểu đồ
            XYChart.Data<String, Number> dataPoint = new XYChart.Data<>(cluster, count);
            series.getData().add(dataPoint);

            // Thay đổi màu sắc cho cột
            dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    // Gán màu cho cột
                    newNode.setStyle("-fx-bar-fill: " + toRgbString(colors[index.getAndIncrement() % colors.length]) + ";");
                }
            });
        }

        barChart.getData().add(series);

        // Thiết lập cảnh và giai đoạn
        Scene scene = new Scene(barChart, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private String toRgbString(Color color) {
        return String.format("rgb(%d, %d, %d)", 
            (int)(color.getRed() * 255), 
            (int)(color.getGreen() * 255), 
            (int)(color.getBlue() * 255));
    }

    private Map<String, Integer> readClusterData(String filePath) {
        Map<String, Integer> dataset = new HashMap<>();
        String currentCluster = null;
        int pointCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim(); // Loại bỏ khoảng trắng
                if (line.startsWith("Cluster")) {
                    // Nếu có cụm hiện tại, thêm số điểm vào dữ liệu
                    if (currentCluster != null) {
                        dataset.put(currentCluster, pointCount);
                    }
                    currentCluster = line.split(":")[0].trim(); // e.g., "Cluster[0]"
                    pointCount = 0; // Đặt lại số điểm cho cụm mới
                } else if (line.startsWith("Point") && currentCluster != null) {
                    // Tăng số điểm cho cụm hiện tại
                    pointCount++;
                }
            }
            // Thêm số điểm của cụm cuối cùng vào dữ liệu
            if (currentCluster != null) {
                dataset.put(currentCluster, pointCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataset;
    }

    public static void main(String[] args) {
        launch(args); // Khởi chạy ứng dụng JavaFX
    }
}
