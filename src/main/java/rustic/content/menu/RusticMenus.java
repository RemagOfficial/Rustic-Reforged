package rustic.content.menu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import rustic.Rustic;

/**
 * Menu types for the tile-entity containers, the 1.21 replacement for 1.12.2's {@code GuiProxy}.
 *
 * <p>The legacy mod mapped a GUI id to a {@code Container} / {@code GuiContainer} pair in
 * {@code GuiProxy.getServerGuiElement} / {@code getClientGuiElement} and opened it with
 * {@code player.openGui}. 1.21 has no GUI ids: the block entity is a {@code MenuProvider}, the
 * {@link MenuType} is what the client uses to rebuild the same container from the packet the server
 * sends when {@code player.openMenu(provider)} runs. That is the whole of the proxy's job, so the
 * proxy itself is never ported.
 *
 * <p><b>Why every entry here is a {@code ChestMenu} rather than a bespoke subclass.</b> Barrel, vase
 * and cabinet all kept a plain 27-slot inventory behind {@code ContainerCabinet}/{@code ContainerBarrel}
 * /{@code ContainerVase}, which were copies of vanilla's {@code Slot} layout with a fixed
 * 176x166 {@code generic_27.png} background - i.e. exactly what {@link ChestMenu} three rows already
 * is, and what 1.12.2's double-cabinet {@code ContainerCabinetDouble} was for six rows. Reusing
 * {@code ChestMenu} reproduces that layout without restating slot arithmetic. The only reason not to
 * point straight at {@link MenuType#GENERIC_9x3} is that a custom {@code Screen} is bound to a
 * {@code MenuType}, and binding ours to vanilla's would restyle every vanilla chest in the game.
 *
 * <p><b>Why the client container is a fresh {@code SimpleContainer}, not the block entity.</b> This
 * mirrors vanilla: the factory builds an empty inventory of the right size and
 * {@code AbstractContainerMenu}'s own slot synchronisation fills it from the server. Resolving the
 * live block entity on the client - and so calling {@code startOpen} a second time against the world
 * copy - is what the vanilla generic chest menus deliberately avoid, so they do too.
 */
public final class RusticMenus {

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Rustic.MODID);

    /** The barrel, vase and single cabinet share one 27-slot (three row) menu type each. */
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> BARREL =
            MENUS.register("barrel", () -> chest(27));
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> VASE =
            MENUS.register("vase", () -> chest(27));
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> CABINET =
            MENUS.register("cabinet", () -> chest(27));
    /** Two joined cabinets show 54 slots, the legacy {@code ContainerCabinetDouble} six rows. */
    public static final DeferredHolder<MenuType<?>, MenuType<ChestMenu>> DOUBLE_CABINET =
            MENUS.register("double_cabinet", () -> chest(54));

    /**
     * A chest menu type of {@code slots} slots. The client factory rebuilds the same empty inventory the
     * vanilla generic menus build - {@link ChestMenu#threeRows} / {@link ChestMenu#sixRows} hand back a
     * {@code SimpleContainer} of the matching size that {@code AbstractContainerMenu}'s slot sync then
     * fills - so it does not need to name this type and no block entity is resolved on the client. The
     * server menu, built by the block entity's {@code createMenu}, is what carries this type into the
     * packet that decides which factory and which {@code Screen} the client uses.
     */
    private static MenuType<ChestMenu> chest(int slots) {
        return IMenuTypeExtension.create((containerId, playerInventory, extraData) ->
                slots == 54 ? ChestMenu.sixRows(containerId, playerInventory)
                        : ChestMenu.threeRows(containerId, playerInventory));
    }

    /** Attaches the menu register to the mod event bus. Called from the mod constructor. */
    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }

    private RusticMenus() {
    }
}
