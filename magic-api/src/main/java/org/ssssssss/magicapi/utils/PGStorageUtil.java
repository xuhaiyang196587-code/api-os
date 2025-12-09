package org.ssssssss.magicapi.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ssssssss.script.annotation.Comment;

public class PGStorageUtil {
	
	public static void main(String[] args) {
		List<String> region = Arrays.asList(
        		"120.665379,31.127306",
        		"120.667772,31.127554",
        		"120.667814,31.123026",
        		"120.665336,31.123054",
        		"120.665379,31.127306"
        );// 闭合多边形
		
		Map<String, Double> datas = new HashMap<>();
		datas.put("120.667772,31.137554", 1.2);
		datas.put("120.669772,31.147554", 1.9);
		datas.put("120.666000,31.125000", 1.6);
		datas.put("120.666500,31.126000", 1.4);
		datas.put("120.667000,31.124000", 1.1);
		
		List<Map<String,Object>> result = PGStorageUtil.calculateGridDataAVG(region, 0.3,datas);
		System.out.println(result);
	}

	
	@Comment("/**\r\n\n"
			+ "	 * 初始化存储网格\r\n\n"
			+ "	 * @param region\r\n\n"
			+ "	 * 			经纬度数据（闭合多边形）\r\n\n"
			+ "	 * 			['120.665379,31.127306','120.665379,31.127306'] \r\n\n"
			+ "	 * @param gridSize\r\n\n"
			+ "	 * 			存储网格的大小（0.3: 表示0.3米一个网格的大小）\r\n\n"
			+ "	 */")
	public static GridMap initGrid(List<String> region , double gridSize) {
        // 定义区域范围
        List<Point> _region = new ArrayList<>();
        region.forEach(point ->{
        	String[] segment = point.split(",");
        	_region.add(new Point(Double.valueOf(segment[0]), Double.valueOf(segment[1])));
        });
        return LocationGridAnalyzer.createGridMap(_region, gridSize);
    }
	
	
	@Comment("/**\r\n\n"
			+ "	 * 计入网格数据\r\n\n"
			+ "	 * @param gridMap\r\n\n"
			+ "	 * 			网格二维数组数据\r\n\n"
			+ "	 * 			var gridMap = PGStorageUtil.initGrid(region, gridSize);\r\n\n"
			+ "	 * @param data\r\n\n"
			+ "	 * 			数据\r\n\n"
			+ "	 * 			{\"120.667772,31.137554\": 1.2,...} \r\n\n"
			+ "	 */")
	public static void addData(GridMap gridMap, Map<String,Double> data) {
		for (Map.Entry<String,Double> entry : data.entrySet()) {
			String key = entry.getKey(); // 经纬度
			Double val = entry.getValue();// 需要存储的值
			String[] segment = key.split(",");
			 // 将值写到对应格子中
			gridMap.addLocationData(new LocationData(new Point(Double.valueOf(segment[0]), Double.valueOf(segment[1])), val));
		}
	}
	
	
	@Comment("/**\r\n\n"
			+ "	 * 自定义网格数据计算\r\n\n"
			+ "	 * @param gridMap\r\n\n"
			+ "	 * 			网格二维数组数据\r\n\n"
			+ "	 * 			var gridMap = PGStorageUtil.initGrid(region, gridSize);\r\n\n"
			+ "	 * @param customCalculate\r\n\n"
			+ "	 * 			自定义计算基于网格数据  (Grid[][] datas, int rows, int cols)->{...}\r\n"
			+ "	 */")
	public static void customCalculate(GridMap gridMap, CustomCalculate customCalculate) {
        customCalculate.callback(gridMap.getGrids(), gridMap.getRows(), gridMap.getCols());
	}
	
	
	@Comment("/**\r\n\n"
			+ "	 * 求网格数据每一格子的数据的平均值\r\n\n"
			+ "	 * @param gridMap\r\n\n"
			+ "	 * 			网格二维数组数据\r\n\n"
			+ "	 * 			var gridMap = PGStorageUtil.initGrid(region, gridSize);\r\n\n"
			+ "	 * @param customCalculate\r\n\n"
			+ "	 * 			自定义计算基于网格数据  (Grid[][] datas, int rows, int cols)->{...}\r\n"
			+ "	 */")
	public static List<Map<String,Object>> calculateAVG(GridMap gridMap) {
		return gridMap.getGridAverages();
	}
	
