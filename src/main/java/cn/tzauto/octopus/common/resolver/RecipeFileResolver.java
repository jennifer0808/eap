package cn.tzauto.octopus.common.resolver;

import java.io.File;
import java.util.Map;

public interface RecipeFileResolver {

    /**
     * resolve recipe file
     * @param file recipeFile
     * @return recipe param by map
     */
    public Map<String,String> resolve(File file);
}
