package cn.tzauto.octopus.isecsLayer.resolver.ace;


import cn.tzauto.octopus.biz.recipe.dao.RecipeTemplateMapper;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

public class ACEResolver {

    private static Logger logger = Logger.getLogger(ACEResolver.class);

    private String csvPath;

    private List<List<String>> dataList;

    public ACEResolver(String csvPath) {
        this.csvPath = csvPath;
    }

    /**
     * 查询csv表头信息
     *
     * @return
     */
    public String[] getMetaData() throws IOException {
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath),"GBK"));
        ) {
            String line = bufferedReader.readLine();
            return line.split(",");
        } catch (Exception e) {
            logger.info("get metaData failed" + e);
            throw new IOException();
        }
    }

    /**
     * 查询csv表头列明的索引
     *
     * @return
     */
    public Map<String, Integer> getColumnNameIndex() throws IOException {
        Map<String, Integer> map = new HashMap<>();
        String[] metaData = getMetaData();
        for (int i = 0; i < metaData.length; i++) {
            if (metaData[i] != null && metaData[i] != "") {
                map.put(metaData[i], i);
            }
        }
        return map;
    }

    /**
     * CSV中在指定的位置加入列
     *
     * @param col     列
     * @param outPath 导出路径
     * @param vals    值
     */
    public void addColumn(List<String> vals,String outPath, int... col) throws IOException {
        dataList = getDataList();
        String line;
        if (col == null || col.length == 0) {
            int i = 0;
            for (String val : vals) {
                dataList.get(i++).add(val);
            }
            exportCsvFile(dataList, outPath);
            return;
        }
        if (col.length > 1) {
            throw new RuntimeException("col length must be 1");
        }
        int i = 0;
        for (String val : vals) {
            dataList.get(i++).add(col[0], val);
        }
        exportCsvFile(dataList, outPath);
    }

    /**
     * CSV删除指定的列
     *
     * @param col
     * @param outPath 导出路径
     */
    public void delColumn(int col, String outPath) throws IOException {
        dataList = getDataList();
        for (int i = 0; i < dataList.size(); i++) {
            dataList.get(i).remove(col);
        }
        exportCsvFile(dataList, outPath);
    }

    /**
     * CSV根据列明删除列
     *
     * @param colName
     * @param outPath
     */
    public void delColumn(String colName, String outPath) throws IOException {
        Map<String, Integer> map = getColumnNameIndex();
        Integer idx = map.get(colName);
        if (idx == null) {
            throw new RuntimeException("the colName is not exit.");
        }
        delColumn(idx,outPath);
        exportCsvFile(dataList, csvPath);
    }

    /**
     * 获取csv数据
     *
     * @return
     * @throws IOException
     */
    public List<List<String>> getDataList() throws IOException {
        dataList = new ArrayList<>();
        try (
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath),"GBK"))
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                List<String> list = new ArrayList<>();
                list.addAll(Arrays.asList(line.split(",",-1)));
                dataList.add(list);
            }
        } catch (Exception e) {
            logger.info("get dataList failed.");
        }
        return dataList;
    }

    /**
     * 导出数据为csv文件
     *
     * @param dataList 数据
     * @Param outPath 导出路径
     */
    public void exportCsvFile(final List<List<String>> dataList, String outPath) {
        File file = new File(outPath);
        try (
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"GBK"))
        ) {
            for (List<String> list : dataList) {
                bufferedWriter.write(StringUtils.join(list, ","));
                bufferedWriter.newLine();
            }
        } catch (Exception e) {
            logger.info("export csv file failed");
        }
    }

    /**
     * 获取指定列的数据
     * @param idx 列号
     * @return
     */
    public List<String> getDataByIndex(int idx) throws IOException {
        if(dataList == null) {
            dataList = getDataList();
        }
        List<String> list = new ArrayList<>();
        for(List<String> l : dataList) {
            list.add(l.get(idx));
        }
        return list;
    }

    /**
     * 获取指定列名的数据
     * @param colName 列名
     * @return
     * @throws IOException
     */
    public List<String> getDataByName(String colName) throws IOException {
        Map<String,Integer> map = getColumnNameIndex();
        return getDataByIndex(map.get(colName));
    }

    public static void main(String[] args) throws IOException {
        SqlSession session = MybatisSqlSession.getSqlSession();
        RecipeTemplateMapper recipeTemplateMapper = session.getMapper(RecipeTemplateMapper.class);
        List<String> params = new ACEResolver("D:/template.csv").getDataByIndex(0);
        int i=0;
        for(String param : params) {
            if(StringUtils.isNotBlank(param)) {
                RecipeTemplate template = new RecipeTemplate();
                template.setId(UUID.randomUUID().toString());
                template.setDeviceTypeId(null);
                template.setDeviceTypeCode("ACEPL203");
                template.setParaCode(String.valueOf(i++));
                template.setParaName(param);
                template.setParaShotName("CurrentRecipeValue" + String.format("%04d",i-1));
                template.setDeviceVariableType("RecipePara");
                template.setCreateBy("shenk");
                template.setCreateDate(new Date());
                template.setUpdateBy("shenk");
                template.setUpdateDate(new Date());
                template.setDelFlag("0");
                recipeTemplateMapper.insert(template);
            }
        }
        session.commit();
        session.close();
    }

}
