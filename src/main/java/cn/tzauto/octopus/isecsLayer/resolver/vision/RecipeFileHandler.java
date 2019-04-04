package cn.tzauto.octopus.isecsLayer.resolver.vision;

import java.io.File;
import java.util.Map;

public interface RecipeFileHandler {

    public Map<String,String> handler(File file);
}
