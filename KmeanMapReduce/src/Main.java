import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import java.io.FileOutputStream;
public class Main extends Configured implements Tool{
	public static PointWritable[] initRandomCentroids(int kClusters, int nLineOfInputFile, String inputFilePath,
			Configuration conf) throws IOException {
		System.out.println("Initializing random " + kClusters + " centroids...");
		PointWritable[] points = new PointWritable[kClusters];

		List<Integer> lstLinePos = new ArrayList<Integer>();
		Random random = new Random();
		int pos;
		while (lstLinePos.size() < kClusters) {
			pos = random.nextInt(nLineOfInputFile);
			if (!lstLinePos.contains(pos)) {
				lstLinePos.add(pos);
			}
		}
		Collections.sort(lstLinePos);

		FileSystem hdfs = FileSystem.get(conf);
		FSDataInputStream in = hdfs.open(new Path(inputFilePath));

		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		int row = 0;
		int i = 0;
		while (i < lstLinePos.size()) {
			pos = lstLinePos.get(i);
			String point = br.readLine();
			if (row == pos) {
				points[i] = new PointWritable(point.split(","));
				i++;
			}
			row++;
		}
		br.close();
		return points;

	}

	public static void saveCentroidsForShared(Configuration conf, PointWritable[] points) {
		for (int i = 0; i < points.length; i++) {
			String centroidName = "C" + i;
			conf.unset(centroidName);
			conf.set(centroidName, points[i].toString());
		}
	}

	public static PointWritable[] readCentroidsFromReducerOutput(Configuration conf, int kClusters,
			String folderOutputPath) throws IOException, FileNotFoundException {
		PointWritable[] points = new PointWritable[kClusters];
		FileSystem hdfs = FileSystem.get(conf);
		FileStatus[] status = hdfs.listStatus(new Path(folderOutputPath));

		for (int i = 0; i < status.length; i++) {

			if (!status[i].getPath().toString().endsWith("_SUCCESS")) {
				Path outFilePath = status[i].getPath();
				System.out.println("read " + outFilePath.toString());
				BufferedReader br = new BufferedReader(new InputStreamReader(hdfs.open(outFilePath)));
				String line = null;// br.readLine();
				while ((line = br.readLine()) != null) {
					System.out.println(line);

					String[] strCentroidInfo = line.split("\t"); // Split line in K,V
					int centroidId = Integer.parseInt(strCentroidInfo[0]);
					String[] attrPoint = strCentroidInfo[1].split(",");
					points[centroidId] = new PointWritable(attrPoint);
				}
				br.close();
			}
		}

		hdfs.delete(new Path(folderOutputPath), true);

		return points;
	}

	private static boolean checkStopKMean(PointWritable[] oldCentroids, PointWritable[] newCentroids, float threshold) {
		boolean needStop = true;

		System.out.println("Check for stop K-Means if distance <= " + threshold);
		for (int i = 0; i < oldCentroids.length; i++) {

			double dist = oldCentroids[i].calcDistance(newCentroids[i]);
			System.out.println("distance centroid[" + i + "] changed: " + dist + " (threshold:" + threshold + ")");
			needStop = dist <= threshold;
			// chỉ cần 1 tâm < ngưỡng thì return false
			if (!needStop) {
				return false;
			}
		}
		return true;
	}

	private static void writeFinalResult(Configuration conf,List<List<String>> clusters, PointWritable[] centroidsFound, String outputFilePath,
			PointWritable[] centroidsInit) throws IOException {
		FileSystem hdfs = FileSystem.get(conf);
		FSDataOutputStream dos = hdfs.create(new Path(outputFilePath), true);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(dos));

		for (int i = 0; i < centroidsFound.length; i++) {
			br.write("Centroid["+i+"]: "+centroidsFound[i].toString()+" |Total Points: "+ clusters.get(i).size());
			br.newLine();
			System.out.println("Centroid[" + i + "]:  (" + centroidsFound[i] + ")  init: (" + centroidsInit[i] + ")");
		}

