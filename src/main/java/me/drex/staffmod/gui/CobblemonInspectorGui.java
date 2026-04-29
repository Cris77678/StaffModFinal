package me.drex.staffmod.gui;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import me.drex.staffmod.logging.AuditLogManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

/**
 * Inspector de party Cobblemon 1.7.3.
 * IVs y EVs son Maps<Stat, Integer> en Kotlin → se acceden via getOrDefault desde Java.
 * Las clases IVs y EVs estan en com.cobblemon.mod.common.pokemon (no en api.pokemon.stats).
 * Para evitar errores de compilacion por cambios de paquete se trabaja con Map<Stat, Integer> genericos.
 */
public class CobblemonInspectorGui extends SimpleGui {

    private final ServerPlayer staff;
    private final ServerPlayer target;

    public CobblemonInspectorGui(ServerPlayer staff, ServerPlayer target) {
        super(MenuType.GENERIC_9x4, staff, false);
        this.staff  = staff;
        this.target = target;
        setTitle(Component.literal("\u00a78\u2756 \u00a73\u1d18\u1d00\u0280\u1d1b\u028f \u1d05\u1d07 \u00a7f" + target.getName().getString()));
        AuditLogManager.log(staff.getName().getString(), staff.getUUID().toString(),
            "PARTY_INSPECT", target.getName().getString(), target.getUUID().toString(), "Inspecciono party");
        build();
    }

    private void build() {
        for (int i = 0; i < getSize(); i++) {
            setSlot(i, new GuiElementBuilder(Items.LIGHT_BLUE_STAINED_GLASS_PANE)
                .setName(Component.literal(" ")).build());
        }

        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(target);
        int partySize = party.size();
        int[] displaySlots = {10, 11, 12, 13, 14, 15};

        for (int i = 0; i < partySize && i < 6; i++) {
            Pokemon pokemon = party.get(i);
            if (pokemon == null) continue;

            boolean isShiny  = pokemon.getShiny();
            String specName  = pokemon.getSpecies().getName();
            int level        = pokemon.getLevel();

            // IVs — pokemon.getIvs() retorna com.cobblemon.mod.common.pokemon.IVs
            // que extiende HashMap<Stat, Int> en Kotlin → desde Java es Map<Stat, Integer>
            @SuppressWarnings("unchecked")
            java.util.Map<Stat, Integer> ivs = (java.util.Map<Stat, Integer>) (Object) pokemon.getIvs();
            int ivHp  = ivs.getOrDefault(Stats.HP, 0);
            int ivAtk = ivs.getOrDefault(Stats.ATTACK, 0);
            int ivDef = ivs.getOrDefault(Stats.DEFENCE, 0);
            int ivSpa = ivs.getOrDefault(Stats.SPECIAL_ATTACK, 0);
            int ivSpd = ivs.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
            int ivSpe = ivs.getOrDefault(Stats.SPEED, 0);
            int totalIvs = ivHp + ivAtk + ivDef + ivSpa + ivSpd + ivSpe;
            int ivPct    = (totalIvs * 100) / 186;

            @SuppressWarnings("unchecked")
            java.util.Map<Stat, Integer> evs = (java.util.Map<Stat, Integer>) (Object) pokemon.getEvs();
            int evHp  = evs.getOrDefault(Stats.HP, 0);
            int evAtk = evs.getOrDefault(Stats.ATTACK, 0);
            int evDef = evs.getOrDefault(Stats.DEFENCE, 0);
            int evSpa = evs.getOrDefault(Stats.SPECIAL_ATTACK, 0);
            int evSpd = evs.getOrDefault(Stats.SPECIAL_DEFENCE, 0);
            int evSpe = evs.getOrDefault(Stats.SPEED, 0);
            int totalEvs = evHp + evAtk + evDef + evSpa + evSpd + evSpe;

            String natureName   = pokemon.getNature().getName().getPath();
            String abilityName  = pokemon.getAbility().getName();

            MoveSet moveSet = pokemon.getMoveSet();
            StringBuilder movesStr = new StringBuilder();
            for (int m = 0; m < moveSet.getMoves().size(); m++) {
                var move = moveSet.get(m);
                if (move != null) {
                    if (movesStr.length() > 0) movesStr.append(", ");
                    movesStr.append(move.getName());
                }
            }

            String heldItem = (pokemon.heldItem() != null && !pokemon.heldItem().isEmpty())
                ? pokemon.heldItem().getItem().toString() : "Ninguno";

            GuiElementBuilder btn = new GuiElementBuilder(isShiny ? Items.NETHER_STAR : Items.PAPER)
                .setName(Component.literal((isShiny ? "\u00a7e\u2728 " : "\u00a7b") + specName
                    + " \u00a77(Nv. \u00a7f" + level + "\u00a77)"))
                .addLoreLine(Component.literal("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"))
                .addLoreLine(Component.literal("\u00a77Naturaleza: \u00a7f" + natureName))
                .addLoreLine(Component.literal("\u00a77Habilidad:  \u00a7f" + abilityName))
                .addLoreLine(Component.literal("\u00a77Shiny: " + (isShiny ? "\u00a7aS\u00ed \u2728" : "\u00a7cNo")))
                .addLoreLine(Component.literal("\u00a77\u00cdtem: \u00a7f" + heldItem))
                .addLoreLine(Component.literal("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"))
                .addLoreLine(Component.literal("\u00a7d\u00a7lIVs \u00a77(" + ivPct + "% perfectos)"))
                .addLoreLine(Component.literal(
                    "\u00a7cHP:\u00a7f" + ivHp + " \u00a76ATK:\u00a7f" + ivAtk + " \u00a7eDEF:\u00a7f" + ivDef))
                .addLoreLine(Component.literal(
                    "\u00a79SPA:\u00a7f" + ivSpa + " \u00a7aSPD:\u00a7f" + ivSpd + " \u00a7bSPE:\u00a7f" + ivSpe))
                .addLoreLine(Component.literal("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"))
                .addLoreLine(Component.literal("\u00a75\u00a7lEVs \u00a77(Total: " + totalEvs + "/510)"))
                .addLoreLine(Component.literal(
                    "\u00a7cHP:\u00a7f" + evHp + " \u00a76ATK:\u00a7f" + evAtk + " \u00a7eDEF:\u00a7f" + evDef))
                .addLoreLine(Component.literal(
                    "\u00a79SPA:\u00a7f" + evSpa + " \u00a7aSPD:\u00a7f" + evSpd + " \u00a7bSPE:\u00a7f" + evSpe))
                .addLoreLine(Component.literal("\u00a78\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500"))
                .addLoreLine(Component.literal("\u00a77Movimientos: \u00a7f" + movesStr));

            setSlot(displaySlots[i], btn.build());
        }

        setSlot(27, new GuiElementBuilder(Items.CHEST)
            .setName(Component.literal("\u00a7b\u00a7lAbrir PC"))
            .addLoreLine(Component.literal("\u00a77Inspecciona las cajas del PC de \u00a7f" + target.getName().getString()))
            .setCallback((idx, type, action, gui) -> new CobblemonPCGui(staff, target, 0).open())
            .build());

        setSlot(35, new GuiElementBuilder(Items.BARRIER)
            .setName(Component.literal("\u00a7cCerrar Inspector"))
            .setCallback((idx, type, action, gui) -> this.close())
            .build());
    }
}
