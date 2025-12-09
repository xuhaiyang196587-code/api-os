package org.ssssssss.magicapi.modules.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.context.RequestContext;
import org.ssssssss.magicapi.core.interceptor.ResultProvider;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import org.ssssssss.magicapi.modules.servlet.utils.RequestGlobal;
import org.ssssssss.script.annotation.Comment;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;

/**
 * response模块
 *
 * @author mxd
 */
@MagicModule("response")
public class ResponseModule {

	private final ResultProvider resultProvider;

	public ResponseModule(ResultProvider resultProvider) {
		this.resultProvider = resultProvider;
	}

	/**
	 * 文件下载
	 *
	 * @param value    文件内容
	 * @param filename 文件名
	 */
	@Comment("文件下载")
	public static ResponseEntity<?> download(@Comment(name = "value", value = "文件内容，如`byte[]`") byte[] value,
											 @Comment(name = "filename", value = "文件名") String filename) throws UnsupportedEncodingException {
		ContentDisposition contentDisposition = ContentDisposition.attachment()
		        .filename(filename, StandardCharsets.UTF_8)  // 自动处理编码和浏览器兼容
		        .build();
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
				.body(value);
	}
 
	@Comment("文件下载")
	public static ResponseEntity<?> download(
			@Comment(name = "filename", value = "下载保存的文件名") String filename,
			@Comment(name = "urls", value = "文件路径集合，如: aa/bb/cc.txt,...") String[] urls
	) throws Exception {
		 return _download(filename, urls);
	}
	@Comment("文件下载")
	public static ResponseEntity<?> download(@Comment(name = "urls", value = "文件路径集合，如: aa/bb/cc.txt,...") String[] urls) throws Exception {
		 return _download(null, urls);
	}
	