	@Comment("/**\r\n\n"
			+ "	 * 初始化网格并计算网格数据，取每一格的平均数\r\n\n"
			+ "	 * @param region\r\n\n"
			+ "	 * 			经纬度数据（闭合多边形）\r\n\n"
			+ "	 * 			['120.665379,31.127306','120.665379,31.127306'] \r\n\n"
			+ "	 * @param gridSize\r\n\n"
			+ "	 * 			存储网格的大小（0.3: 表示0.3米一个网格的大小）\r\n\n"
			+ "	 * @param data\r\n\n"
			+ "	 * 			数据\r\n\n"
			+ "	 * 			{\"120.667772,31.137554\": 1.2,...} \r\n\n"
			+ "	 */")
	public static List<Map<String,Object>> calculateGridDataAVG(List<String> region , double gridSize, Map<String,Double> data) {
		GridMap gridMap = PGStorageUtil.initGrid(region, gridSize);
		PGStorageUtil.addData(gridMap, data);
		List<Map<String,Object>> result = PGStorageUtil.calculateAVG(gridMap);
		return result;
	}
	
}

@FunctionalInterface
interface CustomCalculate {
	void callback(Grid[][] datas, int rows, int cols);
}
// 坐标点类
class Point {
    double longitude;
    double latitude;
    
    public Point(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
}

//格子类
class Grid {
 int row;
 int col;
 List<Double> errors = new ArrayList<>();
 List<Point> points = new ArrayList<>();
 
 public Grid(int row, int col) {
     this.row = row;
     this.col = col;
 }
 
 public void addError(double error) {
     errors.add(error);
 }
 
 public void addPoint(Point point) {
	 points.add(point);
 }
 
 public Double[] getCenterPoint() {
	 return new Double[] {
		 points.stream().map(point-> point.longitude).mapToDouble(Double::doubleValue).average().orElse(0.0)
		 ,
		 points.stream().map(point-> point.latitude).mapToDouble(Double::doubleValue).average().orElse(0.0)
	 };
}
 
 public double getAverageError() {
     if (errors.isEmpty()) {
         return 0.0;
     }
     return errors.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
 }
}

//网格地图类
class GridMap {
 private List<Point> region;
 private double minLon, minLat;
 private int rows, cols;
 private double gridSize;
 private Grid[][] grids;
 
 public GridMap(List<Point> region, double minLon, double maxLon,
                double minLat, double maxLat, int rows, int cols, double gridSize) {
     this.region = region;
     this.minLon = minLon;
//     this.maxLon = maxLon;
     this.minLat = minLat;
//     this.maxLat = maxLat;
     this.rows = rows;
     this.cols = cols;
     this.gridSize = gridSize;
     this.grids = new Grid[rows][cols];
     
     // 初始化所有格子
     for (int i = 0; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
             grids[i][j] = new Grid(i, j);
         }
     }
     
     System.out.printf("创建网格地图: %d行 × %d列, 网格大小: %.1f米\n", rows, cols, gridSize);
//     System.out.printf("区域范围: 经度[%.6f, %.6f], 纬度[%.6f, %.6f]\n",  minLon, maxLon, minLat, maxLat);
 }
 
 // 添加定位数据到对应的格子中
 public void addLocationData(LocationData data) {
     int[] gridIndex = getGridIndex(data.point);
     if (gridIndex != null && isPointInRegion(data.point)) {
         int row = gridIndex[0];
         int col = gridIndex[1];
         if (row >= 0 && row < rows && col >= 0 && col < cols) {
             grids[row][col].addError(data.error);
             grids[row][col].addPoint(new Point(data.point.longitude,data.point.latitude));
//             System.out.printf("定位点(%.6f, %.6f) 误差%.1f米 落入格子[%d,%d]\n", 
//                              data.point.longitude, data.point.latitude, 
//                              data.error, row, col);
         }
     } else {
//         System.out.printf("定位点(%.6f, %.6f) 不在区域内\n", 
//                          data.point.longitude, data.point.latitude);
     }
 }
 
 // 获取点所在的网格索引
 private int[] getGridIndex(Point point) {
     // 计算相对于区域左下角的偏移量（米）
     double offsetX = LocationGridAnalyzer.calculateDistance(minLat, minLon, minLat, point.longitude);
     double offsetY = LocationGridAnalyzer.calculateDistance(minLat, minLon, point.latitude, minLon);
     
     // 计算网格索引
     int col = (int) (offsetX / gridSize);
     int row = (int) (offsetY / gridSize);
     
     return new int[]{row, col};
 }
 
