package com.cisdi.transaction.config.utils;

import ch.qos.logback.core.util.FileUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.cisdi.transaction.domain.dto.EquityFundsDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yuw
 * @version 1.0
 * @date 2022/8/9 9:31
 */
@Slf4j
public class ExportExcelUtils {

    public static MultipartFile exportExcel(HttpServletResponse response, String fileName, Class clazz, List list){
        //response.addHeader("Content-Disposition", "filename=" + fileName+".xlsx");
        //设置类型
        //response.setContentType("application/octet-stream");
        //response.setContentType("application/vnd.ms-excel");
        //response.setCharacterEncoding("utf-8");
        //XWPFDocument转FileItem
        //sizeThreshold :缓存大小
        //repository:临时文件存储位置
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        FileItem fileItem = factory.createItem("textField", "text/plain", true, fileName+".xlsx");
        OutputStream os = null;
        try {
            os = fileItem.getOutputStream();
            ExcelWriter excelWriter = EasyExcel.write(os).build();
            WriteSheet writeSheet = EasyExcel.writerSheet(fileName).build();
            writeSheet.setClazz(clazz);
            // 生成excel
            excelWriter.write(list, writeSheet);
            //关闭流
            excelWriter.finish();
            os.close();
            MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
            return multipartFile;
        } catch (IOException e) {
            log.error("导出Excel文件异常", e);
        }
        return null;
    }


    /**
     * 导入返回表格
     * @param response
     * @param fileName
     * @param clazz
     * @param list
     * @param successNumber
     * @param failNumber
     * @return
     */
    public static MultipartFile importReturnExcel(HttpServletResponse response, String fileName, Class clazz, List list,int successNumber,int failNumber) throws IOException {

        FileItemFactory factory = new DiskFileItemFactory(16, null);
        FileItem fileItem = factory.createItem("textField", "text/plain", true, fileName+".xlsx");
//        String templateFileName = ExportExcelUtils.class.getResource("/").getPath() + "templates" + File.separator + "导入返回信息模板.xlsx";
        InputStream template = new PathMatchingResourcePatternResolver().getResource("templates/导入返回信息模板.xlsx").getInputStream();
        OutputStream os = null;
        try {
            os = fileItem.getOutputStream();
            ExcelWriter excelWriter = EasyExcel.write(os).withTemplate(template).build();
            WriteSheet writeSheet = EasyExcel.writerSheet(fileName).build();
            writeSheet.setClazz(clazz);

            Map map=new HashMap();
            map.put("description","成功"+successNumber+"条,"+"失败"+failNumber+"条,失败内容如下:");
            excelWriter.fill(map,writeSheet);
            // 生成excel
            excelWriter.fill(list, writeSheet);
            //关闭流
            excelWriter.finish();
            os.close();
            MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
            return multipartFile;
        } catch (IOException e) {
            log.error("导出Excel文件异常", e);
        }
        return null;
    }
}
