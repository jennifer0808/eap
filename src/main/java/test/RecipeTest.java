import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.secsLayer.resolver.ismeca.NY20RecipeUtil;
import org.junit.Test;

import java.util.List;

/**
 * Created by leo on 2019-04-10.
 */
public class RecipeTest {

    @Test
    public void testNy20(){
        String filePath = "C:\\Users\\leo\\Documents\\WeChat Files\\Spurs-Leo\\FileStorage\\File\\2019-04\\AAI-T-LGA-3X4.2-3000-5633_V0.txt";
        // transferRcpFromDB(filePath, filePath);
        List<RecipePara> recipeParas = NY20RecipeUtil.transferRcpFromDB2(filePath, "AAI-T-LGA-3X4.2-3000-5633_V0", "IsmecaNY20");
        for (RecipePara r : recipeParas) {
            System.out.println(r.getParaName() + ":" + r.getSetValue());
        }
    }
}