 // 判断点是否在区域内（使用射线法）
 private boolean isPointInRegion(Point point) {
     int crossings = 0;
     int n = region.size() - 1; // 最后一个点与第一个点相同
     
     for (int i = 0; i < n; i++) {
         Point p1 = region.get(i);
         Point p2 = region.get(i + 1);
         
         // 检查点是否在多边形边界上
         if (isPointOnSegment(p1, p2, point)) {
             return true;
         }
         
         // 射线与边相交检查
         if ((p1.latitude > point.latitude) != (p2.latitude > point.latitude) &&
             (point.longitude < (p2.longitude - p1.longitude) * (point.latitude - p1.latitude) / 
              (p2.latitude - p1.latitude) + p1.longitude)) {
             crossings++;
         }
     }
     
     return (crossings % 2 == 1);
 }
 
 // 判断点是否在线段上
 private boolean isPointOnSegment(Point p1, Point p2, Point point) {
     // 检查点是否在线段的边界框内
     if (point.longitude < Math.min(p1.longitude, p2.longitude) || 
         point.longitude > Math.max(p1.longitude, p2.longitude) ||
         point.latitude < Math.min(p1.latitude, p2.latitude) || 
         point.latitude > Math.max(p1.latitude, p2.latitude)) {
         return false;
     }
     
     // 检查点是否在线段上（使用叉积）
     double crossProduct = (point.latitude - p1.latitude) * (p2.longitude - p1.longitude) -
                          (point.longitude - p1.longitude) * (p2.latitude - p1.latitude);
     
     return Math.abs(crossProduct) < 1e-10;
 }
 
 // 返回所有格子的平均误差
 public List<Map<String,Object>> getGridAverages() {
//     System.out.println("\n各格子平均值:");
//     System.out.println("==================");
	 List<Map<String,Object>> datas = new ArrayList<>();
     for (int i = 0; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
             double avgError = grids[i][j].getAverageError();
             Double[] cp = grids[i][j].getCenterPoint();
             if (avgError > 0) {
            	 Map<String,Object> data = new HashMap<>();
            	 data.put("longitude", cp[0]);
            	 data.put("latitude", cp[1]);
            	 data.put("data", avgError);
            	 datas.add(data);
                 //System.out.printf("格子[%d,%d]: 平均误差 = %.2f米 (数据点数: %d)\n", i, j, avgError, grids[i][j].errors.size());
             }
         }
     }
     return datas;
 }
 
 // 获取特定格子的平均误差
 public double getGridAverageError(int row, int col) {
     if (row >= 0 && row < rows && col >= 0 && col < cols) {
         return grids[row][col].getAverageError();
     }
     return 0.0;
 }
 
 // Getter方法
 public int getRows() { return rows; }
 public int getCols() { return cols; }
 public Grid getGrid(int row, int col) { return grids[row][col]; }
 public Grid[][] getGrids() { return grids; }
}

//定位数据类
class LocationData {
 Point point;
 double error;
 
 public LocationData(Point point, double error) {
     this.point = point;
     this.error = error;
 }
}

/**
 * 主分析器类
 */
class LocationGridAnalyzer {
    // 地球半径（米）
    private static final double EARTH_RADIUS = 6371000.0;

    
    // 创建网格地图
    public static GridMap createGridMap(List<Point> region, double gridSize) {
        // 计算区域的边界
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        
        for (Point point : region) {
            minLon = Math.min(minLon, point.longitude);
            maxLon = Math.max(maxLon, point.longitude);
            minLat = Math.min(minLat, point.latitude);
            maxLat = Math.max(maxLat, point.latitude);
        }
        
        // 计算区域的宽度和高度（米）
        double width = calculateDistance(minLat, minLon, minLat, maxLon);
        double height = calculateDistance(minLat, minLon, maxLat, minLon);
        
        // 计算网格的行数和列数
        int cols = (int) Math.ceil(width / gridSize);
        int rows = (int) Math.ceil(height / gridSize);
        
        return new GridMap(region, minLon, maxLon, minLat, maxLat, rows, cols, gridSize);
    }
    
    // 计算两个经纬度点之间的距离（米）
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                  Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                  Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
}
