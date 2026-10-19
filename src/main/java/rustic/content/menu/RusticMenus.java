package rustic.content.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

public final class RusticMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Rustic.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> BARREL =
            MENUS.register("barrel", () -> chest(27));
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> VASE =
            MENUS.register("vase", () -> chest(27));
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> CABINET =
            MENUS.register("cabinet", () -> chest(27));

    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> DOUBLE_CABINET =
            MENUS.register("double_cabinet", () -> chest(54));

    public static final DeferredHolder<MenuType<?>, MenuType<BrewingBarrelMenu>> BREWING_BARREL =
            MENUS.register("brewing_barrel", () ->
                    IMenuTypeExtension.create(BrewingBarrelMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<CondenserMenu>> CONDENSER =
            MENUS.register("condenser", () ->
                    IMenuTypeExtension.create(CondenserMenu::client));
    public static final DeferredHolder<MenuType<?>, MenuType<CondenserAdvancedMenu>> CONDENSER_ADVANCED =
            MENUS.register("condenser_advanced", () ->
                    IMenuTypeExtension.create(CondenserAdvancedMenu::client));

    public static final DeferredHolder<MenuType<?>, MenuType<ApiaryMenu>> APIARY =
            MENUS.register("apiary", () ->
                    IMenuTypeExtension.create(ApiaryMenu::client));

    private static MenuType<ChestMenu> chest(int slots) {
        return IMenuTypeExtension.create((containerId, playerInventory, extraData) ->
                slots == 54 ? ChestMenu.sixRows(containerId, playerInventory)
                        : ChestMenu.threeRows(containerId, playerInventory));
    }

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private RusticMenus() {
    }
}
