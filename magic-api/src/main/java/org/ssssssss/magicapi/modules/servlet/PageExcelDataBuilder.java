package org.ssssssss.magicapi.modules.servlet;

import java.util.List;
import java.util.Map;

@FunctionalInterface
interface PageExcelDataBuilder {
	List<Map<String, Object>> get(int pageNum, int pageSize);
}