	 private static ResponseEntity<?> _download(String filename, String... _urls) throws Exception {
	        // 参数校验
	        if(_urls == null || _urls.length == 0) {
	            throw new IllegalArgumentException("未指定下载文件路径");
	        }

	        List<File> files = new ArrayList<>();
	        Map<String, Integer> nameCounter = new HashMap<>();
	        
	        for (String url : _urls) {
	            String decodedUrl = URLDecoder.decode(url, "UTF-8");
	            File file = new File(RequestGlobal.getUserFilesBaseDir(), decodedUrl);
	            if(!file.exists()) {
	                throw new FileNotFoundException("文件不存在: " + decodedUrl);
	            }
	            
	            // 处理重复文件名
	            String originalName = file.getName();
	            int count = nameCounter.getOrDefault(originalName, 0);
	            nameCounter.put(originalName, count + 1);
	            files.add(file);
	        }

	        // 文件名生成逻辑
	        if(filename == null) {
	            filename = files.size() > 1 ? "附件.zip" : files.get(0).getName();
	        } else if(files.size() > 1 && !filename.endsWith(".zip")) {
	            filename += ".zip";
	        }

	        ContentDisposition contentDisposition = ContentDisposition.attachment()
	                .filename(filename, StandardCharsets.UTF_8)
	                .build();

	        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
	            if (files.size() > 1) {
	                ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);
	                Map<String, Integer> entryCount = new HashMap<>();
	                
	                for (File file : files) {
	                    String entryName = file.getName();
	                    int count = entryCount.getOrDefault(entryName, 0);
	                    if(count > 0) {
	                        // 处理重复文件名：添加(1),(2)等后缀
	                        int dotIndex = entryName.lastIndexOf('.');
	                        if(dotIndex > 0) {
	                            entryName = entryName.substring(0, dotIndex) + 
	                                      "(" + count + ")" + 
	                                      entryName.substring(dotIndex);
	                        } else {
	                            entryName += "(" + count + ")";
	                        }
	                    }
	                    entryCount.put(file.getName(), count + 1);
	                    
	                    ZipEntry entry = new ZipEntry(entryName);
	                    zos.putNextEntry(entry);
	                    Files.copy(file.toPath(), zos);
	                    zos.closeEntry();
	                }
	                zos.finish();
	                return ResponseEntity.ok()
	                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
	                        .header("Content-Disposition", contentDisposition.toString())
	                        .body(baos.toByteArray());
	            } else {
	                Path path = files.get(0).toPath();
	                return ResponseEntity.ok()
	                        .contentType(MediaType.parseMediaType(Files.probeContentType(path)))
	                        .header("Content-Disposition", contentDisposition.toString())
	                        .body(new ByteArrayResource(Files.readAllBytes(path)));
	            }
	        }
	    }

	 @Comment("导出excel")
		public NullValue exportExcel(
				@Comment(name = "fileName", value = "导出的文件名称") String fileName,
				@Comment(name = "title", value = "第一行大标题") String title,
				@Comment(name = "dataBuilder", value = "数据构造器()->{... return datas;}") PageExcelDataBuilder dataBuilder,
				@Comment(name = "appendHeader", value = "从第二行开始追加表头，格式：开始列,结束列,描述内容;...") String... appendHeader
				) throws Exception {
		 return exportPageExcel(fileName, title, dataBuilder, 1,1,1,appendHeader);
	 }
 
		@Comment("导出分页excel")
		public NullValue exportPageExcel(@Comment(name = "fileName", value = "导出的文件名称") String fileName,
				@Comment(name = "title", value = "第一行大标题") String title,
				@Comment(name = "pageExcelDataBuilder", value = "分页数据构造器(pageNum,pageSize)->{... return datas;}\n pageNum / pageSize 值都 >= 1 ") PageExcelDataBuilder pageExcelDataBuilder,
				@Comment(name = "pageSize", value = "每页显示的数据条数，最小值为1") int pageSize,
				@Comment(name = "pageNum", value = "当前第几页，最小值为1") int pageNum,
				@Comment(name = "total", value = "数据总条数，最小值为1") int total,
				@Comment(name = "appendHeader", value = "从第二行开始追加表头，格式：开始列,结束列,描述内容;...") String... appendHeader)
				throws Exception {

			MagicHttpServletResponse response = getResponse();
			String excelName = DateUtil.format(DateUtil.date(), "yyyy_MM_dd") + "_"
					+ fileName.replaceAll(".xlsx", "").replaceAll(".xls", "");
			String encodedFileName = URLEncoder.encode(excelName, StandardCharsets.UTF_8).replaceAll("\\+", "%20"); // 空格替换

			// 创建 SXSSFWorkbook 实例（Hutool 底层使用）
			try (SXSSFWorkbook workbook = new SXSSFWorkbook(); OutputStream out = response.getOutputStream()) {

				// 创建基础样式
				CellStyle titleStyle = workbook.createCellStyle();
				titleStyle.setAlignment(HorizontalAlignment.CENTER);
				titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

				Font titleFont = workbook.createFont();
				titleFont.setBold(true);
				titleFont.setFontHeightInPoints(Short.parseShort("16"));
				titleStyle.setFont(titleFont);

				// 表头样式
				CellStyle headerStyle = workbook.createCellStyle();
				headerStyle.setAlignment(HorizontalAlignment.CENTER);

				Font headerFont = workbook.createFont();
				headerFont.setBold(true);
				titleFont.setFontHeightInPoints(Short.parseShort("14"));
				headerStyle.setFont(headerFont);

				// 添加边框
				cssBorder(headerStyle);

				// 数据单元格样式
				CellStyle dataStyle = workbook.createCellStyle();
				cssBorder(dataStyle);
				 
				if (pageNum < 1 || pageSize < 1 || total < 1) {
					throw new Exception("pageNum / pageSize / total 值均为 >=1 的数值！");
				}
				while ((pageNum - 1) * pageSize < total) {
					List<Map<String, Object>> list = pageExcelDataBuilder.get(pageNum, pageSize);
					if (list == null || list.isEmpty())
						break;
					Map<String, String> headerAlias = new LinkedHashMap<>();
					list.get(0).keySet().forEach(field -> {
						headerAlias.put(field, field);
					});

					SXSSFSheet sheet = workbook.createSheet("第" + (pageNum) + "页");

					// 在创建sheet后立即跟踪所有列
					sheet.trackAllColumnsForAutoSizing();

					// 创建标题行
					Row titleRow = sheet.createRow(0);
					titleRow.setHeightInPoints(30);
					Cell titleCell = titleRow.createCell(0);
					titleCell.setCellValue(title + " - 第" + (pageNum) + "页");

					// 合并标题行单元格
					CellRangeAddress titleRegion = new CellRangeAddress(0, 0, 0, headerAlias.size() - 1);
					sheet.addMergedRegion(titleRegion);
					titleCell.setCellStyle(titleStyle);

					mergeCellCssBoder(sheet, titleRegion);

					int nextStartIdx = 1;
					if (appendHeader != null && appendHeader.length > 0) {
						nextStartIdx = appendHeader.length + 1;
						for (int i = 0; i < appendHeader.length; i++) {
							String cellRangeAddress = appendHeader[i];
							String[] cras = cellRangeAddress.split(";");
							Row titleRow1 = sheet.createRow(i + 1);
							for (int j = 0; j < cras.length; j++) {
								String cra = cras[j];
								String[] craInfo = cra.split(",");
								if (craInfo.length != 3) {
									throw new Exception("cellRangeAddresses 配置存在结构问题！");
								}
								titleRow1.setHeightInPoints(30);
								Cell titleCell1 = titleRow1.createCell(Integer.parseInt(craInfo[0]));
								titleCell1.setCellValue(craInfo[2]);

								// 合并标题行单元格
								CellRangeAddress titleRegion1 = new CellRangeAddress(i + 1, i + 1,
										Integer.parseInt(craInfo[0]), Integer.parseInt(craInfo[1]));
								sheet.addMergedRegion(titleRegion1);
								titleCell1.setCellStyle(titleStyle);

								// 为合并后的标题单元格设置边框
								mergeCellCssBoder(sheet, titleRegion1);
							}
						}
					}

					// 创建表头行
					Row headerRow = sheet.createRow(nextStartIdx);
					int colIndex = 0;
					for (String header : headerAlias.values()) {
						Cell cell = headerRow.createCell(colIndex);
						cell.setCellValue(header);
						cell.setCellStyle(headerStyle);
						// 设置初始列宽
						sheet.setColumnWidth(colIndex, 20 * 256); // 初始宽度20字符
						colIndex++;
					}

					// 写入数据行
					int rowIndex = nextStartIdx + 1; // 数据从第2行开始（0=标题行, 1=表头行）
					for (Map<String, Object> rowData : list) {
						Row dataRow = sheet.createRow(rowIndex);
						colIndex = 0;
						for (String field : headerAlias.keySet()) {
							Object value = rowData.get(field);
							Cell cell = dataRow.createCell(colIndex);
							cell.setCellValue(value != null ? value.toString() : "");
							cell.setCellStyle(dataStyle);
							colIndex++;
						}
						rowIndex++;
					}

					// 自动调整列宽（在所有数据写入后执行）
					for (int i = 0; i < headerAlias.size(); i++) {
						sheet.trackColumnForAutoSizing(i); // 跟踪每一列
						sheet.autoSizeColumn(i);
						// 限制最大宽度并增加一些padding
						int currentWidth = sheet.getColumnWidth(i);
						sheet.setColumnWidth(i, Math.min(currentWidth + 512, 50 * 256));// 最大50字符宽度
					}

					// 清空当前页数据以释放内存
					list.clear();
					list = null;
					pageNum++;
				}

				response.setHeader("Content-Type", "application/vnd.ms-excel;charset=utf-8");
				response.setHeader("Content-Disposition", "attachment;filename=" + encodedFileName + ".xlsx");

				workbook.write(out);
				out.flush();

				// 7. 清理资源
				workbook.dispose(); // 清理临时文件
				workbook.close();
				IoUtil.close(out);
			}
			return end();
		}

		private void mergeCellCssBoder(SXSSFSheet sheet, CellRangeAddress cellRangeAddress) {
			RegionUtil.setBorderTop(BorderStyle.THIN, cellRangeAddress, sheet);
			RegionUtil.setBorderRight(BorderStyle.THIN, cellRangeAddress, sheet);
			RegionUtil.setBorderBottom(BorderStyle.THIN, cellRangeAddress, sheet);
			RegionUtil.setBorderLeft(BorderStyle.THIN, cellRangeAddress, sheet);
			RegionUtil.setTopBorderColor(IndexedColors.DARK_BLUE.index, cellRangeAddress, sheet);
			RegionUtil.setRightBorderColor(IndexedColors.DARK_BLUE.index, cellRangeAddress, sheet);
			RegionUtil.setBottomBorderColor(IndexedColors.DARK_BLUE.index, cellRangeAddress, sheet);
			RegionUtil.setLeftBorderColor(IndexedColors.DARK_BLUE.index, cellRangeAddress, sheet);
		}

		private void cssBorder(CellStyle cellStyle) {
			cellStyle.setBorderTop(BorderStyle.THIN);
			cellStyle.setBorderRight(BorderStyle.THIN);
			cellStyle.setBorderBottom(BorderStyle.THIN);
			cellStyle.setBorderLeft(BorderStyle.THIN);
			cellStyle.setTopBorderColor(IndexedColors.DARK_BLUE.index);
			cellStyle.setLeftBorderColor(IndexedColors.DARK_BLUE.index);
			cellStyle.setBottomBorderColor(IndexedColors.DARK_BLUE.index);
			cellStyle.setRightBorderColor(IndexedColors.DARK_BLUE.index);
		}
	
	/**
	 * 自行构建分页结果
	 *
	 * @param total  条数
	 * @param values 数据内容
	 */
	@Comment("返回自定义分页结果")
	public Object page(@Comment(name = "total", value = "总条数") long total,
			@Comment(name = "values", value = "当前结果集") List<Map<String, Object>> values) {
		return resultProvider.buildPageResult(RequestContext.getRequestEntity(), null, total, values);
	}

	/**
	 * 自定义json结果
	 *
	 * @param value json内容
	 */
	@Comment("自定义返回json内容")
	public ResponseEntity<Object> json(@Comment(name = "value", value = "返回对象") Object value) {
		return ResponseEntity.ok(value);
	}

	/**
	 * 添加Header
	 */
	@Comment("添加response header")
	public ResponseModule addHeader(@Comment(name = "key", value = "header名") String key,
									@Comment(name = "value", value = "header值") String value) {
		if (StringUtils.isNotBlank(key)) {
			MagicHttpServletResponse response = getResponse();
			if (response != null) {
				response.addHeader(key, value);
			}
		}
		return this;
	}

	/**
	 * 设置header
	 */
	@Comment("设置response header")
	public ResponseModule setHeader(@Comment(name = "key", value = "header名") String key,
									@Comment(name = "value", value = "header值") String value) {
		if (StringUtils.isNotBlank(key)) {
			MagicHttpServletResponse response = getResponse();
			if (response != null) {
				response.setHeader(key, value);
			}
		}
		return this;
	}

	/**
	 * 获取OutputStream
	 *
	 * @since 1.2.3
	 */
	@Comment("获取OutputStream")
	public OutputStream getOutputStream() throws IOException {
		MagicHttpServletResponse response = getResponse();
		return response.getOutputStream();
	}


	@Comment("终止输出，执行此方法后不会对结果进行任何输出及处理")
	public NullValue end() {
		return NullValue.INSTANCE;
	}

	private MagicHttpServletResponse getResponse() {
		return RequestContext.getHttpServletResponse();
	}

	/**
	 * 展示图片
	 *
	 * @param value 图片内容
	 * @param mime  图片类型，image/png,image/jpeg,image/gif
	 */
	@Comment("输出图片")
	public ResponseEntity image(@Comment(name = "value", value = "图片内容，如`byte[]`") Object value,
								@Comment(name = "mime", value = "图片类型，如`image/png`、`image/jpeg`、`image/gif`") String mime) {
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, mime).body(value);
	}

	/**
	 * 输出文本
	 *
	 * @param text 文本内容
	 */
	@Comment("输出文本")
	public ResponseEntity text(@Comment(name = "text", value = "文本内容") String text) {
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE).body(text);
	}

	/**
	 * 重定向
	 *
	 * @param url 目标网址
	 */
	@Comment("重定向")
	public NullValue redirect(@Comment(name = "url", value = "目标网址") String url) throws IOException {
		getResponse().sendRedirect(url);
		return NullValue.INSTANCE;
	}

	public static class NullValue {
		static final NullValue INSTANCE = new NullValue();
	}
}