		br.close();
		hdfs.close();
	}
	
	private static void writeClusteredPoints(Configuration conf, List<List<String>> clusters, String outputFilePath) throws IOException {
	    FileSystem hdfs = FileSystem.get(conf);
	    FSDataOutputStream dos = hdfs.create(new Path(outputFilePath), true);
	    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(dos));

	    for (int i = 0; i < clusters.size(); i++) {
	        br.write("Cluster[" + i + "]: ");
	        br.newLine();
	        for (String pointInfo : clusters.get(i)) {
	            br.write(pointInfo);
	            br.newLine();
	        }
	        br.newLine(); // Thêm dòng trắng giữa các cụm
	    }

	    br.close(); // Đóng BufferedWriter
	    hdfs.close(); // Đóng FSDataOutputStream
	}


	public static PointWritable[] copyCentroids(PointWritable[] points) {
		PointWritable[] savedPoints = new PointWritable[points.length];
		for (int i = 0; i < savedPoints.length; i++) {
			savedPoints[i] = PointWritable.copy(points[i]);
		}
		return savedPoints;
	}

	public static int MAX_LOOP = 50;

	public static void printCentroids(PointWritable[] points, String name) {
		System.out.println("=> CURRENT CENTROIDS:");
		for (int i = 0; i < points.length; i++)
			System.out.println("centroids(" + name + ")[" + i + "]=> :" + points[i]);
		System.out.println("----------------------------------");
	}
	
	public List<List<String>> pointsOfCentroids(Configuration conf, PointWritable[] finalCentroids, String inputFilePath) throws IOException {
		List<List<String>> clusters = new ArrayList<>();
	    for (int i = 0; i < finalCentroids.length; i++) {
	        clusters.add(new ArrayList<>()); // Mỗi cụm sẽ là một danh sách các điểm
	    }

	    FileSystem hdfs = FileSystem.get(conf);
	    FSDataInputStream in = hdfs.open(new Path(inputFilePath));
	    BufferedReader br = new BufferedReader(new InputStreamReader(in));
	    String line;
	    while ((line = br.readLine()) != null) {
	    	
	        PointWritable point = new PointWritable(line.split(","));
	        double minDistance = Double.MAX_VALUE;
	        int closestCentroid = -1;

	        for (int i = 0; i < finalCentroids.length; i++) {
	            double distance = point.calcDistance(finalCentroids[i]);
	            if (distance < minDistance) {
	                minDistance = distance;
	                closestCentroid = i;
	            }
	        }
	        clusters.get(closestCentroid).add("  Point: " + point + " - distance: " + minDistance );
	    }
	    br.close();

	    for (int i = 0; i < clusters.size(); i++) {
	        System.out.println("Centroid[" + i + "] => " + finalCentroids[i] + " :");
	        for (String pointInfo : clusters.get(i)) {
	            System.out.println(pointInfo);
	        }
	        System.out.println("----------------------------------");
	    }
	    return clusters;
	}
	private static PointWritable parsePoint(String pointInfo) {
	    // Tách phần chứa tọa độ của điểm và bỏ qua các phần không cần thiết
	    String[] parts = pointInfo.split(":")[1].split("-")[0].trim().split(",");
	    float[] coordinates = new float[parts.length];
	    
	    for (int i = 0; i < parts.length; i++) {
	        try {
	            // Chỉ lấy phần số, bỏ qua các ký tự không phải số
	            String numericPart = parts[i].replaceAll("[^0-9.-]", "");
	            coordinates[i] = Float.parseFloat(numericPart);
	        } catch (NumberFormatException e) {
	            // Log lỗi hoặc xử lý tùy theo nhu cầu
	            System.err.println("Error parsing number: " + parts[i]);
	            throw e;
	        }
	    }
	    
	    return new PointWritable(coordinates);
	}
	private static double calculateSilhouette(List<List<String>> clusters, PointWritable[] centroids) {
	    double totalSilhouette = 0.0;
	    int totalPoints = 0;

	    for (int i = 0; i < clusters.size(); i++) {
	        List<String> cluster = clusters.get(i);
	        for (String pointInfo : cluster) {
	        	PointWritable point = parsePoint(pointInfo);
	            double a = calculateAverageDistance(point, cluster, centroids[i]);
	            double b = Double.MAX_VALUE;
	            
	            // Find the closest cluster's centroid distance (b)
	            for (int j = 0; j < centroids.length; j++) {
	                if (i != j) {
	                    double avgDistToOtherCluster = calculateAverageDistance(point, clusters.get(j), centroids[j]);
	                    b = Math.min(b, avgDistToOtherCluster);
	                }
	            }
	            
	            // Calculate Silhouette for the current point
	            double s = (b - a) / Math.max(a, b);
	            totalSilhouette += s;
	            totalPoints++;
	        }
	    }
	    
	    // Average Silhouette score
	    return totalSilhouette / totalPoints;
	}

	private static double calculateAverageDistance(PointWritable point, List<String> cluster, PointWritable centroid) {
	    double totalDistance = 0.0;
	    for (String otherPointInfo : cluster) {
	        PointWritable otherPoint = parsePoint(otherPointInfo);
	        totalDistance += point.calcDistance(otherPoint);
	    }
	    return totalDistance / cluster.size();
	}

	private static double calculateDaviesBouldin(List<List<String>> clusters, PointWritable[] centroids) {
	    double totalDBIndex = 0.0;
	    
	    for (int i = 0; i < clusters.size(); i++) {
	        double maxRatio = Double.MIN_VALUE;
	        for (int j = 0; j < clusters.size(); j++) {
	            if (i != j) {
	                double s_i = calculateClusterScatter(clusters.get(i), centroids[i]);
	                double s_j = calculateClusterScatter(clusters.get(j), centroids[j]);
	                double m_ij = centroids[i].calcDistance(centroids[j]);
	                
	                double ratio = (s_i + s_j) / m_ij;
	                maxRatio = Math.max(maxRatio, ratio);
	            }
	        }
	        totalDBIndex += maxRatio;
	    }
	    
	    return totalDBIndex / clusters.size();
	}

	private static double calculateClusterScatter(List<String> cluster, PointWritable centroid) {
	    double scatter = 0.0;
	    for (String pointInfo : cluster) {
	        PointWritable point = parsePoint(pointInfo);
	        scatter += centroid.calcDistance(point);
	    }
	    return scatter / cluster.size();
	}

	private static void writeEvaluationMetrics(Configuration conf, List<List<String>> clusters, PointWritable[] centroids, String outputFilePath) throws IOException {
	    double silhouetteScore = calculateSilhouette(clusters, centroids);
	    double daviesBouldinIndex = calculateDaviesBouldin(clusters, centroids);
	    
	    FileSystem hdfs = FileSystem.get(conf);
	    FSDataOutputStream dos = hdfs.create(new Path(outputFilePath), true);
	    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(dos));

	    br.write("Silhouette Score: " + silhouetteScore);
	    br.newLine();
	    br.write("Davies-Bouldin Index: " + daviesBouldinIndex);
	    br.newLine();

	    br.close();
	    hdfs.close();
	}
	private static void writeClusteredPointsToLocal(List<List<String>> clusters, String outputFilePath) throws IOException {
	    FileOutputStream fos = new FileOutputStream(outputFilePath); // Ghi vào hệ thống file cục bộ
	    BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));

	    for (int i = 0; i < clusters.size(); i++) {
	        br.write("Cluster[" + i + "]: ");
	        br.newLine();
	        for (String pointInfo : clusters.get(i)) {
	            br.write(pointInfo);
	            br.newLine();
	        }
	        br.newLine(); // Thêm dòng trắng giữa các cụm
	    }

	    br.close(); // Đóng BufferedWriter
	    fos.close(); // Đóng FileOutputStream
	}
	

	public int run(String[] args) throws Exception {

		Configuration conf = getConf();
		String inputFilePath = conf.get("in", null);
		String outputFolderPath = conf.get("out", null);
		String outputFileName = conf.get("result", "result.txt");

		int nClusters = conf.getInt("k", 2);
		float thresholdStop = conf.getFloat("thresh", 0.001f);
		int numLineOfInputFile = conf.getInt("lines", 0);
		MAX_LOOP = conf.getInt("maxloop", 50);
		int nReduceTask = conf.getInt("NumReduceTask", 1);
		if (inputFilePath == null || outputFolderPath == null || numLineOfInputFile == 0) {
			System.err.printf(
					"Usage: %s -Din <input file name> -Dlines <number of lines in input file> -Dout <Folder ouput> -Dresult <output file result> -Dk <number of clusters> -Dthresh <Threshold>\n",
					getClass().getSimpleName());
			ToolRunner.printGenericCommandUsage(System.err);
			return -1;
		}
		System.out.println("---------------INPUT PARAMETERS---------------");
		System.out.println("inputFilePath:" + inputFilePath);
		System.out.println("outputFolderPath:" + outputFolderPath);
		System.out.println("outputFileName:" + outputFileName);
		System.out.println("maxloop:" + MAX_LOOP);
		System.out.println("numLineOfInputFile:" + numLineOfInputFile);
		System.out.println("nClusters:" + nClusters);
		System.out.println("threshold:" + thresholdStop);
		System.out.println("NumReduceTask:" + nReduceTask);

		System.out.println("--------------- STATR ---------------");
		PointWritable[] oldCentroidPoints = initRandomCentroids(nClusters, numLineOfInputFile, inputFilePath, conf);
		PointWritable[] centroidsInit = copyCentroids(oldCentroidPoints);
		printCentroids(oldCentroidPoints, "init");
		saveCentroidsForShared(conf, oldCentroidPoints);
		int nLoop = 0;

		PointWritable[] newCentroidPoints = null;
		long t1 = (new Date()).getTime();
		while (true) {
			nLoop++;
			if (nLoop == MAX_LOOP) {
				break;
			}
			Job job = new Job(conf, "K-Mean");// Job thực hiện deepCopy conf
			job.setJarByClass(Main.class);
			job.setMapperClass(KMapper.class);
			job.setCombinerClass(Combiner.class);
			job.setReducerClass(KReducer.class);
			job.setMapOutputKeyClass(LongWritable.class);
			job.setMapOutputValueClass(PointWritable.class);
			job.setOutputKeyClass(Text.class);
			job.setOutputValueClass(Text.class);

			FileInputFormat.addInputPath(job, new Path(inputFilePath));

			FileOutputFormat.setOutputPath(job, new Path(outputFolderPath));
			job.setOutputFormatClass(TextOutputFormat.class);

			job.setNumReduceTasks(nReduceTask);

			boolean ret = job.waitForCompletion(true);
			if (!ret) {
				return -1;

			}

			newCentroidPoints = readCentroidsFromReducerOutput(conf, nClusters, outputFolderPath);
			printCentroids(newCentroidPoints, "new");
			boolean needStop = checkStopKMean(newCentroidPoints, oldCentroidPoints, thresholdStop);

			oldCentroidPoints = copyCentroids(newCentroidPoints);

			if (needStop) {
				break;
			} else {
				saveCentroidsForShared(conf, newCentroidPoints);
			}

		}
		if (newCentroidPoints != null) {
			System.out.println("------------------- FINAL RESULT -------------------");
			List<List<String>> clusters = pointsOfCentroids(conf, newCentroidPoints, inputFilePath);
			writeFinalResult(conf, clusters, newCentroidPoints, outputFolderPath + "/" + outputFileName, centroidsInit);
			
			//pointsOfCentroids(conf, newCentroidPoints, inputFilePath);
		    
		    writeClusteredPoints(conf, clusters, outputFolderPath + "/clustered_points.txt");
		 // Ghi kết quả đánh giá Silhouette và Davies-Bouldin
		    writeEvaluationMetrics(conf, clusters, newCentroidPoints, outputFolderPath + "/evaluation_metrics.txt");
		    writeClusteredPointsToLocal(clusters, "D:\\Eclipse_java\\clustered_points.txt");
		}
		System.out.println("----------------------------------------------");
		System.out.println("K-MEANS CLUSTERING FINISHED!");
		System.out.println("Loop:" + nLoop);
		System.out.println("Time:" + ((new Date()).getTime() - t1) + "ms");

		return 1;
	}

	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new Main(), args);
		System.exit(exitCode);
	}
}