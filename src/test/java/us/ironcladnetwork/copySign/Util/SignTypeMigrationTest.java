package us.ironcladnetwork.copySign.Util;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SignTypeMigrationTest {
    @Test void shippedLegacyDefaultsBecomeAutomatic() {
        for(boolean paleOak:new boolean[]{false,true}) {
            var config=new YamlConfiguration();var defaults=new ArrayList<String>();
            for(String wood:List.of("OAK","SPRUCE","BIRCH","JUNGLE","ACACIA","DARK_OAK","MANGROVE","CHERRY","BAMBOO","CRIMSON","WARPED","PALE_OAK")) {
                if(!paleOak&&wood.equals("PALE_OAK"))continue;
                for(String suffix:List.of("_SIGN","_WALL_SIGN","_HANGING_SIGN","_WALL_HANGING_SIGN"))defaults.add(wood+suffix);
            }
            config.set("sign-types.allowed",defaults);ConfigMigrator.migrateDefaultSignTypes(config);
            assertTrue(config.getStringList("sign-types.allowed").isEmpty());
        }
    }
    @Test void customRestrictionsAndAutomaticModeArePreserved() {
        for(List<String> list:List.of(List.<String>of(),List.of("OAK_SIGN"),List.of("POPLAR_SIGN","POPLAR_HANGING_SIGN"))) {
            var config=new YamlConfiguration();config.set("sign-types.allowed",list);
            ConfigMigrator.migrateDefaultSignTypes(config);assertEquals(list,config.getStringList("sign-types.allowed"));
        }
    }
}
