package me.drex.staffmod.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.staffmod.logging.AuditLogManager;
import me.drex.staffmod.util.PermissionUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

/**
 * Inspector del PC de Cobblemon 1.7.3.
 *
 * PCStore extiende PokemonStore<PCBox>.
 * - PokemonStore.iterator() itera sobre Pokemon? (NOT PCBox)
 * - Las cajas se acceden via pc.getBoxes() (propiedad Kotlin 'boxes' -> getter Java)
 * - Cada PCBox tiene 30 slots indexados 0..29, accedidos via box.get(index)
 * - PCBox.get(index) retorna Pokemon? (nullable)
 *
 * IVs/EVs se castean a Map<Stat,Integer> (extienden HashMap en Kotlin).
 */
public class CobblemonPCGui extends SimpleGui {

    private final ServerPlayer staff;
    private final ServerPlayer target;
    private final boolean canEdit;
    private int currentBox;
    private static final int SLOTS_PER_BOX = 30;

    public CobblemonPCGui(ServerPlayer staff, ServerPlayer target, int startBox) {
        super(MenuType.GENERIC_9x6, staff, false);
        this.staff      = staff;
        this.target     = target;
        this.currentBox = startBox;
        this.canEdit    = PermissionUtil.has(staff, "staffmod.pokemon.pc.edit");
        AuditLogManager.log(staff.getName().getString(), staff.getUUID().toString(),
            "PC_INSPECT", target.getName().getString(), target.getUUID().toString(),
            "Caja " + (currentBox + 1));
        build();
    }

    private void build() {
        for (int i = 0; i < getSize(); i++) clearSlot(i);

        PCStore pc = Cobblemon.INSTANCE.getStorage().getPC(target);
        if (pc == null) {
            setTitle(Component.literal("\u00a7cError: PC no disponible"));
            return;
        }

        // pc.getBoxes() = propiedad Kotlin 'boxes': List<PCBox>
        List<PCBox> boxes = pc.getBoxes();
        int totalBoxes = boxes.size();

        if (totalBoxes == 0) {
            setTitle(Component.literal("\u00a7cPC vac\u00edo o inaccesible"));
            return;
        }
        currentBox = Math.max(0, Math.min(currentBox, totalBoxes - 1));

        setTitle(Component.literal("\u00a78\u2756 \u00a7b\u1d18\u1d04 \u1d05\u1d07 \u00a7f" + target.getName().getString()
            + " \u00a78\u2014 \u00a77Caja \u00a7f" + (currentBox + 1) + "\u00a77/\u00a7f" + totalBoxes
            + (canEdit ? " \u00a7a(Editor)" : " \u00a77(Lectura)")));

        PCBox box = boxes.get(currentBox);

        for (int i = 0; i < SLOTS_PER_BOX; i++) {
            // box.get(index) retorna Pokemon? (Kotlin nullable)
            Pokemon pokemon;
            try { pokemon = box.get(i); }
            catch (Exception e) { pokemon = null; }

            if (pokemon == null) {
                setSlot(i, new GuiElementBuilder(Items.LIGHT_GRAY_STAINED_GLASS_PANE)
                    .setName(Component.literal("\u00a78Slot vac\u00edo")).build());
                continue;
            }

            boolean isShiny   = pokemon.getShiny();
            String specName   = pokemon.getSpecies().getName();
            int level         = pokemon.getLevel();
            String natureName = pokemon.getNature().getName().getPath();

            @SuppressWarnings("unchecked")
            Map<Stat, Integer> ivs = (Map<Stat, Integer>) (Object) pokemon.getIvs();
            int ivHp  = ivs.getOrDefault(Stats.HP, 0);
            int ivAtk = ivs.getOrDefault(Stats.ATTACK, 0);
            int ivDef = ivs.getOrDefault(Stats.DEFENCE, 0);
            int ivSpa = ivs.getOrDefault(Stats.SPECIAL_ATTACK, 0);
            int ivSpd = ivs.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
            int ivSpe = ivs.getOrDefault(Stats.SPEED, 0);
            int totalIvs = ivHp + ivAtk + ivDef + ivSpa + ivSpd + ivSpe;
            int ivPct    = (totalIvs * 100) / 186;

            GuiElementBuilder btn = new GuiElementBuilder(isShiny ? Items.NETHER_STAR : Items.EGG)
                .setName(Component.literal((isShiny ? "\u00a7e\u2728 " : "\u00a7b") + specName
                    + " \u00a77Nv.\u00a7f" + level))
                .addLoreLine(Component.literal("\u00a77Naturaleza: \u00a7f" + natureName))
                .addLoreLine(Component.literal("\u00a77IVs: \u00a7d" + ivPct + "% \u00a77(" + totalIvs + "/186)"))
                .addLoreLine(Component.literal(
                    "\u00a7cHP:\u00a7f" + ivHp + " \u00a76ATK:\u00a7f" + ivAtk + " \u00a7eDEF:\u00a7f" + ivDef))
                .addLoreLine(Component.literal(
                    "\u00a79SPA:\u00a7f" + ivSpa + " \u00a7aSPD:\u00a7f" + ivSpd + " \u00a7bSPE:\u00a7f" + ivSpe))
                .addLoreLine(Component.literal("\u00a77Shiny: " + (isShiny ? "\u00a7aS\u00ed \u2728" : "\u00a7cNo")));

            if (canEdit) btn.addLoreLine(Component.literal("\u00a7a(Editor activo)"));
            setSlot(i, btn.build());
        }

        // Barra navegacion fila 6
        for (int i = 45; i < 54; i++) {
            setSlot(i, new GuiElementBuilder(Items.BLACK_STAINED_GLASS_PANE)
                .setName(Component.literal(" ")).build());
        }

        if (currentBox > 0) {
            final int prev = currentBox - 1;
            setSlot(45, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("\u00a7e\u25c4 Caja " + currentBox))
                .setCallback((i, t, a, g) -> new CobblemonPCGui(staff, target, prev).open()).build());
        }

        setSlot(49, new GuiElementBuilder(Items.PAPER)
            .setName(Component.literal("\u00a77Caja \u00a7f" + (currentBox + 1) + " \u00a77de \u00a7f" + totalBoxes))
            .addLoreLine(Component.literal("\u00a77Jugador: \u00a7f" + target.getName().getString()))
            .build());

        if (currentBox < totalBoxes - 1) {
            final int next = currentBox + 1;
            setSlot(53, new GuiElementBuilder(Items.ARROW)
                .setName(Component.literal("\u00a7eCaja " + (currentBox + 2) + " \u25ba"))
                .setCallback((i, t, a, g) -> new CobblemonPCGui(staff, target, next).open()).build());
        }

        setSlot(47, new GuiElementBuilder(Items.DARK_OAK_DOOR)
            .setName(Component.literal("\u00a7c\u25c4 Volver a Party"))
            .setCallback((i, t, a, g) -> new CobblemonInspectorGui(staff, target).open()).build());
    }
}
