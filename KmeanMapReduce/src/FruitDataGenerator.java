import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FruitDataGenerator {
    public static void main(String[] args) {
        String inputFilePath = "fruit.csv"; // Đường dẫn đến file CSV gốc
        String outputFilePath = "fruitNew.csv"; // Đường dẫn đến file CSV mới

        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;

         // Bỏ qua dòng tiêu đề đầu tiên của file gốc
            if ((line = br.readLine()) != null) {
                // Không làm gì cả, chỉ cần bỏ qua dòng này
            }
            // Đọc từng dòng còn lại
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",\\s*"); // Tách các giá trị, bỏ qua khoảng trắng

                if (values.length >= 6) { // Kiểm tra số lượng trường
                    // Lấy các giá trị từ các cột cần thiết (bỏ qua cột đầu tiên)
                    String fruitName = values[1];
                    String fruitSubtype = values[2];
                    String mass = values[3];
                    String width = values[4];
                    String height = values[5];
                    String colorScore = values[6];

                    // Ghi các giá trị vào file mới
                    writer.write(String.join(",", fruitName, fruitSubtype, mass, width, height, colorScore));
                    writer.newLine();
                }
            }

            System.out.println("Dữ liệu đã được ghi vào file: " + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}