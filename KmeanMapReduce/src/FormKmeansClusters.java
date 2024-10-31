import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

class Point {
    double[] attributes;

    public Point(double[] attributes) {
        this.attributes = attributes;
    }

    public boolean matches(Point other) {
        if (attributes.length != other.attributes.length) return false;
        for (int i = 0; i < attributes.length; i++) {
            if (attributes[i] != other.attributes[i]) return false;
        }
        return true;
    }
}

public class FormKmeansClusters extends JFrame {

    private List<List<Point>> clusters = new ArrayList<>();
    private JTextField[] inputFields;
    private JTextArea resultArea;

    private static final String[] LABELS = {
        "fruit_label:  (1 - táo, 2 - quýt, 3 - cam, 4 - chanh)",
        "fruit_subtype:  (1 - granny-smith, 2 - mandarin, 3 - braeburn, 4 - golden_delicious, 5 - cripps_pink, 6 - spanish_jumbo, 7 - selected_seconds, 8 - turkey_navel, 9 - spanish_belsan, 10 - unknown)",
        "mass  (75 - 363)",
        "width ",
        "height",
        "color_score  (0 - 1)"
    };

    public FormKmeansClusters() {
        setTitle("Cluster Finder");
        setSize(1100, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        loadClusters("D:\\Eclipse_java\\clustered_points.txt");

        JPanel inputPanel = new JPanel(new GridLayout(LABELS.length, 2, 5, 5));
        inputFields = new JTextField[LABELS.length];

        for (int i = 0; i < LABELS.length; i++) {
            JLabel label = new JLabel(LABELS[i] + ": ");
            label.setFont(new Font("Arial", Font.PLAIN, 20));
            inputPanel.add(label);

            inputFields[i] = new JTextField();
            inputFields[i].setFont(new Font("Arial", Font.PLAIN, 20));
            inputPanel.add(inputFields[i]);
        }

        JButton findClusterButton = new JButton("Find Cluster");
        findClusterButton.setFont(new Font("Arial", Font.BOLD, 30));
        findClusterButton.setBackground(Color.DARK_GRAY);
        findClusterButton.setForeground(Color.WHITE);
        findClusterButton.addActionListener(new FindClusterAction());

        resultArea = new JTextArea(8, 40);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 20));
        resultArea.setForeground(Color.RED);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setEditable(false);

        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.add(findClusterButton, BorderLayout.NORTH);
        southPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(inputPanel, BorderLayout.NORTH);
        add(southPanel, BorderLayout.SOUTH);
    }

    private void loadClusters(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            List<Point> currentCluster = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Cluster")) {
                    if (currentCluster != null) {
                        clusters.add(currentCluster);
                    }
                    currentCluster = new ArrayList<>();
                } else if (line.startsWith("  Point:")) {
                    String[] parts = line.split(" - ")[0].split(": ")[1].split(",");
                    double[] attributes = new double[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        attributes[i] = Double.parseDouble(parts[i].trim());
                    }
                    currentCluster.add(new Point(attributes));
                }
            }
            if (currentCluster != null) {
                clusters.add(currentCluster);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading clusters from file: " + e.getMessage());
        }
    }

    private class FindClusterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            double[] inputAttributes = new double[LABELS.length];
            try {
                for (int i = 0; i < LABELS.length; i++) {
                    inputAttributes[i] = Double.parseDouble(inputFields[i].getText().trim());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(FormKmeansClusters.this, "Please enter valid numbers for all attributes.");
                return;
            }

            Point inputPoint = new Point(inputAttributes);
            int matchingCluster = -1;

            for (int i = 0; i < clusters.size(); i++) {
                for (Point clusterPoint : clusters.get(i)) {
                    if (inputPoint.matches(clusterPoint)) {
                        matchingCluster = i;
                        break;
                    }
                }
                if (matchingCluster != -1) break;
            }

            if (matchingCluster != -1) {
                resultArea.setText("The input point belongs to Cluster " + matchingCluster);
            } else {
                resultArea.setText("No matching point found in any cluster.");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FormKmeansClusters frame = new FormKmeansClusters();
            frame.setVisible(true);
        });
    }
}