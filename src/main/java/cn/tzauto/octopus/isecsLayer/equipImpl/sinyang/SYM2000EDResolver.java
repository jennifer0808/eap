package cn.tzauto.octopus.isecsLayer.equipImpl.sinyang;


import cn.tzauto.octopus.biz.recipe.service.RecipeService;
import cn.tzauto.octopus.biz.recipe.dao.RecipeTemplateMapper;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.biz.recipe.domain.RecipeTemplate;
import cn.tzauto.octopus.common.dataAccess.base.mybatisutil.MybatisSqlSession;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * Created by wj_co on 2019/1/9.
 */
public class SYM2000EDResolver {
    private static Logger logger = Logger.getLogger(SYM2000EDResolver.class);

    private static String datPath;

    private List<List<String>> dataList;

    public SYM2000EDResolver(String datPath) {
        this.datPath = datPath;
    }

    /**
     * 查询File.dat中rcpList
     *
     * @return
     * @throws IOException
     */
    public List<String> getRecipeList() throws IOException {
        List<String> list = new ArrayList<>();
        Map lineIndex = getRcpAndLine();
        Set<String> keys = lineIndex.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            list.add(key);
        }
        System.out.println(list);
        list.remove("型号");
        return list;
    }

    /**
     * 存储Map<recipeName,recipeLine>
     *
     * @return
     * @throws IOException
     */
    public static Map<String, String> getRcpAndLine() throws IOException {
        Map<String, String> map = new LinkedHashMap<>();//有序
        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(datPath), "GBK"));
        ) {
            Integer i = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] rcpLine = line.split("\n");
                map.put(rcpLine[0].split("\t")[1], rcpLine[0]);
            }
        } catch (Exception e) {
            logger.info("getRecipeList failed" + e);
            throw new IOException();
        }
        System.out.println(map);
        return map;
    }


    /**
     * i=0;删除recipe所在行
     * 1.找到指定某行
     * 2.删除
     * 3.导出
     * i=1;删除除recipe外其他所有行
     *
     * @param recipeName
     * @return
     */
    public Boolean deleteLine(String recipeName, int i) {
        Boolean idDelLine = false;
        Map<String, String> map = new LinkedHashMap<>();
        try {
            Map mapRcpList = getRcpAndLine();
            String rcpLine = (String) mapRcpList.get(recipeName);
            if (rcpLine == null) {
                throw new RuntimeException("the lineName is not exit.");
            }
            if (i == 0) {
                mapRcpList.remove(recipeName);
                exportDatFile(mapRcpList);
            } else if (i == 1) {
                map.put("型号", (String) mapRcpList.get("型号"));
                map.put(recipeName, rcpLine);
                exportDatFile(map);
            }
            idDelLine = true;
        } catch (IOException e) {
            e.printStackTrace();
            idDelLine = false;
        }
        return idDelLine;
    }

    /**
     * 添加行内容
     * @param vals
     * @param recipeName
     * @throws IOException
     */
    public void addLine(List<String> vals, String recipeName) throws IOException {
        Map<String, String> map =null;
        if (vals.isEmpty() || vals==null) {
            throw new RuntimeException("the vals is not exit.");
        }
        map = getRcpAndLine();
        for (String selLine : vals) {
            map.put(recipeName, selLine);
        }
        exportDatFile(map);

    }


    /**
     * 导出文件
     *
     * @param map
     */
    public void exportDatFile(Map map) {
        File file = new File(datPath);
        try (
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "GBK"))
        ) {
            Set<String> keys = map.keySet();
            Iterator<String> iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = (String) map.get(key);
                bw.write(value);
                bw.newLine();
                bw.flush();
            }

        } catch (Exception e) {
            logger.info("export csv file failed");
        }
    }


    /**
     * 按Tab键分割存储
     * @param line
     * @return
     */
    public static List<String> splitLine(String line) {
        List<String> list = new ArrayList<>();
        String[] val = line.split("\t");
        for (int i = 0; i < val.length; i++) {
            list.add(val[i]);
        }
        System.out.println(list);
        return list;
    }

    /**
     * 解析文件
     *
     * @param recipeName
     * @return
     */
    public static Map<String, String> transferFromFile(String recipeName) {
        //路径：datPath
        File file = new File(datPath);
        if (!file.exists()) {
            return null;
        }
        System.out.println(datPath);
        Map<String, String> map = new LinkedHashMap<>();//有序
        try {
            Map mapRcp = getRcpAndLine();
            String rcpLine = (String) mapRcp.get(recipeName);
            String titleLine = (String) mapRcp.get("型号");
            if (rcpLine != null && !"".equals(rcpLine) && titleLine != null && !"".equals(titleLine)) {
                List<String> rlList = splitLine(rcpLine);
                List<String> tlList = splitLine(titleLine);
                for (int j = 2; j < rlList.size(); j++) {
                    map.put(tlList.get(j), rlList.get(j));
                }
            }

        } catch (IOException e) {
            logger.info("解析文件有误：" + e);
        }
        System.out.println(map);
        return map;
    }


    /**
     * 存储recipe参数
     *
     * @param paraMap
     * @param deviceType
     * @return
     */
    public static List<RecipePara> transferFromDB(Map paraMap, String deviceType) {
        SqlSession sqlSession = MybatisSqlSession.getSqlSession();
        RecipeService recipeService = new RecipeService(sqlSession);
        List<RecipeTemplate> recipeTemplates = recipeService.searchRecipeTemplateByDeviceTypeCode(deviceType, "RecipePara");
        sqlSession.close();
        List<RecipePara> recipeParaList = new ArrayList<>();
        for (RecipeTemplate recipeTemplate : recipeTemplates) {
            String key = recipeTemplate.getParaName();
            if (key == null || "".equals(key)) {
                continue;
            }
            RecipePara recipePara = new RecipePara();
            recipePara.setParaCode(recipeTemplate.getParaCode());
            recipePara.setParaName(recipeTemplate.getParaName());
            recipePara.setParaShotName(recipeTemplate.getParaShotName());
            recipePara.setSetValue((String) paraMap.get(key));
            recipePara.setMinValue(recipeTemplate.getMinValue());
            recipePara.setMaxValue(recipeTemplate.getMaxValue());
            recipePara.setParaMeasure(recipeTemplate.getParaUnit());
            recipeParaList.add(recipePara);
        }
        return recipeParaList;
    }


    public static void main(String[] args) {
        datPath = "D:\\autotz\\sinyang\\Recipe\\UserDat\\File.dat";
        Map<String, String> map = SYM2000EDResolver.transferFromFile("3 SOJ 32.4L");
        System.out.println(map.size());
        Set<String> StringKey = map.keySet();
        SqlSession session = MybatisSqlSession.getSqlSession();
        RecipeTemplateMapper recipeTemplateMapper = session.getMapper(RecipeTemplateMapper.class);
        int n = 1;
        for (String s : StringKey) {
            RecipeTemplate template = new RecipeTemplate();
            template.setId(UUID.randomUUID().toString());
            template.setDeviceTypeId(null);
            template.setDeviceTypeCode("STMLSSP2000ED");
            template.setParaCode(String.valueOf(n++));
            template.setParaName(s);
            template.setDeviceVariableType("RecipePara");
            template.setCreateBy("jen");
            template.setCreateDate(new Date());
            template.setUpdateBy("jen");
            template.setUpdateDate(new Date());
            template.setDelFlag("0");
            int num = recipeTemplateMapper.insert(template);
            System.out.println("num:" + num);
            System.out.println(s);
        }

        List<RecipePara> list = SYM2000EDResolver.transferFromDB(map, "STMLSSP2000ED");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i).getParaCode() + "=====" + list.get(i).getParaName() + "=====" + list.get(i).getSetValue());
        }
        session.commit();
        session.close();
    }
}
